package com.foss.simpleshare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for the app. Contains the DirectoryCache table for folder size caching.
 */
@Database(entities = [DirectoryCache::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun directoryCacheDao(): DirectoryCacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Get singleton instance of the database.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simpleshare_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
