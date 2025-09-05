package com.example.callrecode.recording

import java.util.UUID

/**
 * Represents a recording session with all relevant metadata
 */
data class RecordingSession(
    val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String?,
    val contactName: String?,
    val startTime: Long,
    val filePath: String,
    val quality: RecordingQuality,
    val mode: RecordingMode,
    var status: RecordingStatus = RecordingStatus.RECORDING,
    var endTime: Long? = null,
    var duration: Long = 0L,
    var fileSize: Long = 0L
) {
    /**
     * Calculate current duration if recording is ongoing
     */
    fun getCurrentDuration(): Long {
        return if (status == RecordingStatus.RECORDING) {
            System.currentTimeMillis() - startTime
        } else {
            duration
        }
    }
}

/**
 * Recording session status
 */
enum class RecordingStatus {
    RECORDING,   // Currently recording
    PAUSED,      // Recording paused
    STOPPED,     // Recording stopped normally
    ERROR        // Recording stopped due to error
}

/**
 * Recording quality presets
 */
enum class RecordingQuality(
    val displayName: String,
    val sampleRate: Int,
    val bitRate: Int,
    val channels: Int,
    val outputFormat: Int,
    val audioEncoder: Int
) {
    STANDARD(
        displayName = "标准质量",
        sampleRate = 8000,
        bitRate = 12200,
        channels = 1,
        outputFormat = android.media.MediaRecorder.OutputFormat.AMR_NB,
        audioEncoder = android.media.MediaRecorder.AudioEncoder.AMR_NB
    ),
    HIGH(
        displayName = "高质量",
        sampleRate = 44100,
        bitRate = 128000,
        channels = 2,
        outputFormat = android.media.MediaRecorder.OutputFormat.MPEG_4,
        audioEncoder = android.media.MediaRecorder.AudioEncoder.AAC
    )
}

/**
 * Recording modes
 */
enum class RecordingMode(val displayName: String) {
    AUTO("自动录音"),           // Auto record all calls
    MANUAL("手动录音"),         // Manual recording control
    ASK_UNKNOWN("陌生号码询问")  // Ask before recording unknown numbers
}