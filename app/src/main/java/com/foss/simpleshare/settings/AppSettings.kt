package com.foss.simpleshare.settings

import com.foss.simpleshare.data.SortOption

/**
 * Filter mode controlling which files are visible in the browser.
 * Entry names match the legacy persisted strings ("PRESET_ALL", "PRESET_MEDIA", "CUSTOM").
 */
enum class FilterMode {
    PRESET_ALL,
    PRESET_MEDIA,
    CUSTOM
}

/**
 * Single immutable snapshot of every user-configurable app setting.
 *
 * This is the only representation of settings used across screens; persistence
 * goes through [SettingsStore]. Adding a setting means adding a field here and
 * mapping it in [SettingsStore] — nowhere else.
 */
data class AppSettings(
    val defaultPath: String,
    val targetAppPackage: String?,
    val keepSelection: Boolean = true,
    val showThumbnails: Boolean = true,
    val checkLowStorage: Boolean = false,
    val quickOpen: Boolean = false,
    val filterMode: FilterMode = FilterMode.PRESET_ALL,
    val customExtensions: String = "",
    val sortOption: SortOption = SortOption.NAME,
    val isSortAscending: Boolean = true,
    val sortFoldersFirst: Boolean = true
)

/** Extensions shown by each preset filter mode. */
private val MEDIA_PRESET_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "gif", "mp4", "mkv", "webm", "avi", "heic", "webp"
)

/**
 * Parse a user-entered comma-separated extension list into a normalized set.
 * Whitespace is trimmed, casing is lowered, leading dots are removed.
 */
fun parseExtensionList(raw: String): Set<String> =
    raw.split(",")
        .map { it.trim().lowercase().removePrefix(".") }
        .filter { it.isNotEmpty() }
        .toSet()

/**
 * Resolve the set of file extensions the browser should display.
 * An empty set means "all files".
 */
fun resolveAllowedExtensions(settings: AppSettings): Set<String> = when (settings.filterMode) {
    FilterMode.PRESET_ALL -> emptySet()
    FilterMode.PRESET_MEDIA -> MEDIA_PRESET_EXTENSIONS
    FilterMode.CUSTOM -> parseExtensionList(settings.customExtensions)
}
