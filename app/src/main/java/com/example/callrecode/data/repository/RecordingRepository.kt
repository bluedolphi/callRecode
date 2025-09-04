package com.example.callrecode.data.repository

import kotlinx.coroutines.flow.Flow
import com.example.callrecode.data.database.dao.RecordingDao
import com.example.callrecode.data.database.entity.RecordingEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for recording data operations.
 * Implements the Repository pattern to provide a clean API for data access
 * and abstracts the data source (Room database) from the business logic.
 */
@Singleton
class RecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao
) {
    
    /**
     * Get all recordings as a reactive Flow
     */
    fun getAllRecordings(): Flow<List<RecordingEntity>> {
        return recordingDao.getAllRecordings()
    }
    
    /**
     * Get a specific recording by ID
     */
    suspend fun getRecordingById(id: String): RecordingEntity? {
        return recordingDao.getRecordingById(id)
    }
    
    /**
     * Get all recordings for a specific phone number
     */
    suspend fun getRecordingsByPhone(phoneNumber: String): List<RecordingEntity> {
        return recordingDao.getRecordingsByPhone(phoneNumber)
    }
    
    /**
     * Insert a new recording
     */
    suspend fun insertRecording(recording: RecordingEntity) {
        recordingDao.insertRecording(recording)
    }
    
    /**
     * Update an existing recording
     */
    suspend fun updateRecording(recording: RecordingEntity) {
        recordingDao.updateRecording(recording)
    }
    
    /**
     * Delete a recording
     */
    suspend fun deleteRecording(recording: RecordingEntity) {
        recordingDao.deleteRecording(recording)
    }
    
    /**
     * Delete a recording by ID
     */
    suspend fun deleteRecordingById(id: String) {
        recordingDao.deleteRecordingById(id)
    }
    
    /**
     * Get all recordings that haven't been uploaded
     */
    suspend fun getUnuploadedRecordings(): List<RecordingEntity> {
        return recordingDao.getUnuploadedRecordings()
    }
    
    /**
     * Mark a recording as uploaded
     */
    suspend fun markAsUploaded(id: String, uploadTime: Long = System.currentTimeMillis()) {
        recordingDao.markAsUploaded(id, uploadTime)
    }
    
    /**
     * Get recordings within a date range
     */
    suspend fun getRecordingsByDateRange(fromTime: Long, toTime: Long): List<RecordingEntity> {
        return recordingDao.getRecordingsByDateRange(fromTime, toTime)
    }
    
    /**
     * Get total count of recordings
     */
    suspend fun getRecordingCount(): Int {
        return recordingDao.getRecordingCount()
    }
    
    /**
     * Get total file size of all recordings
     */
    suspend fun getTotalFileSize(): Long {
        return recordingDao.getTotalFileSize() ?: 0L
    }
    
    /**
     * Create a new recording with current timestamp
     */
    suspend fun createRecording(
        id: String,
        fileName: String,
        filePath: String,
        phoneNumber: String,
        contactName: String?,
        startTime: Long,
        endTime: Long,
        duration: Long,
        fileSize: Long,
        quality: String,
        recordingMode: String
    ) {
        val currentTime = System.currentTimeMillis()
        val recording = RecordingEntity(
            id = id,
            fileName = fileName,
            filePath = filePath,
            phoneNumber = phoneNumber,
            contactName = contactName,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            fileSize = fileSize,
            quality = quality,
            recordingMode = recordingMode,
            isUploaded = false,
            uploadTime = null,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        insertRecording(recording)
    }
    
    /**
     * Update recording with new timestamp
     */
    suspend fun updateRecordingWithTimestamp(recording: RecordingEntity) {
        val updatedRecording = recording.copy(
            updatedAt = System.currentTimeMillis()
        )
        updateRecording(updatedRecording)
    }
}