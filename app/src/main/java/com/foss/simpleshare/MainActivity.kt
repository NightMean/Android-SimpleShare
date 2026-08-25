package com.foss.simpleshare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.foss.simpleshare.data.AppDatabase
import com.foss.simpleshare.data.FileModel
import com.foss.simpleshare.data.FileRepository
import com.foss.simpleshare.data.fileModelFromPath
import com.foss.simpleshare.settings.AppSettings
import com.foss.simpleshare.settings.SettingsStore
import com.foss.simpleshare.settings.resolveAllowedExtensions
import com.foss.simpleshare.ui.Screen
import com.foss.simpleshare.feature.browser.FileBrowserScreen
import com.foss.simpleshare.feature.settings.SettingsScreen
import com.foss.simpleshare.feature.setup.SetupScreen
import com.foss.simpleshare.ui.theme.SimpleShareTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind system bars so the navigation bar sits over the app content
        // instead of leaving a black strip below it on gesture-navigation devices.
        enableEdgeToEdge()
        prefs = getSharedPreferences(SettingsStore.PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            SimpleShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        var hasPermission by remember { mutableStateOf(checkStoragePermission()) }

        // Database + repositories (folder size caching)
        val appContext = LocalContext.current.applicationContext
        val database = remember { AppDatabase.getDatabase(appContext) }
        val directoryCacheDao = remember { database.directoryCacheDao() }
        val repository = remember { FileRepository(directoryCacheDao) }

        // Single source of truth for every user-configurable setting.
        val settingsStore = remember { SettingsStore(prefs, repository.getDefaultPath()) }
        var settings by remember { mutableStateOf(settingsStore.load()) }

        fun updateSettings(newSettings: AppSettings) {
            settings = newSettings
            settingsStore.save(newSettings)
        }

        var currentScreen by remember {
            mutableStateOf(
                if (!hasPermission || settings.targetAppPackage == null) Screen.SETUP
                else Screen.BROWSER
            )
        }

        // Survives configuration changes (rotation) and process death restoration
        var currentPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(settings.defaultPath) }

        val allowedExtensions = remember(settings.filterMode, settings.customExtensions) {
            resolveAllowedExtensions(settings)
        }

        // Hoisted selection state (shared across screens so selection survives navigation).
        // Saved as path strings so the selection also survives configuration changes.
        val selectedFiles = androidx.compose.runtime.saveable.rememberSaveable(
            saver = androidx.compose.runtime.saveable.listSaver<
                androidx.compose.runtime.snapshots.SnapshotStateList<FileModel>, String>(
                save = { list -> list.map { it.path } },
                restore = { paths ->
                    androidx.compose.runtime.mutableStateListOf<FileModel>()
                        .apply { addAll(paths.mapNotNull(::fileModelFromPath)) }
                }
            )
        ) { androidx.compose.runtime.mutableStateListOf<FileModel>() }

        // UI State (Hoisted to persist across navigation)
        var isGridView by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

        val context = LocalContext.current
        LaunchedEffect(settings.showThumbnails) {
            if (!settings.showThumbnails) {
                com.bumptech.glide.Glide.get(context).clearMemory()
            }
        }

        // Refresh UI after returning from system screens (e.g. permission grant)
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    val newPermission = checkStoragePermission()
                    if (newPermission != hasPermission) {
                        hasPermission = newPermission
                        // If we lose permission we must go back to SETUP
                        if (!newPermission) {
                            currentScreen = Screen.SETUP
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Validate Default Path Logic
        var showInvalidPathDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val file = File(settings.defaultPath)
            if (!file.exists() || !file.isDirectory) {
                showInvalidPathDialog = true
            }
        }

        if (showInvalidPathDialog) {
            AlertDialog(
                onDismissRequest = { /* Force user to acknowledge */ },
                title = { Text(stringResource(R.string.invalid_path_title)) },
                text = { Text(stringResource(R.string.invalid_path_message, File(settings.defaultPath).absolutePath)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newPath = repository.getDefaultPath()
                            currentPath = newPath
                            updateSettings(settings.copy(defaultPath = newPath))
                            showInvalidPathDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            )
        }

        // NAVIGATION HOST
        when (currentScreen) {
            Screen.BROWSER -> {
                if (hasPermission) {
                    FileBrowserScreen(
                        repository = repository,
                        currentPath = currentPath,
                        onPathChange = { newPath -> currentPath = newPath },
                        selectedFiles = selectedFiles,
                        targetAppPackageName = settings.targetAppPackage,
                        keepSelection = settings.keepSelection,
                        showThumbnails = settings.showThumbnails,
                        checkLowStorage = settings.checkLowStorage,
                        quickOpen = settings.quickOpen,
                        allowedExtensions = allowedExtensions,
                        isGridView = isGridView,
                        onViewModeChange = { isGridView = it },
                        onSettingsClick = { currentScreen = Screen.SETTINGS },
                        sortOption = settings.sortOption,
                        isSortAscending = settings.isSortAscending,
                        sortFoldersFirst = settings.sortFoldersFirst,
                        onSortChange = { newOption, newAsc, newFoldersFirst ->
                            updateSettings(
                                settings.copy(
                                    sortOption = newOption,
                                    isSortAscending = newAsc,
                                    sortFoldersFirst = newFoldersFirst
                                )
                            )
                        }
                    )
                } else {
                    // Fallback if permission lost
                    currentScreen = Screen.SETUP
                }
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    currentSettings = settings,
                    currentBrowserPath = currentPath,
                    selectedFileCount = selectedFiles.size,
                    onBack = { currentScreen = Screen.BROWSER },

                    onSave = { newSettings ->
                        // If Keep Selection is disabled, or the visibility filter changed,
                        // clear the current selection
                        val filterChanged = newSettings.filterMode != settings.filterMode ||
                                newSettings.customExtensions != settings.customExtensions
                        if ((!newSettings.keepSelection || filterChanged) && selectedFiles.isNotEmpty()) {
                            selectedFiles.clear()
                        }

                        updateSettings(newSettings)

                        android.widget.Toast.makeText(context, context.getString(R.string.toast_settings_saved), android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onReset = {
                        settingsStore.clear()

                        val newDefaultPath = repository.getDefaultPath()
                        settings = AppSettings(defaultPath = newDefaultPath, targetAppPackage = null)
                        currentPath = newDefaultPath

                        selectedFiles.clear()

                        // Force back to SETUP because targetApp is null
                        currentScreen = Screen.SETUP

                        android.widget.Toast.makeText(context, context.getString(R.string.toast_reset_defaults), android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Screen.SETUP,
            Screen.SETUP_APP_SELECTION -> {
                SetupScreen(
                    currentScreen = currentScreen,
                    permissionGranted = hasPermission,
                    selectedTargetApp = settings.targetAppPackage,
                    onRequestPermission = { requestStoragePermission() },
                    onAppSelected = { app ->
                        updateSettings(settings.copy(targetAppPackage = app))
                    },
                    currentFilterMode = settings.filterMode,
                    currentCustomExtensions = settings.customExtensions,
                    onFilterModeChange = { mode ->
                        updateSettings(settings.copy(filterMode = mode))
                    },
                    onCustomExtensionsChange = { ext ->
                        updateSettings(settings.copy(customExtensions = ext))
                    },
                    onFinish = {
                        currentScreen = Screen.BROWSER
                    },
                    onNavigateToAppSelection = {
                        currentScreen = Screen.SETUP_APP_SELECTION
                    },
                    onBackFromAppSelection = {
                        currentScreen = Screen.SETUP
                    }
                )
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // For < Android 11 we rely on standard READ/WRITE permissions.
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse(String.format("package:%s", packageName))
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1001
            )
        }
    }
}
