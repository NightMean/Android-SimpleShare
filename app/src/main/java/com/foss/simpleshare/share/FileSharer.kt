package com.foss.simpleshare.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.foss.simpleshare.R
import com.foss.simpleshare.data.FileModel

/**
 * Builds and dispatches the share intent that sends selected files straight to
 * the pre-configured target app, bypassing the system share sheet.
 */
object FileSharer {

    /**
     * Share [files] with the configured target app.
     *
     * @param targetAppPackageName bare package ("com.example") or a
     *   "package/activity" pair to deep-link into a specific activity.
     *   When null, falls back to the system chooser.
     */
    fun shareFiles(
        context: Context,
        files: List<FileModel>,
        targetAppPackageName: String?
    ) {
        if (files.isEmpty()) return

        try {
            val intent = buildShareIntent(context, files, targetAppPackageName)

            val shareIntent = if (targetAppPackageName == null) {
                Intent.createChooser(intent, "Share files")
            } else {
                intent
            }

            try {
                context.startActivity(shareIntent)
            } catch (e: android.content.ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.error_app_not_found), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.error_preparing_share, e.message), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    /**
     * Build the share intent without starting anything — visible for testing.
     */
    fun buildShareIntent(
        context: Context,
        files: List<FileModel>,
        targetAppPackageName: String?
    ): Intent {
        val uris = ArrayList<Uri>()
        files.forEach { fileModel ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                fileModel.file
            )
            uris.add(uri)
        }

        // Resolve the MIME from actual extensions; a wrong type (e.g. a PDF
        // announced as an image) makes target apps reject or mis-handle the file.
        val mimeTypes = ShareMimeResolver.resolveMimeTypes(files.map { it.extension }.toSet())

        return Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
            type = mimeTypes.first()

            if (uris.isNotEmpty()) {
                val clipData = ClipData.newRawUri(null, uris[0])
                for (i in 1 until uris.size) {
                    clipData.addItem(ClipData.Item(uris[i]))
                }
                this.clipData = clipData
            }

            if (targetAppPackageName != null) {
                val slashIndex = targetAppPackageName.indexOf('/')
                if (slashIndex != -1 && targetAppPackageName.count { it == '/' } == 1) {
                    component = android.content.ComponentName(
                        targetAppPackageName.substringBefore('/'),
                        targetAppPackageName.substringAfter('/')
                    )
                } else {
                    setPackage(targetAppPackageName)
                }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
