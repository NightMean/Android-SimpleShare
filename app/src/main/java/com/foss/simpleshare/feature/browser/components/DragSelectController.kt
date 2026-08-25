package com.foss.simpleshare.feature.browser.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.foss.simpleshare.data.FileModel

/**
 * State machine for the long-press drag-to-select gesture.
 *
 * Owns all drag-selection state so the screen only forwards raw gesture events.
 * Selection computation itself is pure (see [computeDragSelection]/[syncSelection]).
 */
class DragSelectController(
    private val displayedFilesProvider: () -> List<FileModel>,
    private val selectedFiles: SnapshotStateList<FileModel>,
    private val onQuickOpen: (Int) -> Unit
) {
    var isSelecting by mutableStateOf(false)
        private set
    var dragStartInfo by mutableStateOf<Pair<Int, Set<String>>?>(null)
        private set
    var currentDragIndex by mutableStateOf<Int?>(null)
        private set
    var lastDragPosition by mutableStateOf<Offset?>(null)
        private set

    /** Whether the finger moved past touch slop during the active drag. */
    var hasMovedBeyondTarget: Boolean = false
        private set

    /**
     * Begin a drag at [index] unless it lands on a directory (not selectable).
     */
    fun onDragStart(index: Int, position: Offset) {
        val files = displayedFilesProvider()
        if (index < 0 || index >= files.size || files[index].isDirectory) return

        isSelecting = true
        hasMovedBeyondTarget = false
        dragStartInfo = index to selectedFiles.map { it.path }.toSet()
        currentDragIndex = index
        lastDragPosition = position
    }

    fun markMoved() {
        hasMovedBeyondTarget = true
    }

    fun onDrag(position: Offset, indexAtPosition: Int?) {
        if (!isSelecting) return
        lastDragPosition = position
        if (indexAtPosition != null) {
            currentDragIndex = indexAtPosition
        }
    }

    fun onDragEnd(quickOpenEnabled: Boolean) {
        // No significant movement means this was a plain long-press release;
        // with Quick Open enabled that opens the file under the finger.
        val startIndex = dragStartInfo?.first
        if (quickOpenEnabled && !hasMovedBeyondTarget && startIndex != null) {
            onQuickOpen(startIndex)
        }
        reset()
    }

    fun onDragCancel() = reset()

    /**
     * Recompute the target selection from the current drag range and sync it
     * into [selectedFiles]. Call after any drag state change.
     */
    fun applySelection() {
        val startInfo = dragStartInfo ?: return
        val currentIndex = currentDragIndex ?: return
        if (currentIndex < 0 || currentIndex >= displayedFilesProvider().size) return

        val targetPaths = computeDragSelection(
            displayedFiles = displayedFilesProvider(),
            startIndex = startInfo.first,
            currentIndex = currentIndex,
            initialSelection = startInfo.second
        )
        syncSelection(selectedFiles, targetPaths, displayedFilesProvider())
    }

    private fun reset() {
        isSelecting = false
        dragStartInfo = null
        currentDragIndex = null
        lastDragPosition = null
        hasMovedBeyondTarget = false
    }
}

/** Factory hook for use inside composables. */
@Composable
fun rememberDragSelectController(
    displayedFilesProvider: () -> List<FileModel>,
    selectedFiles: SnapshotStateList<FileModel>,
    onQuickOpen: (Int) -> Unit
): DragSelectController = androidx.compose.runtime.remember {
    DragSelectController(displayedFilesProvider, selectedFiles, onQuickOpen)
}
