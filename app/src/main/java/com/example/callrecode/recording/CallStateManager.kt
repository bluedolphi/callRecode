package com.example.callrecode.recording

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages call state and coordinates with recording engine
 */
class CallStateManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "CallStateManager"
    }
    
    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private val _currentPhoneNumber = MutableStateFlow<String?>(null)
    val currentPhoneNumber: StateFlow<String?> = _currentPhoneNumber.asStateFlow()
    
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: CallRecordingPhoneStateListener? = null
    
    fun initialize(recordingEngine: RecordingEngine) {
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = CallRecordingPhoneStateListener(context, recordingEngine)
        
        // Register phone state listener
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "Phone state listener registered successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to register phone state listener - missing permission", e)
        }
    }
    
    fun updateCallState(newState: CallState, phoneNumber: String? = null) {
        Log.d(TAG, "Updating call state: $newState, phone: $phoneNumber")
        _callState.value = newState
        _currentPhoneNumber.value = phoneNumber
    }
    
    fun cleanup() {
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            Log.d(TAG, "Phone state listener unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up call state manager", e)
        }
    }
}

/**
 * Represents the current state of a phone call
 */
enum class CallState {
    IDLE,        // No active call
    RINGING,     // Incoming call ringing
    ACTIVE,      // Call is active/connected
    DISCONNECTING // Call is ending
}