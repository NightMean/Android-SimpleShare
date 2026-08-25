package com.foss.simpleshare.data

import java.util.Locale

/** Supported file list sort criteria. */
enum class SortOption {
    NAME, SIZE, DATE, TYPE
}

/**
 * Apply search filtering by name (no-op for blank queries).
 */
fun filterBySearch(files: List<FileModel>, query: String): List<FileModel> =
    if (query.isBlank()) files
    else files.filter { it.name.contains(query, ignoreCase = true) }

/**
 * Sort files by the given criterion. Stable with respect to input order for ties.
 */
fun sortFiles(
    files: List<FileModel>,
    option: SortOption,
    ascending: Boolean,
    foldersFirst: Boolean
): List<FileModel> {
    var result = when (option) {
        SortOption.NAME -> if (ascending) files.sortedBy { it.name.lowercase(Locale.getDefault()) }
        else files.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
        SortOption.SIZE -> if (ascending) files.sortedBy { it.size }
        else files.sortedByDescending { it.size }
        SortOption.DATE -> if (ascending) files.sortedBy { it.file.lastModified() }
        else files.sortedByDescending { it.file.lastModified() }
        SortOption.TYPE -> if (ascending) files.sortedBy { it.extension }
        else files.sortedByDescending { it.extension }
    }

    if (foldersFirst) {
        // False < True, so directories (!isDirectory == false) sort before files.
        result = result.sortedBy { !it.isDirectory }
    }
    return result
}
