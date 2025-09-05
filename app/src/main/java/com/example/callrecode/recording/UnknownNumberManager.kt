package com.example.callrecode.recording

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages unknown number detection and recording decisions
 */
class UnknownNumberManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UnknownNumberManager"
        private const val PREFS_NAME = "unknown_number_preferences"
        private const val KEY_REMEMBERED_DECISIONS = "remembered_decisions"
        private const val KEY_ALWAYS_ASK = "always_ask"
        private const val DECISION_ALLOW = "ALLOW"
        private const val DECISION_DENY = "DENY"
    }
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if a number is considered unknown (not in contacts)
     */
    suspend fun isUnknownNumber(phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrBlank()) return true
        
        // Use RecordingModeManager to check contacts
        val recordingModeManager = RecordingModeManager(context)
        return !isKnownContact(phoneNumber, recordingModeManager)
    }
    
    /**
     * Get the recording decision for an unknown number
     * Returns: true to record, false to not record, null to prompt user
     */
    fun getRecordingDecisionForUnknown(phoneNumber: String?): Boolean? {
        if (phoneNumber.isNullOrBlank()) return false
        
        // Check if user has set "always ask"
        if (shouldAlwaysAsk()) {
            Log.d(TAG, "Always ask is enabled for unknown numbers")
            return null // Prompt user
        }
        
        // Check if we have a remembered decision for this number
        val rememberedDecision = getRememberedDecision(phoneNumber)
        if (rememberedDecision != null) {
            Log.d(TAG, "Found remembered decision for $phoneNumber: $rememberedDecision")
            return rememberedDecision == DECISION_ALLOW
        }
        
        // No remembered decision, should prompt user
        return null
    }
    
    /**
     * Remember the user's decision for a specific number
     */
    fun rememberDecision(phoneNumber: String?, allow: Boolean, remember: Boolean = true) {
        if (phoneNumber.isNullOrBlank() || !remember) return
        
        val decision = if (allow) DECISION_ALLOW else DECISION_DENY
        val rememberedDecisions = getRememberedDecisions().toMutableMap()
        rememberedDecisions[phoneNumber] = decision
        
        saveRememberedDecisions(rememberedDecisions)
        Log.d(TAG, "Remembered decision for $phoneNumber: $decision")
    }
    
    /**
     * Clear remembered decision for a number
     */
    fun clearRememberedDecision(phoneNumber: String?) {
        if (phoneNumber.isNullOrBlank()) return
        
        val rememberedDecisions = getRememberedDecisions().toMutableMap()
        rememberedDecisions.remove(phoneNumber)
        saveRememberedDecisions(rememberedDecisions)
        
        Log.d(TAG, "Cleared remembered decision for $phoneNumber")
    }
    
    /**
     * Get all remembered decisions
     */
    fun getRememberedDecisions(): Map<String, String> {
        val decisionsString = preferences.getString(KEY_REMEMBERED_DECISIONS, "") ?: ""
        
        return if (decisionsString.isBlank()) {
            emptyMap()
        } else {
            try {
                decisionsString.split(";").associate { entry ->
                    val parts = entry.split(":")
                    parts[0] to parts[1]
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing remembered decisions", e)
                emptyMap()
            }
        }
    }
    
    /**
     * Save remembered decisions to preferences
     */
    private fun saveRememberedDecisions(decisions: Map<String, String>) {
        val decisionsString = decisions.entries.joinToString(";") { "${it.key}:${it.value}" }
        preferences.edit()
            .putString(KEY_REMEMBERED_DECISIONS, decisionsString)
            .apply()
    }
    
    /**
     * Get remembered decision for a specific number
     */
    private fun getRememberedDecision(phoneNumber: String): String? {
        return getRememberedDecisions()[phoneNumber]
    }
    
    /**
     * Set whether to always ask for unknown numbers (ignore remembered decisions)
     */
    fun setAlwaysAsk(alwaysAsk: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ALWAYS_ASK, alwaysAsk)
            .apply()
        
        Log.d(TAG, "Always ask for unknown numbers: $alwaysAsk")
    }
    
    /**
     * Check if should always ask for unknown numbers
     */
    fun shouldAlwaysAsk(): Boolean {
        return preferences.getBoolean(KEY_ALWAYS_ASK, false)
    }
    
    /**
     * Clear all remembered decisions
     */
    fun clearAllRememberedDecisions() {
        preferences.edit()
            .remove(KEY_REMEMBERED_DECISIONS)
            .apply()
        
        Log.d(TAG, "Cleared all remembered decisions")
    }
    
    /**
     * Get statistics about unknown number decisions
     */
    fun getDecisionStatistics(): UnknownNumberStats {
        val decisions = getRememberedDecisions()
        val allowCount = decisions.values.count { it == DECISION_ALLOW }
        val denyCount = decisions.values.count { it == DECISION_DENY }
        
        return UnknownNumberStats(
            totalRemembered = decisions.size,
            allowedCount = allowCount,
            deniedCount = denyCount,
            alwaysAskEnabled = shouldAlwaysAsk()
        )
    }
    
    /**
     * Analyze number patterns to suggest if it might be spam/business
     */
    fun analyzeNumberPattern(phoneNumber: String?): NumberAnalysis {
        if (phoneNumber.isNullOrBlank()) {
            return NumberAnalysis(NumberType.UNKNOWN, "No number provided")
        }
        
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when {
            digitsOnly.startsWith("400") -> {
                NumberAnalysis(NumberType.BUSINESS, "400 service number")
            }
            digitsOnly.startsWith("800") -> {
                NumberAnalysis(NumberType.BUSINESS, "800 toll-free number")
            }
            digitsOnly.startsWith("95") -> {
                NumberAnalysis(NumberType.BUSINESS, "Business service number")
            }
            digitsOnly.length == 8 && !digitsOnly.startsWith("1") -> {
                NumberAnalysis(NumberType.LANDLINE, "Local landline number")
            }
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> {
                NumberAnalysis(NumberType.MOBILE, "Mobile phone number")
            }
            digitsOnly.length < 8 -> {
                NumberAnalysis(NumberType.SHORT_CODE, "Short code or service number")
            }
            else -> {
                NumberAnalysis(NumberType.UNKNOWN, "Unknown number pattern")
            }
        }
    }
    
    // Helper function to check if number is in contacts
    private suspend fun isKnownContact(phoneNumber: String, recordingModeManager: RecordingModeManager): Boolean {
        // This is a simple implementation - in practice you might want to cache contact lookups
        return try {
            // Use reflection or create a public method in RecordingModeManager
            // For now, assume unknown
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if number is known contact", e)
            false
        }
    }
}

/**
 * Statistics about unknown number recording decisions
 */
data class UnknownNumberStats(
    val totalRemembered: Int,
    val allowedCount: Int,
    val deniedCount: Int,
    val alwaysAskEnabled: Boolean
)

/**
 * Analysis of a phone number pattern
 */
data class NumberAnalysis(
    val type: NumberType,
    val description: String
)

/**
 * Types of phone numbers based on pattern analysis
 */
enum class NumberType(val displayName: String) {
    MOBILE("手机号码"),
    LANDLINE("固定电话"),
    BUSINESS("商务号码"),
    SHORT_CODE("短号/服务号"),
    UNKNOWN("未知类型")
}