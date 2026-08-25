package com.foss.simpleshare.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.io.File
import com.foss.simpleshare.BuildConfig
import com.foss.simpleshare.data.AppModel
import com.foss.simpleshare.data.AppRepository
import com.foss.simpleshare.settings.AppSettings
import com.foss.simpleshare.settings.FilterMode
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.launch
import com.foss.simpleshare.ui.components.TooltipIconButton
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BugReport

private enum class SettingsPage { MAIN, APP_SELECTION }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    currentBrowserPath: String,
    selectedFileCount: Int,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onReset: () -> Unit
) {
    val currentDefaultPath = currentSettings.defaultPath
    var path by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(
        text = currentDefaultPath,
        selection = androidx.compose.ui.text.TextRange(currentDefaultPath.length)
    )) }
    var selectedComponent by remember { mutableStateOf(currentSettings.targetAppPackage) }
    var keepSelection by remember { mutableStateOf(currentSettings.keepSelection) }
    var showThumbnails by remember { mutableStateOf(currentSettings.showThumbnails) }
    var checkLowStorage by remember { mutableStateOf(currentSettings.checkLowStorage) }
    var quickOpen by remember { mutableStateOf(currentSettings.quickOpen) }
    var filterMode by remember { mutableStateOf(currentSettings.filterMode) }
    var customExtensions by remember { mutableStateOf(currentSettings.customExtensions) }
    var isPathError by remember { mutableStateOf(false) }
    
    // Internal Navigation State
    var pageState by remember { mutableStateOf(SettingsPage.MAIN) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = LocalUriHandler.current
    val appRepository = remember { AppRepository(context) }
    var apps by remember { mutableStateOf(emptyList<AppModel>()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        apps = appRepository.getShareableApps()
        isLoadingApps = false
    }
    
    // Warning Dialog Trigger
    var showClearSelectionWarning by remember { mutableStateOf(false) }

    var isCustomExtError by remember { mutableStateOf(false) }

    // Reusable Validation Function
    fun validateAndSave(goBackAfterSave: Boolean = false) {
        val currentPathText = path.text
        val file = File(currentPathText)
        if (!file.exists() || !file.isDirectory) {
            isPathError = true
            android.widget.Toast.makeText(context, "Invalid default folder path. Please enter a valid directory.", android.widget.Toast.LENGTH_LONG).show()
            return
        }

        if (filterMode == FilterMode.CUSTOM) {
            val hasValidChar = customExtensions.any { it.isLetterOrDigit() }
            if (!hasValidChar) {
                isCustomExtError = true
                android.widget.Toast.makeText(context, "Please enter at least one file extension (e.g. pdf, zip, 7z)", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }

        onSave(
            AppSettings(
                defaultPath = currentPathText,
                targetAppPackage = selectedComponent,
                keepSelection = keepSelection,
                showThumbnails = showThumbnails,
                checkLowStorage = checkLowStorage,
                quickOpen = quickOpen,
                filterMode = filterMode,
                customExtensions = customExtensions,
                sortOption = currentSettings.sortOption,
                isSortAscending = currentSettings.isSortAscending,
                sortFoldersFirst = currentSettings.sortFoldersFirst
            )
        )

        if (goBackAfterSave) {
            onBack()
        }
    }

    // Dirty State Tracking simplified (recalculated on recomposition)
    val isDirty = path.text != currentDefaultPath ||
                  selectedComponent != currentSettings.targetAppPackage ||
                  keepSelection != currentSettings.keepSelection ||
                  showThumbnails != currentSettings.showThumbnails ||
                  checkLowStorage != currentSettings.checkLowStorage ||
                  quickOpen != currentSettings.quickOpen ||
                  filterMode != currentSettings.filterMode ||
                  customExtensions != currentSettings.customExtensions
                  
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Handle Back Press
    BackHandler {
        if (pageState == SettingsPage.APP_SELECTION) {
            pageState = SettingsPage.MAIN
        } else {
            if (isDirty) {
                showUnsavedDialog = true
            } else {
                onBack()
            }
        }
    }
    
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to save them?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    validateAndSave(goBackAfterSave = true)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showUnsavedDialog = false
                    onBack() 
                }) { Text("Discard") }
            },
        )
    }
    

    // App Selection State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }
    
    if (showClearSelectionWarning) {
        AlertDialog(
            onDismissRequest = { showClearSelectionWarning = false },
            title = { Text("Disable Keep Selection?") },
            text = { Text("Disabling this will clear your current selection when you save changes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        keepSelection = false
                        showClearSelectionWarning = false
                    }
                ) {
                    Text("Turn Off")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSelectionWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (pageState == SettingsPage.APP_SELECTION && isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search apps...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                             modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                        )
                    } else {
                         Text(if (pageState == SettingsPage.MAIN) "Settings" else "Select App") 
                    }
                },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = {
                            if (pageState == SettingsPage.APP_SELECTION) {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    searchQuery = ""
                                } else {
                                    pageState = SettingsPage.MAIN
                                }
                            } else {
                                if (isDirty) {
                                    showUnsavedDialog = true
                                } else {
                                    onBack()
                                }
                            }
                        },
                        tooltip = "Back"
                    ) {
                        Icon(if (isSearchActive) Icons.Default.ArrowBack else Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pageState == SettingsPage.MAIN) {
                         TextButton(onClick = { validateAndSave(goBackAfterSave = false) }) {
                             Text("Save")
                         }
                    } else {
                         // APP SELECTION ACTIONS
                         if (!isSearchActive) {
                             IconButton(onClick = { isSearchActive = true }) {
                                 Icon(Icons.Default.Search, contentDescription = "Search")
                             }
                         } else {
                             IconButton(onClick = { 
                                 searchQuery = "" 
                             }) {
                                 Icon(Icons.Default.Close, contentDescription = "Clear")
                             }
                         }
                    }
                }
            )
        }
    ) { paddingValues ->
        Crossfade(
            targetState = pageState,
            label = "SettingsNavigation",
            modifier = Modifier.padding(paddingValues)
        ) { targetPage ->
            if (targetPage == SettingsPage.MAIN) {
                // MAIN PAGE content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Section: Target App
                    item(key = "header_target_app") {
                        Text(
                            text = "Target App",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    item(key = "selector_app") {
                        val currentApp = apps.find { 
                            val id = "${it.packageName}/${it.activityName}"
                            id == selectedComponent || it.packageName == selectedComponent
                        }
                        
                        ListItem(
                            headlineContent = { 
                                val displayText = if (currentApp != null) currentApp.name 
                                                  else if (isLoadingApps && selectedComponent != null) "Loading..."
                                                  else "Select App"
                                Text(displayText)
                            },
                            supportingContent = { 
                                val displayPkg = if (currentApp != null) currentApp.packageName 
                                                 else if (selectedComponent != null && !isLoadingApps) "App not found ($selectedComponent)"
                                                 else if (selectedComponent != null) "Loading..."
                                                 else "No app selected"
                                Text(displayPkg, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                            },
                            leadingContent = {
                                if (currentApp != null) {
                                    Image(
                                        painter = rememberDrawablePainter(drawable = currentApp.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    // Placeholder
                                    Box(
                                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                         if (isLoadingApps && selectedComponent != null) {
                                              // Loading
                                              Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                         } else if (selectedComponent != null) {
                                              // Warning / Not Found
                                              Icon(Icons.Default.Close, contentDescription = "Not Found", tint = MaterialTheme.colorScheme.error)
                                         } else {
                                              // Empty / Check
                                              Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                         }
                                    }
                                }
                            },
                            trailingContent = {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Select App")
                            },
                            modifier = Modifier.clickable { pageState = SettingsPage.APP_SELECTION }
                        )
                    }

                    item(key = "divider_1") {
                         Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Section: General
                    item(key = "header_general") {
                        Text(
                            text = "General",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    item(key = "input_path") {
                        // Path Input
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            com.foss.simpleshare.ui.components.PathAutocomplete(
                                value = path,
                                onValueChange = { 
                                    path = it
                                    isPathError = false
                                },
                                onPathChange = { /* handled by onValueChange state update */ },
                                isError = isPathError,
                                label = { 
                                    Text(
                                        if (isPathError) "Invalid Path" else "Default Path",
                                        modifier = Modifier.padding(horizontal = 4.dp).background(MaterialTheme.colorScheme.background)
                                    ) 
                                },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    TextButton(onClick = { 
                                        val newPath = currentBrowserPath
                                        path = androidx.compose.ui.text.input.TextFieldValue(
                                            text = newPath,
                                            selection = androidx.compose.ui.text.TextRange(newPath.length)
                                        )
                                        isPathError = false
                                    }) {
                                        Text("Use Current")
                                    }
                                }
                            )
                        }
                    }

                    item(key = "switch_keep_selection") {
                        ListItem(
                            headlineContent = { Text("Keep Selection") },
                            supportingContent = { Text("Maintain selection across folders") },
                            trailingContent = {
                                Switch(
                                    checked = keepSelection,
                                    onCheckedChange = { newValue ->
                                        if (!newValue && selectedFileCount > 0) showClearSelectionWarning = true
                                        else keepSelection = newValue
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (keepSelection && selectedFileCount > 0) showClearSelectionWarning = true
                                else keepSelection = !keepSelection
                            }
                        )
                    }

                    item(key = "switch_thumbnails") {
                        ListItem(
                            headlineContent = { Text("Thumbnails") },
                            supportingContent = { Text("Show image previews") },
                            trailingContent = {
                                Switch(
                                    checked = showThumbnails,
                                    onCheckedChange = { showThumbnails = it }
                                )
                            },
                            modifier = Modifier.clickable { showThumbnails = !showThumbnails }
                        )
                    }

                    item(key = "switch_check_storage") {
                        ListItem(
                            headlineContent = { Text("Validate Free Space") },
                            supportingContent = { Text("Check if internal storage has enough space for a copy of the selected files (Some apps might copy shared files to cache)") },
                            trailingContent = {
                                Switch(
                                    checked = checkLowStorage,
                                    onCheckedChange = { checkLowStorage = it }
                                )
                            },
                            modifier = Modifier.clickable { checkLowStorage = !checkLowStorage }
                        )
                    }

                    item(key = "switch_quick_open") {
                        ListItem(
                            headlineContent = { Text("Quick Open") },
                            supportingContent = { Text("Long press to open file") },
                            trailingContent = {
                                Switch(
                                    checked = quickOpen,
                                    onCheckedChange = { quickOpen = it }
                                )
                            },
                            modifier = Modifier.clickable { quickOpen = !quickOpen }
                        )
                    }

                    item(key = "divider_2") {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Section: Filter
                    item(key = "header_filters") {
                        Text(
                            text = "File Visibility",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    item(key = "filter_selector") {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            com.foss.simpleshare.ui.components.FilterModeSelector(
                                selectedMode = filterMode.name,
                                customExtensions = customExtensions,
                                onModeSelected = { filterMode = FilterMode.valueOf(it) },
                                onCustomExtensionsChanged = { 
                                    customExtensions = it
                                    isCustomExtError = false
                                },
                                isError = isCustomExtError
                            )
                        }
                    }

                    item(key = "divider_3") {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    // Section: Support
                    item(key = "header_support") {
                        Text(
                            text = "Support",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }

                    item(key = "item_github") {
                        ListItem(
                            headlineContent = { Text("GitHub Project") },
                            supportingContent = { Text("View source code and contributors") },
                            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/NightMean/Android-SimpleShare")
                            }
                        )
                    }

                    item(key = "item_report") {
                        ListItem(
                            headlineContent = { Text("Report an Issue") },
                            supportingContent = { Text("Found a bug? Let us know on GitHub") },
                            leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null) },
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/NightMean/Android-SimpleShare/issues")
                            }
                        )
                    }

                    item(key = "footer_reset") {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 TextButton(
                                     onClick = { 
                                         path = androidx.compose.ui.text.input.TextFieldValue(
                                            text = currentDefaultPath,
                                            selection = androidx.compose.ui.text.TextRange(currentDefaultPath.length)
                                         ) // Reset path to default
                                         onReset() 
                                     }
                                 ) {
                                     Text("Reset to Defaults", color = MaterialTheme.colorScheme.error)
                                 }
                                 
                                 Spacer(modifier = Modifier.height(16.dp))
                                 
                                 val versionName = BuildConfig.VERSION_NAME
                                 val isDebug = BuildConfig.DEBUG
                                 val channel = if (isDebug) "Dev" else "Beta"
                                 Text(
                                     text = "Version $versionName ($channel)",
                                     style = MaterialTheme.typography.labelSmall,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                             }
                        }
                    }
                }
            } else {
                // APP SELECTION PAGE
                com.foss.simpleshare.ui.components.AppList(
                    apps = apps,
                    searchQuery = searchQuery,
                    selectedPackage = selectedComponent,
                    onAppSelected = { 
                        selectedComponent = it
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        }
                        pageState = SettingsPage.MAIN
                    }
                )
            }
        }
    }
}
// End of file
