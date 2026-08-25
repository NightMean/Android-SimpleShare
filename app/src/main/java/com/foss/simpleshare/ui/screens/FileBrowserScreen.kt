package com.foss.simpleshare.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.foss.simpleshare.ui.components.FastScrollbar
import com.foss.simpleshare.ui.components.TooltipIconButton
import com.foss.simpleshare.ui.components.TooltipPosition
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
import com.foss.simpleshare.ui.components.FileGridItem
import com.foss.simpleshare.ui.components.FileListItem
import com.foss.simpleshare.utils.StorageUtils
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

import androidx.compose.ui.res.painterResource
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

    var deletedCount by remember { mutableStateOf(0) }

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



    fun refreshFiles() {
        rawFiles = repository.listFiles(currentPath, allowedExtensions) // Pass extensions
        
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
             Toast.makeText(context, "Selection updated: Removed $removedCount missing files", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Deleted $count files", Toast.LENGTH_SHORT).show()
                selectedFiles.clear()
                refreshFiles()
            }
        }
    }

    // Dialogs (Moved from derivedStateOf)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Files?") },
            text = { Text("Are you sure you want to delete ${selectedFiles.size} selected item(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDeleteConfirmed() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
        
    if (isDeleting) {
            AlertDialog(
            onDismissRequest = { }, // Prevent dismiss
            title = { Text("Deleting...") },
            text = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please wait")
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

    // Async folder size loading: check cache first, then calculate if needed
    LaunchedEffect(rawFiles) {
        val folders = rawFiles.filter { it.isDirectory && it.size == -1L }
        if (folders.isEmpty()) return@LaunchedEffect

        folders.forEach { folder ->
            launch {
                // Calculate on IO thread
                val size = withContext(Dispatchers.IO) {
                    repository.getCachedSize(folder.path)
                        ?: repository.calculateAndCacheSize(folder.path)
                }
                
                // Update UI on main thread (already on Main since withContext returns)
                rawFiles = rawFiles.map { file ->
                    if (file.path == folder.path) file.copy(size = size) else file
                }
            }
        }
    }
    

    // Filter and Sort Logic
    // Filter and Sort Logic

    // Filter and Sort Logic
    val displayedFiles by remember(rawFiles, searchQuery, sortOption, isSortAscending, sortFoldersFirst) {
        derivedStateOf {
            var result = if (searchQuery.isBlank()) {
                rawFiles
            } else {
                rawFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            // Apply Sort Criterion
            result = when (sortOption) {
                SortOption.NAME -> if (isSortAscending) result.sortedBy { it.name.lowercase(Locale.getDefault()) } else result.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
                SortOption.SIZE -> if (isSortAscending) result.sortedBy { it.size } else result.sortedByDescending { it.size }
                SortOption.DATE -> if (isSortAscending) result.sortedBy { it.file.lastModified() } else result.sortedByDescending { it.file.lastModified() }
                SortOption.TYPE -> if (isSortAscending) result.sortedBy { it.extension } else result.sortedByDescending { it.extension }
            }

            // Apply Folders First Priority
            if (sortFoldersFirst) {
                // False < True. So !isDirectory (False for folder) < !isDirectory (True for file)
                // Folders come first.
                result = result.sortedBy { !it.isDirectory }
            }

            result
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
            val availableSpace = StorageUtils.getAvailableStorage()

            if (availableSpace < totalSize) {
                showLowSpaceDialog = true
            } else {
                shareFiles(context, selectedFiles, targetAppPackageName)
            }
        } else {
            shareFiles(context, selectedFiles, targetAppPackageName)
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
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            placeholder = { Text("Search...") },
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
                        val titleText = if (file.absolutePath == File(repository.getDefaultPath()).absolutePath || file.name == "0") "Internal Storage" else file.name
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
                            Icon(Icons.Default.ArrowBack, contentDescription = "Close Search")
                        }
                    } else if (File(currentPath).absolutePath != File(repository.getDefaultPath()).absolutePath) {
                        TooltipIconButton(onClick = {
                            val parent = File(currentPath).parent
                            if (parent != null) {
                                if (!keepSelection) selectedFiles.clear()
                                onPathChange(parent)
                            }
                        }, tooltip = "Go Back") {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isSearchActive && searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    } else {
                        // Delete Button (Only when selection active)
                        if (selectedFiles.isNotEmpty()) {
                            TooltipIconButton(
                                onClick = { showDeleteConfirmDialog = true }, 
                                tooltip = "Delete"
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.delete_24),
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        }
                        
                        TooltipIconButton(onClick = onSettingsClick, tooltip = "Settings") {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                    TooltipIconButton(onClick = { onViewModeChange(!isGridView) }, tooltip = if (isGridView) "List View" else "Grid View", position = TooltipPosition.Above) {
                        Icon(if (isGridView) Icons.Default.List else Icons.Default.GridView, contentDescription = "Toggle View")
                    }

                    // Refresh
                    TooltipIconButton(onClick = { refreshFiles() }, tooltip = "Refresh", position = TooltipPosition.Above) {
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
                    }, tooltip = "Select All", position = TooltipPosition.Above) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                    }

                    // Sort
                    Box {
                        TooltipIconButton(onClick = { showSortMenu = true }, tooltip = "Sort", position = TooltipPosition.Above) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Folders first") },
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
                    TooltipIconButton(onClick = { isSearchActive = true }, tooltip = "Search", position = TooltipPosition.Above) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { onProceedClick() },
                    icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                    text = { 
                         Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Share (${selectedFiles.size})")
                            if (targetAppIcon != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Image(
                                    painter = rememberDrawablePainter(drawable = targetAppIcon),
                                    contentDescription = "Target App",
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
                            isSearchActive -> "No results found"
                            isLoading -> "Loading..."
                            else -> "No files found"
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
                        gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            val x = item.offset.x
                            val y = item.offset.y
                            // Grid items can be complex, but simple bounds check usually suffices
                            offset.x >= x && offset.x <= x + item.size.width &&
                            offset.y >= y && offset.y <= y + item.size.height
                        }?.index
                    } else {
                        listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            val y = item.offset
                            // List items span full width usually
                            offset.y >= y && offset.y <= y + item.size
                        }?.index
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
                        val min = minOf(startIndex, currentIndex)
                        val max = maxOf(startIndex, currentIndex)
                        
                        // New Selection = Initial + Range
                        val newSelectionPaths = initialSelection.toMutableSet()
                        for (i in min..max) {
                            if (!displayedFiles[i].isDirectory) {
                                newSelectionPaths.add(displayedFiles[i].path)
                            }
                        }
                        
                        // Sync to selectedFiles
                        // We need to match the logic of adding/removing
                        // Optimization: Only update if changed significantly? 
                        // But selectedFiles is a mutable list, we need to clear and re-add or diff?
                        // Simplest: Rebuild
                        
                        val currentSet = selectedFiles.map { it.path }.toSet()
                        if (currentSet != newSelectionPaths) {
                             // This is heavy if list is huge, but for reasonable lists it's fine.
                             // More efficient:
                             // 1. Remove items not in new
                             selectedFiles.removeAll { it.path !in newSelectionPaths }
                             // 2. Add items in new that are missing
                             newSelectionPaths.forEach { path ->
                                 if (selectedFiles.none { it.path == path }) {
                                      displayedFiles.find { it.path == path }?.let { selectedFiles.add(it) }
                                 }
                             }
                        }
                    }
                }

                var hasDragged by remember { mutableStateOf(false) }
                var dragStartPosition by remember { mutableStateOf<Offset?>(null) }
                var lastDragEndTime by remember { mutableStateOf(0L) } // Debounce for click after drag
                var pressedItemIndex by remember { mutableStateOf(-1) }
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

                // Fast Scroll Implementation
                val scrollStateValues = remember(isGridView, listState, gridState, displayedFiles) {
                    derivedStateOf {
                        if (isGridView) {
                            val layout = gridState.layoutInfo
                            val totalItems = layout.totalItemsCount
                            
                            val visibleInfo = layout.visibleItemsInfo
                            if (totalItems == 0 || visibleInfo.isEmpty()) {
                                0f to 0f
                            } else {
                                val firstItem = visibleInfo.first()
                                val itemHeight = firstItem.size.height
                                val itemWidth = firstItem.size.width
                                val viewportWidth = layout.viewportSize.width
                                val viewportHeight = layout.viewportSize.height.toFloat()
                                
                                if (itemHeight <= 0 || itemWidth <= 0) {
                                    0f to 0f 
                                } else {
                                    val spanCount = (viewportWidth / itemWidth).coerceAtLeast(1)
                                    val totalRows = (totalItems + spanCount - 1) / spanCount
                                    
                                    // Use Row Index for calculation
                                    val currentRow = gridState.firstVisibleItemIndex / spanCount
                                    val rowOffset = gridState.firstVisibleItemScrollOffset
                                    
                                    val contentHeight = totalRows * itemHeight.toFloat()
                                    val scrollOffset = (currentRow * itemHeight) + rowOffset
                                    
                                    val fraction = (viewportHeight / contentHeight).coerceIn(0f, 1f)
                                    val progress = if (contentHeight > viewportHeight) 
                                        (scrollOffset / (contentHeight - viewportHeight)).coerceIn(0f, 1f)
                                    else 0f
                                    
                                    progress to fraction
                                }
                            }
                        } else {
                            val layout = listState.layoutInfo
                            val totalItems = layout.totalItemsCount
                            
                            val visibleInfo = layout.visibleItemsInfo
                            if (totalItems == 0 || visibleInfo.isEmpty()) {
                                0f to 0f
                            } else {
                                val itemHeight = visibleInfo.first().size
                                val viewportHeight = layout.viewportSize.height.toFloat()
                                
                                if (itemHeight <= 0) 0f to 0f
                                else {
                                    val contentHeight = itemHeight.toFloat() * totalItems
                                    val scrollOffset = (listState.firstVisibleItemIndex * itemHeight) + listState.firstVisibleItemScrollOffset
                                    
                                    val fraction = (viewportHeight / contentHeight).coerceIn(0f, 1f)
                                    val progress = if (contentHeight > viewportHeight) 
                                        (scrollOffset / (contentHeight - viewportHeight)).coerceIn(0f, 1f)
                                    else 0f
                                    
                                    progress to fraction
                                }
                            }
                        }
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
            title = { Text("Low Storage Space") },
            text = { Text("There is not enough space left on the device and upload might fail.\n\nProceed anyway?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLowSpaceDialog = false
                        shareFiles(context, selectedFiles, targetAppPackageName)
                    }
                ) {
                    Text("Yes")
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
            title = { Text("Setup Required") },
            text = { Text("You must select a target app to share files with before using the browser.") },
            confirmButton = {
                TextButton(
                    onClick = { onSettingsClick() }
                ) {
                    Text("Select App")
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }
}



fun shareFiles(context: android.content.Context, files: List<FileModel>, targetAppPackageName: String?) {
    if (files.isEmpty()) return

    val uris = ArrayList<Uri>()
    try {
        files.forEach { fileModel ->
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", 
                fileModel.file
            )
            uris.add(uri)
        }

        val intent = Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            val mimeType = if (files.any { it.extension in setOf("mp4", "mkv", "webm", "avi") }) "video/*" else "image/*"
            type = mimeType

            // Older Android versions only honor FLAG_GRANT_READ_URI_PERMISSION for the
            // first EXTRA_STREAM URI, so the target app sees an empty selection for
            // ACTION_SEND_MULTIPLE. Attaching every URI via ClipData grants read
            // access to all of them.
            if (uris.isNotEmpty()) {
                val clipData = android.content.ClipData.newRawUri(null, uris[0])
                for (i in 1 until uris.size) {
                    clipData.addItem(android.content.ClipData.Item(uris[i]))
                }
                this.clipData = clipData
            }

            if (targetAppPackageName != null) {
                if (targetAppPackageName.contains("/")) {
                    val split = targetAppPackageName.split("/")
                    if (split.size == 2) {
                        component = android.content.ComponentName(split[0], split[1])
                    } else {
                         setPackage(targetAppPackageName)
                    }
                } else {
                     setPackage(targetAppPackageName)
                }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val shareIntent = if (targetAppPackageName == null) {
            Intent.createChooser(intent, "Share files")
        } else {
            intent
        }
        
        try {
            context.startActivity(shareIntent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(context, "Selected app not found or nothing available to share.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
         Toast.makeText(context, "Error preparing share: ${e.message}", Toast.LENGTH_LONG).show()
         e.printStackTrace()
    }
}

