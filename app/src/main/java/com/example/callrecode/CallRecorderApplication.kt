package com.example.callrecode

import android.app.Application

/**
 * Custom Application class for the Call Recorder app
 * This class is responsible for initializing application-wide components
 */
class CallRecorderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize application-wide components here
        // For example: database, dependency injection, etc.
        
        // TODO: Initialize Room Database
        // TODO: Initialize Repository instances
        // TODO: Set up dependency injection if using DI framework
    }

    companion object {
        // Static reference to the application instance
        @Volatile
        private var INSTANCE: CallRecorderApplication? = null

        fun getInstance(): CallRecorderApplication {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallRecorderApplication().also { INSTANCE = it }
            }
        }
    }
}