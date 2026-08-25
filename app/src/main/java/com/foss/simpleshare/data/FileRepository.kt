package com.foss.simpleshare.data

import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for file operations.
 * Supports cached folder size calculations via Room database.
 */
class FileRepository(private val directoryCacheDao: DirectoryCacheDao? = null) {

    /**
     * List files in the given path, filtered by allowed extensions.
     * Folder sizes are initially set to -1L (calculating placeholder).
     */
    fun listFiles(path: String, allowedExtensions: Set<String>): List<FileModel> {
        val root = File(path)
        if (!root.exists() || !root.isDirectory) return emptyList()

        return root.listFiles()?.mapNotNull { file ->
            val isDirectory = file.isDirectory
            val extension = file.extension.lowercase(Locale.getDefault())
            
            // Allow directories OR if allowedExtensions is EMPTY (All Files) OR if extension is allowed
            if (isDirectory || allowedExtensions.isEmpty() || allowedExtensions.contains(extension)) {
                FileModel(
                    file = file,
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = isDirectory,
                    // -1L = placeholder ("calculating..."), actual size loaded async
                    size = if (isDirectory) -1L else file.length(),
                    extension = extension,
                    itemCount = if (isDirectory) (file.listFiles()?.size ?: 0) else 0
                )
            } else {
                null
            }
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.getDefault()) })) 
        ?: emptyList()
    }

    /**
     * List files with cached sizes applied for folders.
     * Folders with valid cache get their cached size immediately.
     * Folders without cache get -1L (placeholder for async calculation).
     */
    suspend fun listFilesWithCachedSizes(path: String, allowedExtensions: Set<String>): List<FileModel> {
        val files = withContext(Dispatchers.IO) { listFiles(path, allowedExtensions) }

        // Apply cached sizes for folders
        return files.map { file ->
            if (file.isDirectory && file.size == -1L) {
                val cachedSize = getCachedSize(file.path)
                if (cachedSize != null) file.copy(size = cachedSize) else file
            } else {
                file
            }
        }
    }

    fun getDefaultPath(): String {
        return "/storage/emulated/0/"
    }

    /**
     * Get cached size for a folder. Returns null if not cached.
     */
    suspend fun getCachedSize(folderPath: String): Long? {
        val dao = directoryCacheDao ?: return null
        val folder = File(folderPath)
        if (!folder.exists()) return null
        
        val cached = dao.getByPath(folderPath)
        // Validate cache by checking lastModified
        return if (cached != null && cached.lastModified == folder.lastModified()) {
            cached.size
        } else {
            null
        }
    }

    /**
     * Calculate folder size recursively and cache the result.
     */
    suspend fun calculateAndCacheSize(folderPath: String): Long = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return@withContext 0L
        
        val size = getFolderSizeRecursive(folder)
        
        // Cache the result
        directoryCacheDao?.insert(
            DirectoryCache(
                path = folderPath,
                size = size,
                lastModified = folder.lastModified()
            )
        )
        
        size
    }

    /**
     * Invalidate cache for a specific path (when folder contents change).
     */
    suspend fun invalidateCache(folderPath: String) {
        directoryCacheDao?.deleteByPath(folderPath)
    }

    /**
     * Recursively calculate folder size.
     */
    private fun getFolderSizeRecursive(directory: File): Long {
        var length: Long = 0
        directory.listFiles()?.forEach { file ->
            length += if (file.isFile) file.length() else getFolderSizeRecursive(file)
        }
        return length
    }
    /**
     * Delete a list of files. Returns the number of successfully deleted files.
     */
    suspend fun deleteFiles(files: List<FileModel>): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        files.forEach { fileModel ->
            val file = fileModel.file
            if (file.exists()) {
                val success = if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
                
                if (success) {
                    deletedCount++
                    // Invalidate cache if it was a directory
                    if (fileModel.isDirectory) {
                        directoryCacheDao?.deleteByPath(fileModel.path)
                    }
                }
            }
        }
        deletedCount
    }
}

