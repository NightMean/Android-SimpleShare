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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.foss.simpleshare.data.FileRepository
import com.foss.simpleshare.data.AppDatabase
import com.foss.simpleshare.ui.screens.FileBrowserScreen
import com.foss.simpleshare.ui.theme.SimpleShareTheme

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "simpleshare_prefs"
    private val KEY_DEFAULT_PATH = "default_path"
    private val KEY_TARGET_APP = "target_app_package"
    private val KEY_KEEP_SELECTION = "keep_selection"

    private val KEY_SHOW_THUMBNAILS = "show_thumbnails"
    private val KEY_CHECK_LOW_STORAGE = "check_low_storage"
    private val KEY_QUICK_OPEN = "quick_open"
    private val KEY_FILTER_MODE = "filter_mode"
    private val KEY_CUSTOM_EXTENSIONS = "custom_extensions"
    private val KEY_SORT_OPTION = "sort_option"
    private val KEY_SORT_ASCENDING = "sort_ascending"
    private val KEY_SORT_FOLDERS_FIRST = "sort_folders_first"

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind system bars so the navigation bar sits over the app content
        // instead of leaving a black strip below it on gesture-navigation devices.
        enableEdgeToEdge()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        // State for Screen Navigation
        val savedTargetApp = prefs.getString(KEY_TARGET_APP, null)
        var targetAppPackage by remember { mutableStateOf(savedTargetApp) }
        
        var currentScreen by remember {
            mutableStateOf(
                if (!hasPermission || targetAppPackage == null) com.foss.simpleshare.ui.Screen.SETUP 
                else com.foss.simpleshare.ui.Screen.BROWSER
            )
        }
        
        // Define default path from prefs, but also keep track of current browsing path
        // Initialize database for folder size caching (use application context)
        val appContext = LocalContext.current.applicationContext
        val database = remember { AppDatabase.getDatabase(appContext) }
        val directoryCacheDao = remember { database.directoryCacheDao() }
        
        val initialDefaultPath = remember {
            prefs.getString(KEY_DEFAULT_PATH, FileRepository(directoryCacheDao).getDefaultPath()) ?: FileRepository(directoryCacheDao).getDefaultPath()
        }
        var defaultPathSetting by remember { mutableStateOf(initialDefaultPath) }

        val savedKeepSelection = prefs.getBoolean(KEY_KEEP_SELECTION, true) // Default true
        val savedShowThumbnails = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true) // Default true
        val savedCheckLowStorage = prefs.getBoolean(KEY_CHECK_LOW_STORAGE, false) 
        val savedQuickOpen = prefs.getBoolean(KEY_QUICK_OPEN, false) 
        val savedFilterMode = prefs.getString(KEY_FILTER_MODE, "PRESET_ALL") ?: "PRESET_ALL"
        val savedCustomExtensions = prefs.getString(KEY_CUSTOM_EXTENSIONS, "") ?: ""

        val savedSortOptionStr = prefs.getString(KEY_SORT_OPTION, "NAME") ?: "NAME"
        val savedSortAscending = prefs.getBoolean(KEY_SORT_ASCENDING, true)
        val savedSortFoldersFirst = prefs.getBoolean(KEY_SORT_FOLDERS_FIRST, true)

        // We initialize currentPath with initialDefaultPath
        var currentPath by remember { mutableStateOf(initialDefaultPath) }
        var keepSelection by remember { mutableStateOf(savedKeepSelection) }
        var showThumbnails by remember { mutableStateOf(savedShowThumbnails) }
        var checkLowStorage by remember { mutableStateOf(savedCheckLowStorage) }
        var quickOpen by remember { mutableStateOf(savedQuickOpen) }
        var filterMode by remember { mutableStateOf(savedFilterMode) }
        var customExtensions by remember { mutableStateOf(savedCustomExtensions) }

        var sortOption by remember { mutableStateOf(try { com.foss.simpleshare.ui.screens.SortOption.valueOf(savedSortOptionStr) } catch(e: Exception) { com.foss.simpleshare.ui.screens.SortOption.NAME }) }
        var isSortAscending by remember { mutableStateOf(savedSortAscending) }
        var sortFoldersFirst by remember { mutableStateOf(savedSortFoldersFirst) }

        // Compute Allowed Extensions based on Mode
        val allowedExtensions = remember(filterMode, customExtensions) {
            if (filterMode == "PRESET_MEDIA") {
                setOf("jpg", "jpeg", "png", "gif", "mp4", "mkv", "webm", "avi", "heic", "webp") // Media Preset
            } else if (filterMode == "PRESET_ALL") {
                emptySet() // Empty set means "All" in our logic (need to verify this in Repo/Browser)
            } else {
                // Custom Parsing
                customExtensions.split(",")
                    .map { it.trim().lowercase().removePrefix(".") }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        }
        
        // Hoisted selection state
        val selectedFiles = remember { androidx.compose.runtime.mutableStateListOf<com.foss.simpleshare.data.FileModel>() }
        
        // UI State (Hoisted to persist across navigation)
        var isGridView by remember { mutableStateOf(false) }
        
        // Handle Coil Cache clearing when disabled
        val context = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(showThumbnails) {
            if (!showThumbnails) {
                com.bumptech.glide.Glide.get(context).clearMemory()
            }
        }

        // This is a simple state trigger to refresh UI after returning from Settings
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    val newPermission = checkStoragePermission()
                    if (newPermission != hasPermission) {
                        hasPermission = newPermission
                        // If we gain permission and we were in SETUP, we should stay in SETUP until App is selected
                        // If we lose permission, we must go to SETUP
                        if (!newPermission) {
                            currentScreen = com.foss.simpleshare.ui.Screen.SETUP
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
            val file = File(defaultPathSetting)
            if (!file.exists() || !file.isDirectory) {
                showInvalidPathDialog = true
            }
        }

        if (showInvalidPathDialog) {
            AlertDialog(
                onDismissRequest = { /* Force user to acknowledge */ },
                title = { Text("Default Path Not Found") },
                text = { Text("The default path you set (\"${File(defaultPathSetting).absolutePath}\") no longer exists. The app will reset to the main storage directory.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newPath = FileRepository(directoryCacheDao).getDefaultPath()
                            // Reset State
                            currentPath = newPath
                            defaultPathSetting = newPath // Update setting too
                            // Update Prefs
                            prefs.edit().putString(KEY_DEFAULT_PATH, newPath).apply()
                            
                            showInvalidPathDialog = false
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

        if (false) { 
             // Legacy PermissionScreen block removed, handled by SetupScreen
        }

        val repository = remember { FileRepository(directoryCacheDao) }
        
        // NAVIGATION HOST
        when (currentScreen) {
            com.foss.simpleshare.ui.Screen.BROWSER -> {
                if (hasPermission) {
                    FileBrowserScreen(
                        repository = repository,
                        currentPath = currentPath,
                        onPathChange = { newPath -> currentPath = newPath },
                        selectedFiles = selectedFiles,
                        targetAppPackageName = targetAppPackage,
                        keepSelection = keepSelection,
                        showThumbnails = showThumbnails,
                        checkLowStorage = checkLowStorage,
                        quickOpen = quickOpen,
                        allowedExtensions = allowedExtensions,
                        isGridView = isGridView,
                        onViewModeChange = { isGridView = it },
                        onSettingsClick = { currentScreen = com.foss.simpleshare.ui.Screen.SETTINGS },
                        sortOption = sortOption,
                        isSortAscending = isSortAscending,
                        sortFoldersFirst = sortFoldersFirst,
                        onSortChange = { newOption, newAsc, newFoldersFirst ->
                            sortOption = newOption
                            isSortAscending = newAsc
                            sortFoldersFirst = newFoldersFirst
                            
                            val editor = prefs.edit()
                            editor.putString(KEY_SORT_OPTION, newOption.name)
                            editor.putBoolean(KEY_SORT_ASCENDING, newAsc)
                            editor.putBoolean(KEY_SORT_FOLDERS_FIRST, newFoldersFirst)
                            editor.apply()
                        }
                    )
                } else {
                    // Fallback if permission lost
                    currentScreen = com.foss.simpleshare.ui.Screen.SETUP
                }
            }
            com.foss.simpleshare.ui.Screen.SETTINGS -> {
                com.foss.simpleshare.ui.screens.SettingsScreen(
                    currentDefaultPath = defaultPathSetting,
                    currentBrowserPath = currentPath,
                    currentTargetAppPackage = targetAppPackage, // Can be null now
                    currentKeepSelection = keepSelection,
                    currentShowThumbnails = showThumbnails,
                    currentCheckLowStorage = checkLowStorage,
                    currentQuickOpen = quickOpen,
                    currentFilterMode = filterMode,
                    currentCustomExtensions = customExtensions,
                    selectedFileCount = selectedFiles.size,
                    onBack = { currentScreen = com.foss.simpleshare.ui.Screen.BROWSER },

                    onSave = { path, targetApp, keepSel, showIcons, checkSpace, qOpen, fMode, cExt ->
                        val editor = prefs.edit()
                        editor.putString(KEY_DEFAULT_PATH, path)
                        
                        // Update state to trigger recomposition with new saved value
                        defaultPathSetting = path
                        
                        if (targetApp != null) {
                            editor.putString(KEY_TARGET_APP, targetApp)
                            targetAppPackage = targetApp
                        } else {
                            editor.remove(KEY_TARGET_APP)
                            targetAppPackage = null
                        }
                        editor.putBoolean(KEY_KEEP_SELECTION, keepSel)
                        keepSelection = keepSel
                        
                        // If Keep Selection is disabled, or IF VISIBILITY CHANGED, we should clear the current selection
                        val filterChanged = filterMode != fMode || customExtensions != cExt
                        if ((!keepSel || filterChanged) && selectedFiles.isNotEmpty()) {
                            selectedFiles.clear()
                        }
                        
                        editor.putBoolean(KEY_SHOW_THUMBNAILS, showIcons)
                        showThumbnails = showIcons
                        
                        editor.putBoolean(KEY_CHECK_LOW_STORAGE, checkSpace)
                        checkLowStorage = checkSpace
                        
                        editor.putBoolean(KEY_QUICK_OPEN, qOpen)
                        quickOpen = qOpen
                        
                        editor.putString(KEY_FILTER_MODE, fMode)
                        filterMode = fMode
                        
                        editor.putString(KEY_CUSTOM_EXTENSIONS, cExt)
                        customExtensions = cExt

                        editor.apply()
                        
                        android.widget.Toast.makeText(context, "Settings saved", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onReset = {
                        val editor = prefs.edit()
                        editor.clear()
                        editor.apply()
                        
                        targetAppPackage = null
                        keepSelection = true 
                        showThumbnails = true 
                        checkLowStorage = false 
                        quickOpen = false 
                        filterMode = "PRESET_ALL"
                        customExtensions = ""
                        
                        sortOption = com.foss.simpleshare.ui.screens.SortOption.NAME
                        isSortAscending = true
                        sortFoldersFirst = true
                        
                        // Recalculate default path from Repo (now includes slash)
                        val newDefaultPath = FileRepository(directoryCacheDao).getDefaultPath()
                        defaultPathSetting = newDefaultPath
                        currentPath = newDefaultPath
                        
                        selectedFiles.clear()
                        
                        // Force back to SETUP because targetApp is null
                        currentScreen = com.foss.simpleshare.ui.Screen.SETUP
                        
                        android.widget.Toast.makeText(context, "Reset to defaults.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
            com.foss.simpleshare.ui.Screen.SETUP,
            com.foss.simpleshare.ui.Screen.SETUP_APP_SELECTION -> {
                com.foss.simpleshare.ui.screens.SetupScreen(
                    currentScreen = currentScreen,
                    permissionGranted = hasPermission,
                    selectedTargetApp = targetAppPackage,
                    onRequestPermission = { requestStoragePermission() },
                    onAppSelected = { app -> 
                         targetAppPackage = app
                         val editor = prefs.edit()
                         editor.putString(KEY_TARGET_APP, app)
                         editor.apply()
                    },
                    currentFilterMode = filterMode,
                    currentCustomExtensions = customExtensions,
                    onFilterModeChange = { mode ->
                        filterMode = mode
                        val editor = prefs.edit()
                        editor.putString(KEY_FILTER_MODE, mode)
                        editor.apply()
                    },
                    onCustomExtensionsChange = { ext ->
                        customExtensions = ext
                        val editor = prefs.edit()
                        editor.putString(KEY_CUSTOM_EXTENSIONS, ext)
                        editor.apply()
                    },
                    onFinish = {
                        currentScreen = com.foss.simpleshare.ui.Screen.BROWSER
                    },
                    onNavigateToAppSelection = {
                        currentScreen = com.foss.simpleshare.ui.Screen.SETUP_APP_SELECTION
                    },
                    onBackFromAppSelection = {
                        currentScreen = com.foss.simpleshare.ui.Screen.SETUP
                    }
                )
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // For < Android 11, basic read/write usually enough, but we want full manager access logic mapping
            // Actually for < 11 we rely on standard permissions
            // Since User says "Target SDK 34, Min 28", checkStoragePermission() should cover both cases.
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

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Access to all files is required to browse and share media.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permissions")
            }
        }
    }
}
