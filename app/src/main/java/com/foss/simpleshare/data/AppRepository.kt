package com.foss.simpleshare.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class AppRepository(private val context: Context) {

    private companion object {
        const val TAG = "AppRepository"
    }

    fun getShareableApps(): List<AppModel> {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*" // Use */* to ensure we find ALL apps (like Google Drive, primitive file handlers, etc)
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        val packageManager = context.packageManager
        // MATCH_ALL ensures we see everything, including disabled components that might be toggleable, 
        // but mainly it helps avoid filtering. However, MATCH_DEFAULT_ONLY is standard for resolution.
        // User reports missing apps. Let's try MATCH_ALL flag which is 131072.
        // For Tiramisu+, we use ResolveInfoFlags.
        
        val resolveInfos = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val flags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                packageManager.queryIntentActivities(intent, flags)
            } else {
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
        } catch (e: Exception) {
            // Empty list silently disabled the whole target-app picker with no trace;
            // at least record why the query failed.
            android.util.Log.w(TAG, "queryIntentActivities(ACTION_SEND) failed", e)
            emptyList()
        }

        return resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val packageName = activityInfo.packageName
            
            if (packageName == context.packageName) return@mapNotNull null

            try {
                // Load label safely
                val appName = resolveInfo.loadLabel(packageManager)?.toString() ?: packageName
                // Load icon safely
                val icon = resolveInfo.loadIcon(packageManager) ?: packageManager.getDefaultActivityIcon()
                val activityName = activityInfo.name
                
                AppModel(appName, packageName, activityName, icon)
            } catch (e: Exception) {
                // If loading resources fails, skip or try fallback
                null
            }
        }.distinctBy { "${it.packageName}/${it.activityName}" }.sortedBy { it.name }
    }
}
