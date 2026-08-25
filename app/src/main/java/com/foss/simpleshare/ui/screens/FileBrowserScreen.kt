package com.foss.simpleshare.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.foss.simpleshare.ui.components.FastScrollbar
import com.foss.simpleshare.ui.components.TooltipIconButton
import com.foss.simpleshare.ui.components.TooltipPosition
import com.foss.simpleshare.ui.components.computeDragSelection
import com.foss.simpleshare.ui.components.computeGridScrollProgress
import com.foss.simpleshare.ui.components.computeListScrollProgress
import com.foss.simpleshare.ui.components.gridItemIndexAtOffset
import com.foss.simpleshare.ui.components.listItemIndexAtOffset
import com.foss.simpleshare.ui.components.syncSelection
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.geometry.Offset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.foss.simpleshare.data.FileModel
import com.foss.simpleshare.data.FileRepository
import com.foss.simpleshare.data.SortOption
import com.foss.simpleshare.data.filterBySearch
import com.foss.simpleshare.data.sortFiles
import com.foss.simpleshare.share.FileSharer
import com.foss.simpleshare.ui.components.FileGridItem
import com.foss.simpleshare.ui.components.FileListItem
import com.foss.simpleshare.utils.StorageUtils
import com.foss.simpleshare.ui.components.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.foss.simpleshare.R




@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FileBrowserScreen(
    repository: FileRepository,
    currentPath: String,
    onPathChange: (String) -> Unit,
    selectedFiles: MutableList<FileModel>, 
    targetAppPackageName: String?,
    keepSelection: Boolean,
    showThumbnails: Boolean,
    checkLowStorage: Boolean,
    quickOpen: Boolean,
    allowedExtensions: Set<String>, // New filter parameter
    isGridView: Boolean,
    onViewModeChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    sortOption: SortOption,
    isSortAscending: Boolean,
    sortFoldersFirst: Boolean,
    onSortChange: (SortOption, Boolean, Boolean) -> Unit
) {
    var rawFiles by remember { mutableStateOf(emptyList<FileModel>()) }
    var isLoading by remember { mutableStateOf(true) } // Track loading state
    // var isGridView by remember { mutableStateOf(false) } // Hoisted to MainActivity
    var showLowSpaceDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope() // Moved up
    
    // Deletion State
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    var deletedCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    // Logic States
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Load badge icon
    var targetAppIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    
    LaunchedEffect(targetAppPackageName) {
        if (targetAppPackageName != null) {
            withContext(Dispatchers.IO) {
                try {
                    if (targetAppPackageName.contains("/")) {
                        val split = targetAppPackageName.split("/")
                        val componentName = android.content.ComponentName(split[0], split[1])
                        targetAppIcon = context.packageManager.getActivityIcon(componentName)
                    } else {
                         targetAppIcon = context.packageManager.getApplicationIcon(targetAppPackageName)
                    }
                } catch (e: Exception) {
                    try {
                        val packageName = targetAppPackageName.substringBefore("/")
                        targetAppIcon = context.packageManager.getApplicationIcon(packageName)
                    } catch (e2: Exception) {
                         targetAppIcon = null
                    }
                }
            }
        } else {
            targetAppIcon = null
        }
    }



    // Active refresh job so a double-tap on Refresh cannot race itself
    var refreshJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun refreshFiles() {
        refreshJob?.cancel()
        refreshJob = coroutineScope.launch {
            isLoading = true
            // Directory listing does disk I/O; keep it off the main thread
            val freshFiles = repository.listFilesWithCachedSizes(currentPath, allowedExtensions)

            rawFiles = freshFiles

            // Prune selection: Remove files that no longer exist
            val iterator = selectedFiles.iterator()
            var removedCount = 0
            while (iterator.hasNext()) {
                val file = iterator.next()
                if (!file.file.exists()) {
                    iterator.remove()
                    removedCount++
                }
            }

            if (removedCount > 0) {
                Toast.makeText(context, context.getString(R.string.toast_selection_updated, removedCount), Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }
    
    // Deletion Logic
    fun onDeleteConfirmed() {
        showDeleteConfirmDialog = false
        isDeleting = true
        
        val filesToDelete = selectedFiles.toList() // Copy list
        
        coroutineScope.launch {
            // If many files, maybe show progress logic, but deleteFiles is atomic-ish in our repo currently.
            // For better UX on large lists, we could chunk it or move logic here.
            // For now, simple bulk delete.
            val count = repository.deleteFiles(filesToDelete)
            
            withContext(Dispatchers.Main) {
                isDeleting = false
                deletedCount = count
                Toast.makeText(context, context.getString(R.string.toast_deleted, count), Toast.LENGTH_SHORT).show()
                selectedFiles.clear()
                refreshFiles()
            }
        }
    }

    // Dialogs (Moved from derivedStateOf)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_message, selectedFiles.size)) },
            confirmButton = {
                TextButton(onClick = { onDeleteConfirmed() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
        
    if (isDeleting) {
            AlertDialog(
            onDismissRequest = { }, // Prevent dismiss
            title = { Text(stringResource(R.string.deleting_title)) },
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.deleting_message))
                }
            },
            confirmButton = {}
        )
    }

    // Load files (with cached sizes applied for folders)
    LaunchedEffect(currentPath, allowedExtensions) {
        isLoading = true
        rawFiles = repository.listFilesWithCachedSizes(currentPath, allowedExtensions)
        isLoading = false
    }

    // Async folder details (size + child count): check cache first, then calculate if needed
    LaunchedEffect(rawFiles) {
        val pendingFolders = rawFiles.filter { it.isDirectory && (it.size == -1L || it.itemCount == -1) }
        if (pendingFolders.isEmpty()) return@LaunchedEffect

        pendingFolders.forEach { folder ->
            launch {
                val size = if (folder.size == -1L) {
                    withContext(Dispatchers.IO) {
                        repository.getCachedSize(folder.path)
                            ?: repository.calculateAndCacheSize(folder.path)
                    }
                } else folder.size

                val itemCount = if (folder.itemCount == -1) {
                    // file.list() reads names only — cheaper than listFiles()
                    withContext(Dispatchers.IO) { java.io.File(folder.path).list()?.size ?: 0 }
                } else folder.itemCount

                rawFiles = rawFiles.map { file ->
                    if (file.path == folder.path) file.copy(size = size, itemCount = itemCount) else file
                }
            }
        }
    }
    

    // Filter and Sort Logic (pure functions, see data/FileListOps.kt)
    val displayedFiles by remember(rawFiles, searchQuery, sortOption, isSortAscending, sortFoldersFirst) {
        derivedStateOf {
            sortFiles(
                files = filterBySearch(rawFiles, searchQuery),
                option = sortOption,
                ascending = isSortAscending,
                foldersFirst = sortFoldersFirst
            )
        }
    }

    // Handle Back Press
    val isAtRoot = File(currentPath).absolutePath == File(repository.getDefaultPath()).absolutePath
    BackHandler(enabled = !isAtRoot || isSearchActive) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            val parent = File(currentPath).parent
            if (parent != null) {
                if (!keepSelection) selectedFiles.clear()
                onPathChange(parent)
            }
        }
    }

    fun handleFileClick(file: FileModel) {
        if (file.isDirectory) {
            if (!keepSelection) selectedFiles.clear()
            // Clear search on navigation
            isSearchActive = false
            searchQuery = ""
            onPathChange(file.path)
        } else {
            val index = selectedFiles.indexOfFirst { it.path == file.path }
            if (index != -1) {
                selectedFiles.removeAt(index)
                // Safety: Ensure no other copies exist
                selectedFiles.removeAll { it.path == file.path }
            } else {
                // Ensure not already added (redundant but safe)
                if (selectedFiles.none { it.path == file.path }) {
                    selectedFiles.add(file)
                }
            }
        }
    }

    fun onProceedClick() {
        if (checkLowStorage) {
            val totalSize = selectedFiles.sumOf { it.size }
            // Check the volume currently being browsed, not a hardcoded one
            val availableSpace = StorageUtils.getAvailableStorage(currentPath)

            if (availableSpace < totalSize) {
                showLowSpaceDialog = true
            } else {
                FileSharer.shareFiles(context, selectedFiles, targetAppPackageName)
            }
        } else {
            FileSharer.shareFiles(context, selectedFiles, targetAppPackageName)
        }
    }

    fun openFile(fileModel: FileModel) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                fileModel.file
            )
            
            val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileModel.extension.lowercase()) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_cannot_open_file, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    val selectedPaths by remember { derivedStateOf { selectedFiles.map { it.path }.toSet() } }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    // val coroutineScope = rememberCoroutineScope() // Moved up to line 142
    
    // Auto-scroll to top when sort options change
    LaunchedEffect(sortOption, isSortAscending, sortFoldersFirst) {
        if (isGridView) {
            gridState.scrollToItem(0)
        } else {
            listState.scrollToItem(0)
        }
    }

    // Reset scroll to the top when entering a different folder. The lazy list
    // states survive folder changes within this composition; without this reset,
    // a large folder can open scrolled to the previous position (e.g. 10-15th
    // item) because the old first-visible key no longer exists.
    LaunchedEffect(currentPath) {
        listState.scrollToItem(0)
        gridState.scrollToItem(0)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background, // Match app background
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    if (isSearchActive) {
                        val focusRequester = remember { FocusRequester() }
                        val keyboardController = LocalSoftwareKeyboardController.current

                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
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
                        val file = File(currentPath)
                        val titleText = if (file.absolutePath == File(repository.getDefaultPath()).absolutePath || file.name == "0") stringResource(R.string.title_internal_storage) else file.name
                        Text(
                            text = titleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = { 
                            isSearchActive = false 
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_close_search))
                        }
                    } else if (File(currentPath).absolutePath != File(repository.getDefaultPath()).absolutePath) {
                        TooltipIconButton(onClick = {
                            val parent = File(currentPath).parent
                            if (parent != null) {
                                if (!keepSelection) selectedFiles.clear()
                                onPathChange(parent)
                            }
                        }, tooltip = stringResource(R.string.tooltip_go_back)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
                actions = {
                    if (isSearchActive && searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear))
                        }
                    } else {
                        // Delete Button (Only when selection active)
                        if (selectedFiles.isNotEmpty()) {
                            TooltipIconButton(
                                onClick = { showDeleteConfirmDialog = true }, 
                                tooltip = stringResource(R.string.tooltip_delete)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.delete_24),
                                    contentDescription = stringResource(R.string.cd_delete),
                                    tint = Color.Red
                                )
                            }
                        }
                        
                        TooltipIconButton(onClick = onSettingsClick, tooltip = "Settings") {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Toggle
                    TooltipIconButton(onClick = { onViewModeChange(!isGridView) }, tooltip = stringResource(if (isGridView) R.string.tooltip_list_view else R.string.tooltip_grid_view), position = TooltipPosition.Above) {
                        Icon(if (isGridView) Icons.Default.List else Icons.Default.GridView, contentDescription = "Toggle View")
                    }

                    // Refresh
                    TooltipIconButton(onClick = { refreshFiles() }, tooltip = stringResource(R.string.tooltip_refresh), position = TooltipPosition.Above) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }

                    // Select All
                    TooltipIconButton(onClick = { 
                        if (displayedFiles.isNotEmpty()) {
                             val filesToConsider = displayedFiles.filter { !it.isDirectory }
                             
                             // Optimization: Use a Set for fast lookup of selected paths
                             val selectedPathsSet = selectedFiles.map { it.path }.toHashSet()
                             
                             // Check if all displayed files are already selected
                             val allSelected = filesToConsider.all { it.path in selectedPathsSet }
                             
                             if (allSelected) {
                                  // Deselect All: efficient remove
                                  val pathsToRemove = filesToConsider.map { it.path }.toHashSet()
                                  selectedFiles.removeAll { it.path in pathsToRemove }
                             } else {
                                  // Select All: efficient add
                                  // Find files that are NOT yet selected
                                  val filesToAdd = filesToConsider.filter { it.path !in selectedPathsSet }
                                  selectedFiles.addAll(filesToAdd)
                             }
                        }
                    }, tooltip = stringResource(R.string.tooltip_select_all), position = TooltipPosition.Above) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                    }

                    // Sort
                    Box {
                        TooltipIconButton(onClick = { showSortMenu = true }, tooltip = stringResource(R.string.tooltip_sort), position = TooltipPosition.Above) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_folders_first)) },
                                onClick = {
                                    onSortChange(sortOption, isSortAscending, !sortFoldersFirst)
                                    showSortMenu = false
                                },
                                trailingIcon = {
                                    if (sortFoldersFirst) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                            androidx.compose.material3.Divider()
                            
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
                                            if (sortOption == option) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    // In many apps: Arrow UP = Ascending (A->Z, 0->9). Arrow DOWN = Descending.
                                                    // Let's use ArrowUpward for Ascending.
                                                    imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = { 
                                        if (sortOption == option) {
                                            onSortChange(sortOption, !isSortAscending, sortFoldersFirst)
                                        } else {
                                            onSortChange(option, true, sortFoldersFirst)
                                        }
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Search
                    TooltipIconButton(onClick = { isSearchActive = true }, tooltip = stringResource(R.string.tooltip_search), position = TooltipPosition.Above) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { onProceedClick() },
                    icon = { Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share)) },
                    text = { 
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.fab_share_count, selectedFiles.size))
                            if (targetAppIcon != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Image(
                                    painter = rememberDrawablePainter(drawable = targetAppIcon),
                                    contentDescription = stringResource(R.string.cd_target_app),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) { // Enforce background
            
            if (displayedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            isSearchActive -> stringResource(R.string.empty_no_results)
                            isLoading -> stringResource(R.string.empty_loading)
                            else -> stringResource(R.string.empty_no_files)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Drag Selection Logic
                var isDragSelecting by remember { mutableStateOf(false) }
                var dragStartInfo by remember { mutableStateOf<Pair<Int, Set<String>>?>(null) } // Start Index + Initial Selection
                var currentDragIndex by remember { mutableStateOf<Int?>(null) }
                var lastDragPosition by remember { mutableStateOf<Offset?>(null) }

                val hapticFeedback = LocalHapticFeedback.current
                val density = LocalDensity.current

                // Helper to get index from offset
                fun getItemIndexFromOffset(offset: Offset): Int? {
                    return if (isGridView) {
                        gridItemIndexAtOffset(gridState.layoutInfo, offset.x, offset.y)
                    } else {
                        listItemIndexAtOffset(listState.layoutInfo, offset.y)
                    }
                }

                // Auto Scroll Logic
                LaunchedEffect(isDragSelecting, lastDragPosition) {
                    if (isDragSelecting && lastDragPosition != null) {
                        val viewportHeight = if (isGridView) gridState.layoutInfo.viewportSize.height else listState.layoutInfo.viewportSize.height
                        val topHotZone = with(density) { 60.dp.toPx() }
                        val bottomHotZone = viewportHeight - topHotZone
                        
                        val y = lastDragPosition!!.y
                        
                        if (y < topHotZone) {
                             while (isDragSelecting && lastDragPosition!!.y < topHotZone) {
                                 val speed = (topHotZone - lastDragPosition!!.y) * 0.5f // rudimentary speed
                                 if (isGridView) gridState.scrollBy(-speed) else listState.scrollBy(-speed)
                                 // Update selection during scroll
                                 currentDragIndex = getItemIndexFromOffset(lastDragPosition!!) ?: currentDragIndex
                                 kotlinx.coroutines.delay(16)
                             }
                        } else if (y > bottomHotZone) {
                            while (isDragSelecting && lastDragPosition!!.y > bottomHotZone) {
                                 val speed = (lastDragPosition!!.y - bottomHotZone) * 0.5f
                                 if (isGridView) gridState.scrollBy(speed) else listState.scrollBy(speed)
                                 // Update selection during scroll
                                 currentDragIndex = getItemIndexFromOffset(lastDragPosition!!) ?: currentDragIndex
                                 kotlinx.coroutines.delay(16)
                            }
                        }
                    }
                }
                
                // Update Selection Effect
                LaunchedEffect(dragStartInfo, currentDragIndex) {
                    val startInfo = dragStartInfo
                    val currentIndex = currentDragIndex
                    if (startInfo != null && currentIndex != null && currentIndex >= 0 && currentIndex < displayedFiles.size) {
                        val (startIndex, initialSelection) = startInfo

                        // New Selection = Initial + Range (pure computation)
                        val newSelectionPaths = computeDragSelection(
                            displayedFiles = displayedFiles,
                            startIndex = startIndex,
                            currentIndex = currentIndex,
                            initialSelection = initialSelection
                        )

                        syncSelection(selectedFiles, newSelectionPaths, displayedFiles)
                    }
                }

                var hasDragged by remember { mutableStateOf(false) }
                var dragStartPosition by remember { mutableStateOf<Offset?>(null) }
                var lastDragEndTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) } // Debounce for click after drag
                var pressedItemIndex by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
                val viewConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current

                fun handleFileClickWithDebounce(file: FileModel) {
                     // If a drag/long-press is active OR just finished, ignore this click
                     // Lowered debounce to 50ms to ensure manual taps are registered
                     if (!isDragSelecting && System.currentTimeMillis() - lastDragEndTime > 50) {
                         handleFileClick(file)
                     }
                }

                Box(
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(isGridView) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val index = getItemIndexFromOffset(offset)
                                    if (index != null && index >= 0 && index < displayedFiles.size) {
                                        pressedItemIndex = index
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            pressedItemIndex = -1
                                        }
                                    }
                                },
                                onTap = { offset ->
                                    val index = getItemIndexFromOffset(offset)
                                    if (index != null && index >= 0 && index < displayedFiles.size) {
                                        handleFileClickWithDebounce(displayedFiles[index])
                                    }
                                }
                            )
                        }
                        .pointerInput(isGridView, quickOpen) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val index = getItemIndexFromOffset(offset)
                                    if (index != null && index >= 0 && index < displayedFiles.size) {
                                        val item = displayedFiles[index]
                                        
                                        // Disable long-press/drag on folders
                                        if (!item.isDirectory) {
                                            isDragSelecting = true
                                            hasDragged = false // Reset
                                            dragStartPosition = offset
                                            
                                            // Capture initial state
                                            val initialSet = selectedFiles.map { it.path }.toSet()
                                            dragStartInfo = index to initialSet
                                            currentDragIndex = index
                                            lastDragPosition = offset
                                            
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                },
                                onDragEnd = { 
                                    lastDragEndTime = System.currentTimeMillis()
                                    // If we haven't moved significantly, treat as just a Long Press Release (Quick Open)
                                    if (quickOpen && !hasDragged && dragStartInfo != null) {
                                        val startIndex = dragStartInfo!!.first
                                        if (startIndex >= 0 && startIndex < displayedFiles.size) {
                                            openFile(displayedFiles[startIndex])
                                        }
                                    }

                                    isDragSelecting = false 
                                    dragStartInfo = null
                                    currentDragIndex = null
                                    lastDragPosition = null
                                    hasDragged = false
                                    dragStartPosition = null
                                },
                                onDragCancel = { 
                                    lastDragEndTime = System.currentTimeMillis()
                                    isDragSelecting = false 
                                    dragStartInfo = null
                                    currentDragIndex = null
                                    lastDragPosition = null
                                    hasDragged = false
                                    dragStartPosition = null
                                },
                                onDrag = { change, _ ->
                                    lastDragPosition = change.position
                                    val start = dragStartPosition
                                    
                                    // Only mark as dragged if we moved beyond touch slop
                                    if (start != null) {
                                        val distance = (change.position - start).getDistance()
                                        if (distance > viewConfiguration.touchSlop) {
                                            hasDragged = true
                                        }
                                    } else {
                                        // Fallback if start not captured (should impossible)
                                        hasDragged = true
                                    }
                                    
                                    if (hasDragged) {
                                        val index = getItemIndexFromOffset(change.position)
                                        if (index != null) {
                                            currentDragIndex = index
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    if (isGridView) {
                        LazyVerticalGrid(
                            state = gridState,
                            userScrollEnabled = !isDragSelecting, // Prevent scroll interference during drag
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = displayedFiles.size,
                                key = { index -> displayedFiles[index].path }
                            ) { index ->
                                val file = displayedFiles[index]
                                val isSelected = file.path in selectedPaths
                                FileGridItem(
                                    file = file.copy(isSelected = isSelected),
                                    showThumbnail = showThumbnails,
                                    isPressed = (index == pressedItemIndex)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            userScrollEnabled = !isDragSelecting, // Prevent scroll interference during drag
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 24.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                             items(
                                count = displayedFiles.size,
                                key = { index -> displayedFiles[index].path }
                             ) { index ->
                                val file = displayedFiles[index]
                                val isSelected = file.path in selectedPaths
                                FileListItem(
                                    file = file.copy(isSelected = isSelected),
                                    showThumbnail = showThumbnails,
                                    isPressed = (index == pressedItemIndex),
                                    onClick = { handleFileClickWithDebounce(file) }
                                )
                            }
                        }
                    }
                }

                // Fast Scroll Implementation (pure math in ui/components/ScrollMath.kt)
                val scrollStateValues = remember(isGridView, listState, gridState, displayedFiles) {
                    derivedStateOf {
                        if (isGridView) computeGridScrollProgress(gridState.layoutInfo)
                        else computeListScrollProgress(listState.layoutInfo)
                    }
                }

                FastScrollbar(
                    listSize = displayedFiles.size,
                    scrollState = scrollStateValues,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                    getLabelForIndex = { index -> 
                        displayedFiles.getOrNull(index)?.name?.firstOrNull()?.uppercaseChar() ?: '#' 
                    },
                    onScrollTo = { progress ->
                         coroutineScope.launch {
                            val totalItems = if (isGridView) {
                                gridState.layoutInfo.totalItemsCount
                            } else {
                                listState.layoutInfo.totalItemsCount
                            }

                            val itemSize = if (isGridView) {
                                gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
                            } else {
                                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                            }
                            
                            if (itemSize > 0) {
                                val totalPixels = totalItems * itemSize.toFloat()
                                val targetPixels = progress * totalPixels
                                val targetIndex = (targetPixels / itemSize).toInt().coerceIn(0, totalItems - 1)
                                val offset = (targetPixels % itemSize).toInt()
                                
                                if (isGridView) {
                                    gridState.scrollToItem(targetIndex, -offset)
                                } else {
                                    listState.scrollToItem(targetIndex, -offset)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    if (showLowSpaceDialog) {
        AlertDialog(
            onDismissRequest = { showLowSpaceDialog = false },
            title = { Text(stringResource(R.string.low_space_title)) },
            text = { Text(stringResource(R.string.low_space_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLowSpaceDialog = false
                        FileSharer.shareFiles(context, selectedFiles, targetAppPackageName)
                    }
                ) {
                    Text(stringResource(R.string.action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLowSpaceDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    if (targetAppPackageName == null) {
        AlertDialog(
            onDismissRequest = { }, 
            title = { Text(stringResource(R.string.setup_required_title)) },
            text = { Text(stringResource(R.string.setup_required_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onSettingsClick() }
                ) {
                    Text(stringResource(R.string.action_select_app))
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }
}
