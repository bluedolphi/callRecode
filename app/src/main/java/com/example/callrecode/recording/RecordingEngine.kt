package com.example.callrecode.recording

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * Core recording engine that manages MediaRecorder and recording sessions
 */
class RecordingEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "RecordingEngine"
        private const val MAX_RECORDING_DURATION_HOURS = 4
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private val audioQualityManager = AudioQualityManager()
    private val recordingFileManager = RecordingFileManager(context)
    
    private val _currentSession = MutableStateFlow<RecordingSession?>(null)
    val currentSession: StateFlow<RecordingSession?> = _currentSession.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private var recordingJob: Job? = null
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Default settings - can be configured via settings
    private var defaultQuality = RecordingQuality.STANDARD
    private var defaultMode = RecordingMode.AUTO
    
    /**
     * Start recording with specified parameters
     */
    suspend fun startRecording(
        phoneNumber: String?,
        contactName: String? = null,
        isManual: Boolean = false,
        quality: RecordingQuality = defaultQuality
    ): Result<RecordingSession> = withContext(Dispatchers.IO) {
        try {
            if (_isRecording.value) {
                Log.w(TAG, "Recording already in progress")
                return@withContext Result.failure(IllegalStateException("Recording already in progress"))
            }
            
            Log.d(TAG, "Starting recording for phone: $phoneNumber, manual: $isManual")
            
            // Generate file name and path
            val fileName = recordingFileManager.generateFileName(phoneNumber, System.currentTimeMillis())
            val recordingFile = recordingFileManager.getRecordingFile(fileName)
            
            // Ensure parent directory exists
            recordingFile.parentFile?.mkdirs()
            
            // Create recording session
            val session = RecordingSession(
                phoneNumber = phoneNumber,
                contactName = contactName,
                startTime = System.currentTimeMillis(),
                filePath = recordingFile.absolutePath,
                quality = quality,
                mode = if (isManual) RecordingMode.MANUAL else defaultMode,
                status = RecordingStatus.RECORDING
            )
            
            // Initialize and configure MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                audioQualityManager.configureRecorder(this, quality, recordingFile.absolutePath)
                
                // Set maximum duration (4 hours)
                setMaxDuration(MAX_RECORDING_DURATION_HOURS * 60 * 60 * 1000)
                
                // Set event listeners
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error: what=$what, extra=$extra")
                    handleRecordingError("MediaRecorder error: $what")
                }
                
                setOnInfoListener { _, what, extra ->
                    Log.i(TAG, "MediaRecorder info: what=$what, extra=$extra")
                    when (what) {
                        MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED -> {
                            Log.w(TAG, "Maximum recording duration reached")
                            recordingScope.launch { stopRecording() }
                        }
                    }
                }
            }
            
            // Prepare and start recording
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            
            // Update state
            _currentSession.value = session
            _isRecording.value = true
            
            Log.i(TAG, "Recording started successfully: ${session.id}")
            
            // Start monitoring job
            startRecordingMonitor(session)
            
            Result.success(session)
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording - IO error", e)
            cleanup()
            Result.failure(RecordingException("Failed to start recording: IO error", e))
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to start recording - permission denied", e)
            cleanup()
            Result.failure(RecordingException("Recording permission denied", e))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            Result.failure(RecordingException("Failed to start recording", e))
        }
    }
    
    /**
     * Stop current recording
     */
    suspend fun stopRecording(): Result<RecordingSession> = withContext(Dispatchers.IO) {
        try {
            val session = _currentSession.value
            if (session == null || !_isRecording.value) {
                Log.w(TAG, "No active recording to stop")
                return@withContext Result.failure(IllegalStateException("No active recording"))
            }
            
            Log.d(TAG, "Stopping recording: ${session.id}")
            
            // Stop MediaRecorder
            mediaRecorder?.apply {
                try {
                    stop()
                    Log.d(TAG, "MediaRecorder stopped")
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Error stopping MediaRecorder", e)
                }
            }
            
            // Update session
            val endTime = System.currentTimeMillis()
            val updatedSession = session.copy(
                status = RecordingStatus.STOPPED,
                endTime = endTime,
                duration = endTime - session.startTime,
                fileSize = getRecordingFileSize(session.filePath)
            )
            
            // Cancel monitoring
            recordingJob?.cancel()
            
            // Cleanup
            cleanup()
            
            _currentSession.value = updatedSession
            _isRecording.value = false
            
            Log.i(TAG, "Recording stopped successfully: ${session.id}, duration: ${updatedSession.duration}ms")
            
            Result.success(updatedSession)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            handleRecordingError("Failed to stop recording: ${e.message}")
            Result.failure(RecordingException("Failed to stop recording", e))
        }
    }
    
    /**
     * Pause current recording (if supported by the format)
     */
    suspend fun pauseRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = _currentSession.value
            if (session == null || !_isRecording.value || session.status != RecordingStatus.RECORDING) {
                return@withContext Result.failure(IllegalStateException("No active recording to pause"))
            }
            
            mediaRecorder?.pause()
            
            val pausedSession = session.copy(status = RecordingStatus.PAUSED)
            _currentSession.value = pausedSession
            
            Log.d(TAG, "Recording paused: ${session.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recording", e)
            Result.failure(RecordingException("Failed to pause recording", e))
        }
    }
    
    /**
     * Resume paused recording
     */
    suspend fun resumeRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = _currentSession.value
            if (session == null || session.status != RecordingStatus.PAUSED) {
                return@withContext Result.failure(IllegalStateException("No paused recording to resume"))
            }
            
            mediaRecorder?.resume()
            
            val resumedSession = session.copy(status = RecordingStatus.RECORDING)
            _currentSession.value = resumedSession
            
            Log.d(TAG, "Recording resumed: ${session.id}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recording", e)
            Result.failure(RecordingException("Failed to resume recording", e))
        }
    }
    
    // Phone state event handlers
    fun onIncomingCall(phoneNumber: String?) {
        Log.d(TAG, "Incoming call detected: $phoneNumber")
        // TODO: Check recording mode and decide whether to start recording
    }
    
    fun onCallStarted(phoneNumber: String?) {
        Log.d(TAG, "Call started: $phoneNumber")
        if (defaultMode == RecordingMode.AUTO) {
            recordingScope.launch {
                startRecording(phoneNumber)
            }
        }
    }
    
    fun onCallEnded() {
        Log.d(TAG, "Call ended")
        if (_isRecording.value) {
            recordingScope.launch {
                stopRecording()
            }
        }
    }
    
    // Configuration methods
    fun setRecordingQuality(quality: RecordingQuality) {
        defaultQuality = quality
        Log.d(TAG, "Default recording quality set to: ${quality.displayName}")
    }
    
    fun getRecordingQuality(): RecordingQuality = defaultQuality
    
    fun setRecordingMode(mode: RecordingMode) {
        defaultMode = mode
        Log.d(TAG, "Default recording mode set to: ${mode.displayName}")
    }
    
    fun getRecordingMode(): RecordingMode = defaultMode
    
    // Private helper methods
    private fun startRecordingMonitor(session: RecordingSession) {
        recordingJob = recordingScope.launch {
            while (_isRecording.value && isActive) {
                delay(1000) // Update every second
                
                // Update session with current info
                val currentTime = System.currentTimeMillis()
                val updatedSession = session.copy(
                    duration = currentTime - session.startTime
                )
                _currentSession.value = updatedSession
            }
        }
    }
    
    private fun handleRecordingError(message: String) {
        Log.e(TAG, "Recording error: $message")
        
        val session = _currentSession.value
        if (session != null) {
            val errorSession = session.copy(
                status = RecordingStatus.ERROR,
                endTime = System.currentTimeMillis()
            )
            _currentSession.value = errorSession
        }
        
        cleanup()
        _isRecording.value = false
    }
    
    private fun cleanup() {
        try {
            mediaRecorder?.apply {
                reset()
                release()
            }
            mediaRecorder = null
            
            recordingJob?.cancel()
            
            Log.d(TAG, "RecordingEngine cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    private fun getRecordingFileSize(filePath: String): Long {
        return try {
            File(filePath).length()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size for: $filePath", e)
            0L
        }
    }
    
    fun release() {
        recordingScope.cancel()
        cleanup()
        Log.d(TAG, "RecordingEngine released")
    }
}

/**
 * Exception for recording-related errors
 */
class RecordingException(message: String, cause: Throwable? = null) : Exception(message, cause)