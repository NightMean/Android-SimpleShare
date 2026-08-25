package com.foss.simpleshare.data

import java.io.File

data class FileModel(
    val file: File,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val extension: String,
    val itemCount: Int = 0,
    var isSelected: Boolean = false
)

/**
 * Rebuild a [FileModel] from an absolute path by inspecting the file system.
 * Returns null when the file no longer exists. Folder sizes are left at -1L
 * ("calculating" placeholder); they are only needed for display and sorting.
 */
fun fileModelFromPath(path: String): FileModel? {
    val file = File(path)
    if (!file.exists()) return null

    val isDirectory = file.isDirectory
    val extension = file.extension.lowercase()
    return FileModel(
        file = file,
        name = file.name,
        path = file.absolutePath,
        isDirectory = isDirectory,
        // -1L matches the "calculating..." placeholder used by FileRepository
        size = if (isDirectory) -1L else file.length(),
        extension = extension,
        itemCount = if (isDirectory) (file.listFiles()?.size ?: 0) else 0
    )
}
