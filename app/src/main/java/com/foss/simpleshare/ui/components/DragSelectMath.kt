package com.foss.simpleshare.ui.components

import com.foss.simpleshare.data.FileModel

/**
 * Pure computation for the long-press drag-select gesture in the file browser.
 *
 * Given the item where the drag started, the item currently under the finger,
 * and the selection as it was when the drag started, returns the full target
 * selection: everything that was selected plus every regular file in the
 * dragged range (directories are never selectable).
 */
fun computeDragSelection(
    displayedFiles: List<FileModel>,
    startIndex: Int,
    currentIndex: Int,
    initialSelection: Set<String>
): Set<String> {
    val min = minOf(startIndex, currentIndex).coerceIn(0, (displayedFiles.size - 1).coerceAtLeast(0))
    val max = maxOf(startIndex, currentIndex).coerceIn(0, (displayedFiles.size - 1).coerceAtLeast(0))

    val result = initialSelection.toMutableSet()
    for (i in min..max) {
        val file = displayedFiles[i]
        if (!file.isDirectory) {
            result.add(file.path)
        }
    }
    return result
}

/**
 * Synchronize a mutable selection list to exactly [targetPaths], resolving the
 * FileModel instances to add from [sourceFiles]. No-op when already in sync.
 */
fun syncSelection(
    currentSelection: MutableList<FileModel>,
    targetPaths: Set<String>,
    sourceFiles: List<FileModel>
) {
    val currentPaths = currentSelection.map { it.path }.toSet()
    if (currentPaths == targetPaths) return

    // 1. Remove items no longer selected
    currentSelection.removeAll { it.path !in targetPaths }

    // 2. Add items newly selected
    targetPaths.forEach { path ->
        if (currentSelection.none { it.path == path }) {
            sourceFiles.find { it.path == path }?.let { currentSelection.add(it) }
        }
    }
}
