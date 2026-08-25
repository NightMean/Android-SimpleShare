package com.foss.simpleshare

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foss.simpleshare.data.FileModel
import com.foss.simpleshare.share.FileSharer
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device tests for the share intent built by [FileSharer]. These run against
 * the real FileProvider registered in the merged manifest, which a JVM test
 * cannot do.
 */
@RunWith(AndroidJUnit4::class)
class FileSharerInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun tempFile(name: String): FileModel {
        val baseDir = context.getExternalFilesDir(null) ?: context.cacheDir
        val file = File(baseDir, name)
        file.writeText("test")
        return FileModel(
            file = file,
            name = file.name,
            path = file.absolutePath,
            isDirectory = false,
            size = file.length(),
            extension = file.extension
        )
    }

    @Test
    fun singleImageSendCarriesSpecificMimeAndUri() {
        val intent = FileSharer.buildShareIntent(context, listOf(tempFile("pic.jpg")), null)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/jpeg", intent.type)
        assertNotNull(Intent.EXTRA_STREAM.let { intent.getParcelableExtra<android.net.Uri>(it) })
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun multipleFilesGrantEveryUriViaClipData() {
        val files = listOf(tempFile("a.jpg"), tempFile("b.webp"), tempFile("c.gif"))
        val intent = FileSharer.buildShareIntent(context, files, "com.example.target")

        assertEquals(Intent.ACTION_SEND_MULTIPLE, intent.action)
        assertEquals("image/*", intent.type)
        assertEquals("com.example.target", intent.`package`)

        // The older-Android fix: every URI must appear in ClipData so read
        // permission is granted to all of them, not just the first.
        val clipUris = intent.clipData?.let { clip -> (0 until clip.itemCount).map { clip.getItemAt(it).uri } }
        assertEquals(files.size, clipUris?.size)
    }

    @Test
    fun packageActivityPairResolvesToComponent() {
        val intent = FileSharer.buildShareIntent(
            context,
            listOf(tempFile("doc.pdf")),
            "com.example.pkg/com.example.pkg.ShareActivity"
        )

        assertEquals("com.example.pkg", intent.component?.packageName)
        assertEquals("com.example.pkg.ShareActivity", intent.component?.className)
        assertEquals("application/pdf", intent.type)
    }

    @Test
    fun providerActuallyServesTheSharedFile() {
        // Prove the content:// URI returned by the provider is readable through
        // the same grant the target app would receive.
        val model = tempFile("served.txt")
        val intent = FileSharer.buildShareIntent(context, listOf(model), null)
        val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)!!

        context.contentResolver.openInputStream(uri)!!.use { stream ->
            assertEquals("test", stream.bufferedReader().readText())
        }
    }
}
