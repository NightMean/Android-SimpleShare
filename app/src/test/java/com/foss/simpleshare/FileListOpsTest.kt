package com.foss.simpleshare

import com.foss.simpleshare.data.FileModel
import com.foss.simpleshare.data.SortOption
import com.foss.simpleshare.data.filterBySearch
import com.foss.simpleshare.data.sortFiles
import com.foss.simpleshare.settings.AppSettings
import com.foss.simpleshare.settings.FilterMode
import com.foss.simpleshare.settings.parseExtensionList
import com.foss.simpleshare.settings.resolveAllowedExtensions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileListOpsTest {

    private fun fileModel(
        name: String,
        isDirectory: Boolean = false,
        size: Long = 100L,
        lastModified: Long = 1_000L,
        extension: String = name.substringAfterLast('.', "")
    ): FileModel = FileModel(
        file = File("/tmp/$name").apply { setLastModified(lastModified) },
        name = name,
        path = "/tmp/$name",
        isDirectory = isDirectory,
        size = size,
        extension = extension
    )

    // --- sortFiles ---

    @Test
    fun `sort by name ascending is case-insensitive`() {
        val files = listOf(fileModel("banana"), fileModel("Apple"), fileModel("cherry"))
        val result = sortFiles(files, SortOption.NAME, ascending = true, foldersFirst = false)
        assertEquals(listOf("Apple", "banana", "cherry"), result.map { it.name })
    }

    @Test
    fun `sort by name descending reverses order`() {
        val files = listOf(fileModel("a"), fileModel("c"), fileModel("b"))
        val result = sortFiles(files, SortOption.NAME, ascending = false, foldersFirst = false)
        assertEquals(listOf("c", "b", "a"), result.map { it.name })
    }

    @Test
    fun `sort by size orders numerically not lexicographically`() {
        val files = listOf(fileModel("small", size = 5), fileModel("big", size = 10_000), fileModel("mid", size = 500))
        val result = sortFiles(files, SortOption.SIZE, ascending = true, foldersFirst = false)
        assertEquals(listOf("small", "mid", "big"), result.map { it.name })
    }

    @Test
    fun `sort by date uses file lastModified`() {
        val files = listOf(fileModel("old", lastModified = 1L), fileModel("new", lastModified = 2L))
        val result = sortFiles(files, SortOption.DATE, ascending = true, foldersFirst = false)
        assertEquals(listOf("old", "new"), result.map { it.name })
    }

    @Test
    fun `sort by type groups by extension`() {
        val files = listOf(fileModel("b.txt"), fileModel("a.pdf"), fileModel("c.txt"))
        val result = sortFiles(files, SortOption.TYPE, ascending = true, foldersFirst = false)
        assertEquals(listOf("pdf", "txt", "txt"), result.map { it.extension })
    }

    @Test
    fun `folders come first when enabled regardless of other criteria`() {
        val files = listOf(fileModel("z.txt"), fileModel("a_folder", isDirectory = true))
        val result = sortFiles(files, SortOption.NAME, ascending = true, foldersFirst = true)
        assertEquals(listOf("a_folder", "z.txt"), result.map { it.name })
    }

    @Test
    fun `folders do not come first when disabled`() {
        val files = listOf(fileModel("z_folder", isDirectory = true), fileModel("a.txt"))
        val result = sortFiles(files, SortOption.NAME, ascending = true, foldersFirst = false)
        assertEquals(listOf("a.txt", "z_folder"), result.map { it.name })
    }

    // --- filterBySearch ---

    @Test
    fun `blank query returns all files`() {
        val files = listOf(fileModel("a"), fileModel("b"))
        assertEquals(files, filterBySearch(files, "   "))
    }

    @Test
    fun `query filters case-insensitively by name`() {
        val files = listOf(fileModel("Holiday.JPG"), fileModel("report.pdf"))
        val result = filterBySearch(files, "HOLIDAY")
        assertEquals(listOf("Holiday.JPG"), result.map { it.name })
    }

    // --- parseExtensionList / resolveAllowedExtensions ---

    @Test
    fun `parseExtensionList trims, lowercases and strips leading dots`() {
        assertEquals(setOf("pdf", "zip", "7z"), parseExtensionList(" PDF, .Zip , 7z,, "))
    }

    @Test
    fun `empty custom list parses to empty set`() {
        assertTrue(parseExtensionList("").isEmpty())
    }

    @Test
    fun `PRESET_ALL resolves to empty set meaning all files`() {
        val settings = AppSettings(defaultPath = "/x", targetAppPackage = null)
        assertTrue(resolveAllowedExtensions(settings).isEmpty())
    }

    @Test
    fun `PRESET_MEDIA resolves to media extensions`() {
        val settings = AppSettings(defaultPath = "/x", targetAppPackage = null, filterMode = FilterMode.PRESET_MEDIA)
        val exts = resolveAllowedExtensions(settings)
        assertTrue("jpg" in exts && "mp4" in exts)
        assertTrue("exe" !in exts)
    }

    @Test
    fun `CUSTOM resolves to parsed user extensions`() {
        val settings = AppSettings(
            defaultPath = "/x",
            targetAppPackage = null,
            filterMode = FilterMode.CUSTOM,
            customExtensions = ".png, jpg"
        )
        assertEquals(setOf("png", "jpg"), resolveAllowedExtensions(settings))
    }
}
