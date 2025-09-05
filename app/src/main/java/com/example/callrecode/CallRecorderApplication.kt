package com.example.callrecode

import android.app.Application
import com.example.callrecode.data.database.CallRecordDatabase
import com.example.callrecode.data.repository.RecordingRepository
import com.example.callrecode.data.repository.UploadConfigRepository
import com.example.callrecode.data.repository.CallLogRepository

/**
 * Custom Application class for the Call Recorder app
 * This class is responsible for initializing application-wide components
 */
class CallRecorderApplication : Application() {

    // Lazy initialization of database
    private val database by lazy {
        CallRecordDatabase.getDatabase(this)
    }

    // Lazy initialization of repositories
    private val recordingRepository by lazy {
        RecordingRepository(database.recordingDao())
    }
    
    private val uploadConfigRepository by lazy {
        UploadConfigRepository(database.uploadConfigDao())
    }
    
    private val callLogRepository by lazy {
        CallLogRepository(database.callLogDao())
    }

    override fun onCreate() {
        super.onCreate()
        
        // Set the instance for singleton access
        synchronized(this) {
            INSTANCE = this
        }
        
        // Initialize application-wide components
        // Database will be initialized lazily when first accessed
    }

    // Getter methods for dependency injection
    fun getDatabase(): CallRecordDatabase = database
    fun getRecordingRepository(): RecordingRepository = recordingRepository
    fun getUploadConfigRepository(): UploadConfigRepository = uploadConfigRepository
    fun getCallLogRepository(): CallLogRepository = callLogRepository

    companion object {
        // Static reference to the application instance
        @Volatile
        private var INSTANCE: CallRecorderApplication? = null

        fun getInstance(): CallRecorderApplication {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: throw IllegalStateException("Application not initialized")
            }
        }
    }
}