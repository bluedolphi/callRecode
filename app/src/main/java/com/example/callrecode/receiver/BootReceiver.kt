package com.example.callrecode.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.callrecode.service.CallRecordingService

/**
 * Boot receiver to automatically start the call recording service after device boot
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.i(TAG, "Device boot completed or app updated, starting recording service")
                
                try {
                    // Check if auto-start is enabled in preferences
                    val prefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
                    val autoStartEnabled = prefs.getBoolean("auto_start_service", true)
                    
                    if (autoStartEnabled) {
                        CallRecordingService.startService(context)
                        Log.i(TAG, "Call recording service started automatically")
                    } else {
                        Log.d(TAG, "Auto-start disabled by user")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting recording service on boot", e)
                }
            }
        }
    }
}