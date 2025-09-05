package com.example.callrecode.recording

import android.content.Context
import android.content.SharedPreferences
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages recording mode logic and contact checking
 */
class RecordingModeManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RecordingModeManager"
        private const val PREFS_NAME = "recording_preferences"
        private const val KEY_RECORDING_MODE = "recording_mode"
        private const val KEY_AUTO_RECORD_UNKNOWN = "auto_record_unknown"
        private const val KEY_WHITELIST_ENABLED = "whitelist_enabled"
        private const val KEY_BLACKLIST_ENABLED = "blacklist_enabled"
    }
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Determine if a call should be automatically recorded
     */
    suspend fun shouldAutoRecord(phoneNumber: String?): Boolean = withContext(Dispatchers.IO) {
        val currentMode = getCurrentMode()
        
        Log.d(TAG, "Checking auto record for mode: $currentMode, number: $phoneNumber")
        
        return@withContext when (currentMode) {
            RecordingMode.AUTO -> {
                // Auto record all calls, but check blacklist
                !isInBlacklist(phoneNumber)
            }
            RecordingMode.MANUAL -> {
                // Never auto record in manual mode
                false
            }
            RecordingMode.ASK_UNKNOWN -> {
                // Only auto record known contacts, ask for unknown
                phoneNumber != null && isKnownContact(phoneNumber) && !isInBlacklist(phoneNumber)
            }
        }
    }
    
    /**
     * Check if user should be prompted for recording unknown number
     */
    suspend fun shouldPromptForUnknown(phoneNumber: String?): Boolean = withContext(Dispatchers.IO) {
        val currentMode = getCurrentMode()
        
        return@withContext currentMode == RecordingMode.ASK_UNKNOWN && 
                phoneNumber != null && 
                !isKnownContact(phoneNumber) && 
                !isInBlacklist(phoneNumber)
    }
    
    /**
     * Check if a phone number is a known contact
     */
    private suspend fun isKnownContact(phoneNumber: String?): Boolean = withContext(Dispatchers.IO) {
        if (phoneNumber.isNullOrBlank()) return@withContext false
        
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            
            context.contentResolver.query(
                uri.buildUpon().appendPath(phoneNumber).build(),
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val hasContact = cursor.moveToFirst()
                Log.d(TAG, "Contact lookup for $phoneNumber: $hasContact")
                return@withContext hasContact
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to read contacts", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact for $phoneNumber", e)
        }
        
        return@withContext false
    }
    
    /**
     * Check if number is in blacklist
     */
    private fun isInBlacklist(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank() || !isBlacklistEnabled()) return false
        
        // TODO: Implement blacklist storage and lookup
        // For now, return false
        return false
    }
    
    /**
     * Check if number is in whitelist
     */
    private fun isInWhitelist(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank() || !isWhitelistEnabled()) return true
        
        // TODO: Implement whitelist storage and lookup
        // For now, return true (allow all)
        return true
    }
    
    /**
     * Get current recording mode
     */
    fun getCurrentMode(): RecordingMode {
        val modeOrdinal = preferences.getInt(KEY_RECORDING_MODE, RecordingMode.AUTO.ordinal)
        return try {
            RecordingMode.values()[modeOrdinal]
        } catch (e: Exception) {
            Log.w(TAG, "Invalid recording mode ordinal: $modeOrdinal, defaulting to AUTO")
            RecordingMode.AUTO
        }
    }
    
    /**
     * Set recording mode
     */
    fun setRecordingMode(mode: RecordingMode) {
        preferences.edit()
            .putInt(KEY_RECORDING_MODE, mode.ordinal)
            .apply()
        
        Log.d(TAG, "Recording mode set to: ${mode.displayName}")
    }
    
    /**
     * Enable/disable automatic recording for unknown numbers
     */
    fun setAutoRecordUnknown(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_AUTO_RECORD_UNKNOWN, enabled)
            .apply()
        
        Log.d(TAG, "Auto record unknown numbers: $enabled")
    }
    
    /**
     * Check if auto record unknown is enabled
     */
    fun isAutoRecordUnknownEnabled(): Boolean {
        return preferences.getBoolean(KEY_AUTO_RECORD_UNKNOWN, false)
    }
    
    /**
     * Enable/disable whitelist
     */
    fun setWhitelistEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_WHITELIST_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Check if whitelist is enabled
     */
    fun isWhitelistEnabled(): Boolean {
        return preferences.getBoolean(KEY_WHITELIST_ENABLED, false)
    }
    
    /**
     * Enable/disable blacklist
     */
    fun setBlacklistEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_BLACKLIST_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Check if blacklist is enabled
     */
    fun isBlacklistEnabled(): Boolean {
        return preferences.getBoolean(KEY_BLACKLIST_ENABLED, false)
    }
    
    /**
     * Add number to whitelist
     */
    fun addToWhitelist(phoneNumber: String) {
        // TODO: Implement whitelist storage
        Log.d(TAG, "Added to whitelist: $phoneNumber")
    }
    
    /**
     * Add number to blacklist
     */
    fun addToBlacklist(phoneNumber: String) {
        // TODO: Implement blacklist storage
        Log.d(TAG, "Added to blacklist: $phoneNumber")
    }
    
    /**
     * Remove number from whitelist
     */
    fun removeFromWhitelist(phoneNumber: String) {
        // TODO: Implement whitelist storage
        Log.d(TAG, "Removed from whitelist: $phoneNumber")
    }
    
    /**
     * Remove number from blacklist
     */
    fun removeFromBlacklist(phoneNumber: String) {
        // TODO: Implement blacklist storage
        Log.d(TAG, "Removed from blacklist: $phoneNumber")
    }
}