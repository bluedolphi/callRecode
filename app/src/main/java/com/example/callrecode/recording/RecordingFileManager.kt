package com.example.callrecode.recording

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages recording file naming, storage paths, and cleanup
 */
class RecordingFileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RecordingFileManager"
        private const val RECORDINGS_DIR_NAME = "recordings"
        private const val FILE_EXTENSION = ".m4a"
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        private const val UNKNOWN_CONTACT = "unknown"
        private const val DEFAULT_CLEANUP_DAYS = 30
    }
    
    private val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
    
    /**
     * Get the recordings directory, creating it if necessary
     */
    fun getRecordingsDirectory(): File {
        // Use app's private external files directory to avoid needing WRITE_EXTERNAL_STORAGE permission
        val appFilesDir = context.getExternalFilesDir(null) ?: context.filesDir
        val recordingsDir = File(appFilesDir, RECORDINGS_DIR_NAME)
        
        if (!recordingsDir.exists()) {
            val created = recordingsDir.mkdirs()
            Log.d(TAG, "Created recordings directory: $created - ${recordingsDir.absolutePath}")
        }
        
        return recordingsDir
    }
    
    /**
     * Generate a filename for a recording
     * Format: {timestamp}_{phoneNumber}_{duration}.{extension}
     * Example: 20250905_1234567890.m4a
     */
    fun generateFileName(phoneNumber: String?, startTime: Long): String {
        val timestamp = dateFormat.format(Date(startTime))
        val sanitizedPhone = sanitizePhoneNumber(phoneNumber)
        
        return "${timestamp}_${sanitizedPhone}${FILE_EXTENSION}"
    }
    
    /**
     * Generate a filename with duration (for completed recordings)
     */
    fun generateFileNameWithDuration(phoneNumber: String?, startTime: Long, durationSeconds: Long): String {
        val timestamp = dateFormat.format(Date(startTime))
        val sanitizedPhone = sanitizePhoneNumber(phoneNumber)
        val durationStr = formatDuration(durationSeconds)
        
        return "${timestamp}_${sanitizedPhone}_${durationStr}${FILE_EXTENSION}"
    }
    
    /**
     * Get a File object for a recording
     */
    fun getRecordingFile(fileName: String): File {
        return File(getRecordingsDirectory(), fileName)
    }
    
    /**
     * Rename a recording file (useful for adding duration after recording completes)
     */
    fun renameRecordingFile(oldFileName: String, newFileName: String): Boolean {
        return try {
            val oldFile = getRecordingFile(oldFileName)
            val newFile = getRecordingFile(newFileName)
            
            if (oldFile.exists() && !newFile.exists()) {
                val renamed = oldFile.renameTo(newFile)
                Log.d(TAG, "Renamed recording file: $oldFileName -> $newFileName, success: $renamed")
                renamed
            } else {
                Log.w(TAG, "Cannot rename file: old exists=${oldFile.exists()}, new exists=${newFile.exists()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming recording file: $oldFileName -> $newFileName", e)
            false
        }
    }
    
    /**
     * Get all recording files
     */
    fun getAllRecordingFiles(): List<File> {
        return try {
            val recordingsDir = getRecordingsDirectory()
            val files = recordingsDir.listFiles { _, name -> 
                name.endsWith(FILE_EXTENSION) 
            }?.toList() ?: emptyList()
            
            // Sort by modified date, newest first
            files.sortedByDescending { it.lastModified() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recording files", e)
            emptyList()
        }
    }
    
    /**
     * Get recording files for a specific phone number
     */
    fun getRecordingFilesForNumber(phoneNumber: String?): List<File> {
        if (phoneNumber.isNullOrBlank()) return emptyList()
        
        val sanitizedPhone = sanitizePhoneNumber(phoneNumber)
        return getAllRecordingFiles().filter { file ->
            file.name.contains("_${sanitizedPhone}_") || 
            file.name.contains("_${sanitizedPhone}${FILE_EXTENSION}")
        }
    }
    
    /**
     * Delete a recording file
     */
    fun deleteRecordingFile(fileName: String): Boolean {
        return try {
            val file = getRecordingFile(fileName)
            val deleted = file.delete()
            Log.d(TAG, "Deleted recording file: $fileName, success: $deleted")
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recording file: $fileName", e)
            false
        }
    }
    
    /**
     * Clean up old recording files
     */
    fun cleanupOldRecordings(daysToKeep: Int = DEFAULT_CLEANUP_DAYS): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            val allFiles = getAllRecordingFiles()
            var deletedCount = 0
            
            for (file in allFiles) {
                if (file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted old recording: ${file.name}")
                    }
                }
            }
            
            Log.i(TAG, "Cleanup completed: deleted $deletedCount old recordings (older than $daysToKeep days)")
            deletedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            0
        }
    }
    
    /**
     * Get total size of all recording files
     */
    fun getTotalRecordingsSize(): Long {
        return try {
            getAllRecordingFiles().sumOf { it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total recordings size", e)
            0L
        }
    }
    
    /**
     * Get available storage space for recordings
     */
    fun getAvailableStorageSpace(): Long {
        return try {
            getRecordingsDirectory().usableSpace
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available storage space", e)
            0L
        }
    }
    
    /**
     * Check if there's enough space for recording
     */
    fun hasEnoughSpaceForRecording(estimatedDurationMinutes: Int, quality: RecordingQuality): Boolean {
        return try {
            val audioQualityManager = AudioQualityManager()
            val estimatedSize = audioQualityManager.getEstimatedFileSizePerMinute(quality) * estimatedDurationMinutes
            val availableSpace = getAvailableStorageSpace()
            val bufferSpace = 50 * 1024 * 1024L // 50MB buffer
            
            val hasSpace = availableSpace > (estimatedSize + bufferSpace)
            Log.d(TAG, "Space check: estimated=$estimatedSize, available=$availableSpace, hasSpace=$hasSpace")
            hasSpace
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage space", e)
            false
        }
    }
    
    // Private helper methods
    
    /**
     * Sanitize phone number for use in filename
     */
    private fun sanitizePhoneNumber(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) {
            return UNKNOWN_CONTACT
        }
        
        // Remove all non-digit characters and limit length
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when {
            digitsOnly.isEmpty() -> UNKNOWN_CONTACT
            digitsOnly.length > 15 -> digitsOnly.take(15) // Max international number length
            else -> digitsOnly
        }
    }
    
    /**
     * Format duration in seconds to a readable string
     */
    private fun formatDuration(durationSeconds: Long): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        
        return when {
            hours > 0 -> "${hours}h${minutes}m${seconds}s"
            minutes > 0 -> "${minutes}m${seconds}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Parse phone number and contact name from filename
     */
    fun parseFileNameInfo(fileName: String): FileNameInfo? {
        return try {
            // Remove extension
            val nameWithoutExt = fileName.substringBeforeLast(FILE_EXTENSION)
            val parts = nameWithoutExt.split("_")
            
            if (parts.size >= 2) {
                val timestamp = parts[0]
                val phoneNumber = if (parts[1] != UNKNOWN_CONTACT) parts[1] else null
                val duration = if (parts.size >= 3) parts[2] else null
                
                FileNameInfo(
                    timestamp = timestamp,
                    phoneNumber = phoneNumber,
                    duration = duration
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing filename: $fileName", e)
            null
        }
    }
}

/**
 * Information extracted from a recording filename
 */
data class FileNameInfo(
    val timestamp: String,
    val phoneNumber: String?,
    val duration: String?
)