package com.example.callrecode.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Broadcast receiver for phone state changes
 * Handles incoming and outgoing call detection
 */
class PhoneStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "PhoneStateReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Received broadcast: ${intent?.action}")
        
        when (intent?.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                handlePhoneStateChange(context, intent)
            }
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                handleOutgoingCall(context, intent)
            }
        }
    }
    
    private fun handlePhoneStateChange(context: Context?, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        Log.d(TAG, "Phone state changed: $state, number: $phoneNumber")
        
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.d(TAG, "Incoming call detected: $phoneNumber")
                // Handle incoming call
                notifyCallStateChange(context, CallStateChange.INCOMING_CALL, phoneNumber)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                Log.d(TAG, "Call answered or outgoing call started")
                notifyCallStateChange(context, CallStateChange.CALL_STARTED, phoneNumber)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                Log.d(TAG, "Call ended or no active call")
                notifyCallStateChange(context, CallStateChange.CALL_ENDED, phoneNumber)
            }
        }
    }
    
    private fun handleOutgoingCall(context: Context?, intent: Intent) {
        val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        Log.d(TAG, "Outgoing call detected: $phoneNumber")
        notifyCallStateChange(context, CallStateChange.OUTGOING_CALL, phoneNumber)
    }
    
    private fun notifyCallStateChange(context: Context?, change: CallStateChange, phoneNumber: String?) {
        // TODO: Notify recording service or call state manager
        Log.d(TAG, "Call state change: $change for number: $phoneNumber")
    }
}

/**
 * Types of call state changes
 */
enum class CallStateChange {
    INCOMING_CALL,
    OUTGOING_CALL,
    CALL_STARTED,
    CALL_ENDED
}