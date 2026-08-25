package com.foss.simpleshare.share

/**
 * MIME resolution for share intents.
 *
 * Purely functional (no Android framework calls) so it is unit-testable on the
 * JVM. Covers the extensions that matter for direct-to-app sharing; anything
 * unknown or mixed falls back to the any-type MIME, which every ACTION_SEND
 * handler accepts.
 */
object ShareMimeResolver {

    private val EXTENSION_TO_MIME = mapOf(
        // Images
        "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
        "gif" to "image/gif", "webp" to "image/webp", "bmp" to "image/bmp",
        "heic" to "image/heic", "heif" to "image/heif", "svg" to "image/svg+xml",
        // Video
        "mp4" to "video/mp4", "mkv" to "video/x-matroska", "webm" to "video/webm",
        "avi" to "video/x-msvideo", "mov" to "video/quicktime", "3gp" to "video/3gpp",
        "ts" to "video/mp2t",
        // Audio
        "mp3" to "audio/mpeg", "wav" to "audio/wav", "ogg" to "audio/ogg",
        "flac" to "audio/flac", "m4a" to "audio/mp4", "opus" to "audio/opus",
        // Documents & data
        "pdf" to "application/pdf", "txt" to "text/plain", "csv" to "text/csv",
        "html" to "text/html", "xml" to "text/xml", "json" to "application/json",
        "zip" to "application/zip", "7z" to "application/x-7z-compressed",
        "rar" to "application/vnd.rar", "gz" to "application/gzip",
        "apk" to "application/vnd.android.package-archive",
        "doc" to "application/msword",
        "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "xls" to "application/vnd.ms-excel",
        "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ppt" to "application/vnd.ms-powerpoint",
        "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    )

    /**
     * Resolve the intent MIME type for a set of file extensions.
     *
     * - single known type   → the specific MIME (e.g. "application/pdf")
     * - homogeneous group   → wildcard group ("image/asterisk", "video/asterisk")
     * - unknown / mixed     → the any-type MIME
     */
    fun resolveMimeTypes(extensions: Set<String>): List<String> {
        val normalized = extensions.map { it.lowercase() }.toSet()
        if (normalized.isEmpty()) return listOf("*/*")

        val resolved = normalized.mapNotNull { EXTENSION_TO_MIME[it] }
        // Any unknown extension: no safe common guess → */*
        if (resolved.size != normalized.size) return listOf("*/*")

        return when {
            // Single known type → the specific MIME (e.g. "application/pdf")
            resolved.size == 1 -> resolved
            else -> {
                // Homogeneous groups collapse to a wildcard, mixed types stay */*
                val groups = resolved.map { it.substringBefore('/') }.toSet()
                if (groups.size == 1) listOf("${groups.first()}/*") else listOf("*/*")
            }
        }
    }
}
