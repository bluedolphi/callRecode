package com.example.callrecode.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages WeChat call states and coordinates state transitions
 */
class WeChatCallStateManager(
    private val accessibilityService: AccessibilityService
) {
    companion object {
        private const val TAG = "WeChatCallStateManager"
        private const val STATE_CHANGE_DEBOUNCE_MS = 1000L
    }
    
    private val _currentState = MutableStateFlow(WeChatCallState.IDLE)
    val currentState: StateFlow<WeChatCallState> = _currentState.asStateFlow()
    
    private val _currentCallInfo = MutableStateFlow<WeChatCallInfo?>(null)
    val currentCallInfo: StateFlow<WeChatCallInfo?> = _currentCallInfo.asStateFlow()
    
    private var lastStateChangeTime = 0L
    private var stateChangeListener: ((WeChatCallState, WeChatCallInfo?) -> Unit)? = null
    
    /**
     * Set listener for state changes
     */
    fun setStateChangeListener(listener: (WeChatCallState, WeChatCallInfo?) -> Unit) {
        stateChangeListener = listener
    }
    
    /**
     * Update call state based on detection results
     */
    fun updateCallState(detection: WeChatCallDetection) {
        val currentTime = System.currentTimeMillis()
        
        // Debounce rapid state changes
        if (currentTime - lastStateChangeTime < STATE_CHANGE_DEBOUNCE_MS &&
            detection.callState == _currentState.value) {
            Log.d(TAG, "Ignoring duplicate state change within debounce period")
            return
        }
        
        val newState = detection.callState
        val oldState = _currentState.value
        
        Log.d(TAG, "State transition: $oldState -> $newState (confidence: ${detection.confidence})")
        
        // Only process state changes with sufficient confidence
        if (detection.confidence < 0.6f) {
            Log.w(TAG, "Ignoring state change due to low confidence: ${detection.confidence}")
            return
        }
        
        // Validate state transition
        if (!isValidStateTransition(oldState, newState)) {
            Log.w(TAG, "Invalid state transition from $oldState to $newState")
            return
        }
        
        // Update state
        _currentState.value = newState
        lastStateChangeTime = currentTime
        
        // Update or create call info
        updateCallInfo(newState, detection)
        
        // Notify listener
        stateChangeListener?.invoke(newState, _currentCallInfo.value)
        
        Log.i(TAG, "WeChat call state updated: $newState")
    }
    
    /**
     * Update call information based on current state and detection
     */
    private fun updateCallInfo(newState: WeChatCallState, detection: WeChatCallDetection) {
        when (newState) {
            WeChatCallState.CALL_CONNECTING -> {
                // Start new call info or update existing
                val callInfo = _currentCallInfo.value ?: WeChatCallInfo(
                    contactName = detection.contactName,
                    callType = detection.callType,
                    startTime = System.currentTimeMillis()
                )
                _currentCallInfo.value = callInfo
            }
            
            WeChatCallState.CALL_STARTED -> {
                // Update existing call info or create new if missing
                val callInfo = _currentCallInfo.value?.copy(
                    startTime = _currentCallInfo.value?.startTime ?: System.currentTimeMillis()
                ) ?: WeChatCallInfo(
                    contactName = detection.contactName,
                    callType = detection.callType,
                    startTime = System.currentTimeMillis()
                )
                _currentCallInfo.value = callInfo
            }
            
            WeChatCallState.CALL_ENDED -> {
                // Update end time and duration
                val currentTime = System.currentTimeMillis()
                val callInfo = _currentCallInfo.value
                if (callInfo != null) {
                    val updatedCallInfo = callInfo.copy(
                        endTime = currentTime,
                        duration = currentTime - callInfo.startTime
                    )
                    _currentCallInfo.value = updatedCallInfo
                } else {
                    Log.w(TAG, "Call ended but no call info available")
                }
                
                // Clear call info after a delay (in a real implementation)
                // For now, keep it for logging purposes
            }
            
            WeChatCallState.IDLE -> {
                // Clear call info
                _currentCallInfo.value = null
            }
        }
    }
    
    /**
     * Validate if state transition is logical
     */
    private fun isValidStateTransition(fromState: WeChatCallState, toState: WeChatCallState): Boolean {
        return when (fromState) {
            WeChatCallState.IDLE -> {
                toState == WeChatCallState.CALL_CONNECTING
            }
            WeChatCallState.CALL_CONNECTING -> {
                toState == WeChatCallState.CALL_STARTED || 
                toState == WeChatCallState.CALL_ENDED ||
                toState == WeChatCallState.IDLE
            }
            WeChatCallState.CALL_STARTED -> {
                toState == WeChatCallState.CALL_ENDED ||
                toState == WeChatCallState.IDLE
            }
            WeChatCallState.CALL_ENDED -> {
                toState == WeChatCallState.IDLE ||
                toState == WeChatCallState.CALL_CONNECTING  // New call
            }
        }
    }
    
    /**
     * Force state to idle (used for cleanup)
     */
    fun forceStateToIdle() {
        Log.d(TAG, "Forcing state to IDLE")
        _currentState.value = WeChatCallState.IDLE
        _currentCallInfo.value = null
        
        stateChangeListener?.invoke(WeChatCallState.IDLE, null)
    }
    
    /**
     * Get current call info
     */
    fun getCurrentCallInfo(): WeChatCallInfo? = _currentCallInfo.value
    
    /**
     * Get current call state
     */
    fun getCurrentState(): WeChatCallState = _currentState.value
    
    /**
     * Check if currently in a WeChat call
     */
    fun isInCall(): Boolean {
        return _currentState.value in listOf(
            WeChatCallState.CALL_CONNECTING,
            WeChatCallState.CALL_STARTED
        )
    }
    
    /**
     * Get call duration if call is active
     */
    fun getCurrentCallDuration(): Long {
        val callInfo = _currentCallInfo.value
        return if (callInfo != null && isInCall()) {
            System.currentTimeMillis() - callInfo.startTime
        } else {
            0L
        }
    }
}