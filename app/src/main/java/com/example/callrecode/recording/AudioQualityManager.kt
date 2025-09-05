package com.example.callrecode.recording

import android.media.MediaRecorder
import android.util.Log

/**
 * Manages audio recording quality settings and MediaRecorder configuration
 */
class AudioQualityManager {
    companion object {
        private const val TAG = "AudioQualityManager"
    }
    
    /**
     * Configure MediaRecorder with specified quality settings
     */
    fun configureRecorder(
        recorder: MediaRecorder,
        quality: RecordingQuality,
        outputFile: String
    ) {
        try {
            Log.d(TAG, "Configuring recorder with quality: ${quality.displayName}")
            
            // Set audio source (MIC for general recording, VOICE_CALL for call recording if supported)
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            
            // Set output format
            recorder.setOutputFormat(quality.outputFormat)
            
            // Set audio encoder
            recorder.setAudioEncoder(quality.audioEncoder)
            
            // Set audio parameters
            recorder.setAudioSamplingRate(quality.sampleRate)
            recorder.setAudioEncodingBitRate(quality.bitRate)
            recorder.setAudioChannels(quality.channels)
            
            // Set output file
            recorder.setOutputFile(outputFile)
            
            Log.d(TAG, "MediaRecorder configured successfully")
            Log.d(TAG, "Sample Rate: ${quality.sampleRate}Hz")
            Log.d(TAG, "Bit Rate: ${quality.bitRate}bps")
            Log.d(TAG, "Channels: ${quality.channels}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring MediaRecorder", e)
            throw RecordingConfigurationException("Failed to configure recording quality", e)
        }
    }
    
    /**
     * Get estimated file size per minute for given quality
     */
    fun getEstimatedFileSizePerMinute(quality: RecordingQuality): Long {
        // Rough calculation: (bitRate / 8) * 60 seconds
        return (quality.bitRate / 8) * 60L
    }
    
    /**
     * Get recommended quality based on available storage space
     */
    fun getRecommendedQuality(availableSpaceBytes: Long, estimatedDurationMinutes: Int): RecordingQuality {
        val highQualitySize = getEstimatedFileSizePerMinute(RecordingQuality.HIGH) * estimatedDurationMinutes
        
        return if (availableSpaceBytes > highQualitySize * 2) { // 2x buffer
            RecordingQuality.HIGH
        } else {
            RecordingQuality.STANDARD
        }
    }
}

/**
 * Exception thrown when recording configuration fails
 */
class RecordingConfigurationException(message: String, cause: Throwable? = null) : Exception(message, cause)