package com.foss.simpleshare

import com.foss.simpleshare.share.ShareMimeResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareMimeResolverTest {

    @Test
    fun `empty selection resolves to any type`() {
        assertEquals(listOf("*/*"), ShareMimeResolver.resolveMimeTypes(emptySet()))
    }

    @Test
    fun `single known file resolves to its specific mime`() {
        assertEquals(
            listOf("application/pdf"),
            ShareMimeResolver.resolveMimeTypes(setOf("pdf"))
        )
    }

    @Test
    fun `single image resolves to specific image mime`() {
        assertEquals(listOf("image/jpeg"), ShareMimeResolver.resolveMimeTypes(setOf("jpg")))
    }

    @Test
    fun `homogeneous images collapse to image wildcard`() {
        assertEquals(
            listOf("image/*"),
            ShareMimeResolver.resolveMimeTypes(setOf("jpg", "png", "webp"))
        )
    }

    @Test
    fun `homogeneous videos collapse to video wildcard`() {
        assertEquals(
            listOf("video/*"),
            ShareMimeResolver.resolveMimeTypes(setOf("mp4", "mkv"))
        )
    }

    @Test
    fun `mixed media groups resolve to any type`() {
        assertEquals(
            listOf("*/*"),
            ShareMimeResolver.resolveMimeTypes(setOf("jpg", "mp4"))
        )
    }

    @Test
    fun `unknown extension resolves to any type`() {
        assertEquals(listOf("*/*"), ShareMimeResolver.resolveMimeTypes(setOf("xyzabc")))
    }

    @Test
    fun `known mixed with unknown falls back to any type`() {
        assertEquals(listOf("*/*"), ShareMimeResolver.resolveMimeTypes(setOf("pdf", "xyzabc")))
    }

    @Test
    fun `resolution is case-insensitive`() {
        assertEquals(listOf("image/png"), ShareMimeResolver.resolveMimeTypes(setOf("PNG")))
    }
}
