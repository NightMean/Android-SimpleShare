package com.foss.simpleshare.settings

import android.content.SharedPreferences
import com.foss.simpleshare.data.SortOption

/**
 * Single access point for persisting [AppSettings] to SharedPreferences.
 * Key names and value formats are unchanged from the legacy implementation so
 * existing installations load their settings transparently.
 */
class SettingsStore(
    private val prefs: SharedPreferences,
    private val fallbackDefaultPath: String
) {

    fun load(): AppSettings = AppSettings(
        defaultPath = prefs.getString(KEY_DEFAULT_PATH, null) ?: fallbackDefaultPath,
        targetAppPackage = prefs.getString(KEY_TARGET_APP, null),
        keepSelection = prefs.getBoolean(KEY_KEEP_SELECTION, true),
        showThumbnails = prefs.getBoolean(KEY_SHOW_THUMBNAILS, true),
        checkLowStorage = prefs.getBoolean(KEY_CHECK_LOW_STORAGE, false),
        quickOpen = prefs.getBoolean(KEY_QUICK_OPEN, false),
        filterMode = prefs.getString(KEY_FILTER_MODE, null)
            ?.let { runCatching { FilterMode.valueOf(it) }.getOrNull() }
            ?: FilterMode.PRESET_ALL,
        customExtensions = prefs.getString(KEY_CUSTOM_EXTENSIONS, "") ?: "",
        sortOption = prefs.getString(KEY_SORT_OPTION, null)
            ?.let { runCatching { SortOption.valueOf(it) }.getOrNull() }
            ?: SortOption.NAME,
        isSortAscending = prefs.getBoolean(KEY_SORT_ASCENDING, true),
        sortFoldersFirst = prefs.getBoolean(KEY_SORT_FOLDERS_FIRST, true)
    )

    fun save(settings: AppSettings) {
        prefs.edit().apply {
            putString(KEY_DEFAULT_PATH, settings.defaultPath)
            if (settings.targetAppPackage != null) {
                putString(KEY_TARGET_APP, settings.targetAppPackage)
            } else {
                remove(KEY_TARGET_APP)
            }
            putBoolean(KEY_KEEP_SELECTION, settings.keepSelection)
            putBoolean(KEY_SHOW_THUMBNAILS, settings.showThumbnails)
            putBoolean(KEY_CHECK_LOW_STORAGE, settings.checkLowStorage)
            putBoolean(KEY_QUICK_OPEN, settings.quickOpen)
            putString(KEY_FILTER_MODE, settings.filterMode.name)
            putString(KEY_CUSTOM_EXTENSIONS, settings.customExtensions)
            putString(KEY_SORT_OPTION, settings.sortOption.name)
            putBoolean(KEY_SORT_ASCENDING, settings.isSortAscending)
            putBoolean(KEY_SORT_FOLDERS_FIRST, settings.sortFoldersFirst)
        }.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "simpleshare_prefs"

        private const val KEY_DEFAULT_PATH = "default_path"
        private const val KEY_TARGET_APP = "target_app_package"
        private const val KEY_KEEP_SELECTION = "keep_selection"
        private const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
        private const val KEY_CHECK_LOW_STORAGE = "check_low_storage"
        private const val KEY_QUICK_OPEN = "quick_open"
        private const val KEY_FILTER_MODE = "filter_mode"
        private const val KEY_CUSTOM_EXTENSIONS = "custom_extensions"
        private const val KEY_SORT_OPTION = "sort_option"
        private const val KEY_SORT_ASCENDING = "sort_ascending"
        private const val KEY_SORT_FOLDERS_FIRST = "sort_folders_first"
    }
}
