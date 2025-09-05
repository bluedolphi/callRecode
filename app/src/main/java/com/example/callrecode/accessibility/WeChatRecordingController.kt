package com.example.callrecode.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.example.callrecode.recording.RecordingEngine
import com.example.callrecode.recording.RecordingQuality
import kotlinx.coroutines.*

/**
 * Controls recording integration with WeChat calls
 * Bridges WeChat call detection with the recording engine
 */
class WeChatRecordingController(
    private val accessibilityService: AccessibilityService
) {
    companion object {
        private const val TAG = "WeChatRecordingController"
        private const val WECHAT_CONTACT_PREFIX = "WeChat_"
        private const val RECORDING_DELAY_MS = 2000L // 2 second delay before starting recording
    }
    
    private var recordingEngine: RecordingEngine? = null
    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var currentRecordingJob: Job? = null
    private var isPreparedForRecording = false
    
    /**
     * Initialize recording controller with recording engine
     */
    fun initialize(context: Context) {
        try {
            recordingEngine = RecordingEngine(context)
            Log.d(TAG, "WeChatRecordingController initialized with RecordingEngine")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RecordingEngine", e)
        }
    }
    
    /**
     * Prepare for recording (called when call is connecting)
     */
    suspend fun prepareRecording(callInfo: WeChatCallInfo) {
        try {
            Log.d(TAG, "Preparing for WeChat recording: ${callInfo.contactName}, type: ${callInfo.callType}")
            
            isPreparedForRecording = true
            
            // Pre-validate recording conditions
            if (!canStartRecording()) {
                Log.w(TAG, "Cannot start recording - conditions not met")
                return
            }
            
            Log.d(TAG, "WeChat recording preparation complete")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing recording", e)
            isPreparedForRecording = false
        }
    }
    
    /**
     * Start recording for WeChat call
     */
    suspend fun startRecording(callInfo: WeChatCallInfo) {
        try {
            Log.i(TAG, "Starting WeChat call recording: ${callInfo.contactName}")
            
            if (!canStartRecording()) {
                Log.w(TAG, "Cannot start WeChat recording - conditions not met")
                return
            }
            
            // Cancel any existing recording job
            currentRecordingJob?.cancel()
            
            // Start recording with delay to ensure call is established
            currentRecordingJob = controllerScope.launch {
                try {
                    // Wait for call to establish
                    delay(RECORDING_DELAY_MS)
                    
                    // Double-check if we should still record
                    if (!isActive || !canStartRecording()) {
                        Log.d(TAG, "Recording cancelled during delay period")
                        return@launch
                    }
                    
                    val engine = recordingEngine
                    if (engine == null) {
                        Log.e(TAG, "RecordingEngine not available")
                        return@launch
                    }
                    
                    // Generate contact name for WeChat call
                    val contactName = generateWeChatContactName(callInfo)
                    
                    // Determine recording quality based on call type
                    val quality = getRecordingQualityForCallType(callInfo.callType)
                    
                    // Start recording
                    val result = engine.startRecording(
                        phoneNumber = contactName,
                        contactName = callInfo.contactName,
                        isManual = false, // WeChat calls are always auto-detected
                        quality = quality
                    )
                    
                    result.onSuccess { session ->
                        Log.i(TAG, "WeChat call recording started successfully: ${session.id}")
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to start WeChat call recording", error)
                    }
                    
                } catch (e: CancellationException) {
                    Log.d(TAG, "WeChat recording start cancelled")
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting WeChat recording", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating WeChat recording start", e)
        }
    }
    
    /**
     * Stop current recording
     */
    suspend fun stopRecording() {
        try {
            Log.i(TAG, "Stopping WeChat call recording")
            
            // Cancel any pending recording start
            currentRecordingJob?.cancel()
            currentRecordingJob = null
            
            val engine = recordingEngine
            if (engine == null) {
                Log.w(TAG, "RecordingEngine not available for stopping recording")
                return
            }
            
            // Check if actually recording
            if (!engine.isRecording.value) {
                Log.d(TAG, "No active recording to stop")
                return
            }
            
            val result = engine.stopRecording()
            result.onSuccess { session ->
                Log.i(TAG, "WeChat call recording stopped: ${session.id}, duration: ${session.duration}ms")
            }.onFailure { error ->
                Log.e(TAG, "Error stopping WeChat recording", error)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping WeChat recording", e)
        } finally {
            isPreparedForRecording = false
        }
    }
    
    /**
     * Check if recording can be started
     */
    private fun canStartRecording(): Boolean {
        val engine = recordingEngine
        if (engine == null) {
            Log.w(TAG, "RecordingEngine not initialized")
            return false
        }
        
        // Check if already recording
        if (engine.isRecording.value) {
            Log.w(TAG, "Already recording - cannot start WeChat recording")
            return false
        }
        
        // Add other recording conditions here (permissions, storage, etc.)
        return true
    }
    
    /**
     * Generate contact name for WeChat recording
     */
    private fun generateWeChatContactName(callInfo: WeChatCallInfo): String {
        val contactName = callInfo.contactName?.takeIf { it.isNotBlank() }
        val callTypePrefix = when (callInfo.callType) {
            WeChatCallType.VIDEO_CALL -> "Video"
            WeChatCallType.VOICE_CALL -> "Voice"
            WeChatCallType.GROUP_CALL -> "Group"
            WeChatCallType.UNKNOWN -> "Call"
        }
        
        return if (contactName != null) {
            "${WECHAT_CONTACT_PREFIX}${contactName}_${callTypePrefix}"
        } else {
            "${WECHAT_CONTACT_PREFIX}Unknown_${callTypePrefix}_${System.currentTimeMillis()}"
        }
    }
    
    /**
     * Get recording quality based on call type
     */
    private fun getRecordingQualityForCallType(callType: WeChatCallType): RecordingQuality {
        return when (callType) {
            WeChatCallType.VIDEO_CALL -> {
                // Video calls might have better audio quality
                RecordingQuality.HIGH
            }
            WeChatCallType.VOICE_CALL,
            WeChatCallType.GROUP_CALL,
            WeChatCallType.UNKNOWN -> {
                // Use standard quality for voice calls to save space
                RecordingQuality.STANDARD
            }
        }
    }
    
    /**
     * Check if currently recording a WeChat call
     */
    fun isRecording(): Boolean {
        return recordingEngine?.isRecording?.value == true
    }
    
    /**
     * Get current recording session if active
     */
    fun getCurrentRecordingSession() = recordingEngine?.currentSession?.value
    
    /**
     * Force stop any ongoing operations (cleanup)
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up WeChat recording controller")
            
            controllerScope.cancel()
            currentRecordingJob?.cancel()
            
            // Force stop recording if active
            if (isRecording()) {
                controllerScope.launch {
                    stopRecording()
                }
            }
            
            recordingEngine?.release()
            isPreparedForRecording = false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}