package com.foss.simpleshare.feature.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foss.simpleshare.data.FileModel
import com.foss.simpleshare.data.FileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns the browser's file-listing state: the current directory contents,
 * loading flag, asynchronous folder detail resolution (sizes + child counts)
 * and deletion. Screen composables render this state and forward events;
 * they no longer perform I/O themselves.
 */
class BrowserViewModel(private val repository: FileRepository) : ViewModel() {

    var rawFiles by mutableStateOf<List<FileModel>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set

    private var loadJob: Job? = null

    /**
     * Load the directory listing for [path], applying cached folder sizes,
     * then resolve missing folder details asynchronously.
     * Also used for manual refresh (same semantics).
     */
    fun load(path: String, allowedExtensions: Set<String>) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            // Directory listing does disk I/O; keep it off the main thread
            rawFiles = repository.listFilesWithCachedSizes(path, allowedExtensions)
            isLoading = false
            resolveFolderDetails()
        }
    }

    /**
     * Delete [files] and report the number successfully removed via [onDeleted]
     * (invoked on the main thread).
     */
    fun delete(files: List<FileModel>, onDeleted: (Int) -> Unit) {
        viewModelScope.launch {
            // deleteFiles() hops to Dispatchers.IO internally; the continuation
            // resumes on the main dispatcher.
            onDeleted(repository.deleteFiles(files))
        }
    }

    /**
     * Resolve placeholder sizes (-1L) and child counts (-1) for directories,
     * updating the list in place as results arrive.
     */
    private fun resolveFolderDetails() {
        val pendingFolders = rawFiles.filter { it.isDirectory && (it.size == -1L || it.itemCount == -1) }
        if (pendingFolders.isEmpty()) return

        pendingFolders.forEach { folder ->
            viewModelScope.launch {
                val size = if (folder.size == -1L) {
                    repository.getCachedSize(folder.path)
                        ?: repository.calculateAndCacheSize(folder.path)
                } else folder.size

                val itemCount = if (folder.itemCount == -1) {
                    // file.list() reads names only — cheaper than listFiles()
                    java.io.File(folder.path).list()?.size ?: 0
                } else folder.itemCount

                rawFiles = rawFiles.map { file ->
                    if (file.path == folder.path) file.copy(size = size, itemCount = itemCount) else file
                }
            }
        }
    }

    class Factory(private val repository: FileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrowserViewModel(repository) as T
    }
}
