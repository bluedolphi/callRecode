package com.example.callrecode.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.callrecode.data.database.entity.RecordingEntity
import com.example.callrecode.data.database.entity.UploadConfigEntity
import com.example.callrecode.data.database.entity.CallLogEntity
import com.example.callrecode.data.database.dao.RecordingDao
import com.example.callrecode.data.database.dao.UploadConfigDao
import com.example.callrecode.data.database.dao.CallLogDao
import com.example.callrecode.data.database.converter.Converters

/**
 * Room database class for the Call Record application.
 * This serves as the main database access point and contains references to all entities and DAOs.
 */
@Database(
    entities = [
        RecordingEntity::class,
        UploadConfigEntity::class,
        CallLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CallRecordDatabase : RoomDatabase() {
    
    // Abstract functions to get DAO instances
    abstract fun recordingDao(): RecordingDao
    abstract fun uploadConfigDao(): UploadConfigDao
    abstract fun callLogDao(): CallLogDao
    
    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: CallRecordDatabase? = null
        
        // Database name
        private const val DATABASE_NAME = "call_record_database"
        
        /**
         * Get database instance using singleton pattern
         */
        fun getDatabase(context: Context): CallRecordDatabase {
            // Return existing instance if available
            return INSTANCE ?: synchronized(this) {
                // Create new instance if none exists
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CallRecordDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clear database instance (useful for testing)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}