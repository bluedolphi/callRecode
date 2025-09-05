package com.example.callrecode.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AccessibilityService for monitoring WeChat voice and video calls
 * Detects WeChat call interfaces and triggers recording
 */
class WeChatAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "WeChatAccessibilityService"
        private const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
        
        // Static instance to check service status
        @Volatile
        private var instance: WeChatAccessibilityService? = null
        
        fun isServiceRunning(): Boolean = instance != null
        
        fun getInstance(): WeChatAccessibilityService? = instance
    }
    
    private lateinit var weChatCallDetector: WeChatCallDetector
    private lateinit var weChatCallStateManager: WeChatCallStateManager
    private lateinit var weChatRecordingController: WeChatRecordingController
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()
    
    private val _weChatCallState = MutableStateFlow(WeChatCallState.IDLE)
    val weChatCallState: StateFlow<WeChatCallState> = _weChatCallState.asStateFlow()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        Log.i(TAG, "WeChat AccessibilityService connected")
        instance = this
        _isServiceConnected.value = true
        
        // Initialize components
        initializeComponents()
        
        Log.i(TAG, "WeChat monitoring service is now active")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName != WECHAT_PACKAGE_NAME) {
            return
        }
        
        try {
            Log.d(TAG, "Accessibility event: ${event.eventType}, class: ${event.className}")
            
            // Process event in background to avoid blocking
            serviceScope.launch {
                processAccessibilityEvent(event)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        Log.i(TAG, "WeChat AccessibilityService destroyed")
        
        // Cleanup
        serviceScope.cancel()
        instance = null
        _isServiceConnected.value = false
        
        // Stop any ongoing recording
        try {
            weChatRecordingController.stopRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording during service destruction", e)
        }
    }
    
    /**
     * Initialize all monitoring components
     */
    private fun initializeComponents() {
        try {
            weChatCallDetector = WeChatCallDetector(this)
            weChatCallStateManager = WeChatCallStateManager(this)
            weChatRecordingController = WeChatRecordingController(this)
            
            // Setup state change listener
            weChatCallStateManager.setStateChangeListener { newState, callInfo ->
                _weChatCallState.value = newState
                handleCallStateChange(newState, callInfo)
            }
            
            Log.d(TAG, "All components initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
        }
    }
    
    /**
     * Process accessibility events from WeChat
     */
    private suspend fun processAccessibilityEvent(event: AccessibilityEvent) = withContext(Dispatchers.IO) {
        try {
            // Get root node for analysis
            val rootNode = rootInActiveWindow
            if (rootNode == null) {
                Log.d(TAG, "No root node available")
                return@withContext
            }
            
            // Detect WeChat call interface
            val callDetection = weChatCallDetector.detectCallInterface(rootNode, event)
            
            if (callDetection != null) {
                Log.i(TAG, "WeChat call detected: ${callDetection.callType}, state: ${callDetection.callState}")
                
                // Update call state
                weChatCallStateManager.updateCallState(callDetection)
            }
            
            // Always recycle the node to prevent memory leaks
            rootNode.recycle()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing WeChat accessibility event", e)
        }
    }
    
    /**
     * Handle WeChat call state changes
     */
    private fun handleCallStateChange(newState: WeChatCallState, callInfo: WeChatCallInfo?) {
        Log.d(TAG, "WeChat call state changed: $newState")
        
        serviceScope.launch {
            try {
                when (newState) {
                    WeChatCallState.CALL_STARTED -> {
                        Log.i(TAG, "WeChat call started, initiating recording")
                        callInfo?.let { 
                            weChatRecordingController.startRecording(it)
                        }
                    }
                    WeChatCallState.CALL_ENDED -> {
                        Log.i(TAG, "WeChat call ended, stopping recording")
                        weChatRecordingController.stopRecording()
                    }
                    WeChatCallState.CALL_CONNECTING -> {
                        Log.d(TAG, "WeChat call connecting, preparing for recording")
                        // Prepare but don't start recording yet
                        callInfo?.let { 
                            weChatRecordingController.prepareRecording(it)
                        }
                    }
                    else -> {
                        Log.d(TAG, "WeChat call state: $newState - no action needed")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling call state change", e)
            }
        }
    }
    
    /**
     * Get current WeChat call information
     */
    fun getCurrentCallInfo(): WeChatCallInfo? {
        return weChatCallStateManager.getCurrentCallInfo()
    }
    
    /**
     * Force refresh WeChat interface detection
     */
    fun refreshWeChatInterface() {
        serviceScope.launch {
            try {
                val rootNode = rootInActiveWindow
                rootNode?.let { node ->
                    val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
                    processAccessibilityEvent(event)
                    node.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing WeChat interface", e)
            }
        }
    }
}

/**
 * WeChat call states
 */
enum class WeChatCallState(val displayName: String) {
    IDLE("空闲"),
    CALL_CONNECTING("连接中"),
    CALL_STARTED("通话中"),
    CALL_ENDED("通话结束")
}

/**
 * WeChat call information
 */
data class WeChatCallInfo(
    val contactName: String?,
    val callType: WeChatCallType,
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long? = null,
    var duration: Long = 0
)

/**
 * WeChat call types
 */
enum class WeChatCallType(val displayName: String) {
    VOICE_CALL("语音通话"),
    VIDEO_CALL("视频通话"),
    GROUP_CALL("群通话"),
    UNKNOWN("未知类型")
}