package com.example.callrecode.recording

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Phone state listener for call recording functionality
 * Monitors call state changes and triggers recording actions
 */
class CallRecordingPhoneStateListener(
    private val context: Context,
    private val recordingEngine: RecordingEngine
) : PhoneStateListener() {
    
    companion object {
        private const val TAG = "CallRecordingPhoneStateListener"
    }
    
    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        Log.d(TAG, "Call state changed: $state, phone: $phoneNumber")
        
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(TAG, "Incoming call ringing: $phoneNumber")
                recordingEngine.onIncomingCall(phoneNumber)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.d(TAG, "Call started (off hook): $phoneNumber")
                recordingEngine.onCallStarted(phoneNumber)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d(TAG, "Call ended (idle)")
                recordingEngine.onCallEnded()
            }
        }
    }
}