package com.example.callrecode.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.callrecode.data.database.entity.RecordingEntity

/**
 * Data Access Object (DAO) for RecordingEntity.
 * Provides database operations for call recordings.
 */
@Dao
interface RecordingDao {
    
    /**
     * Get all recordings ordered by start time (newest first)
     */
    @Query("SELECT * FROM recordings ORDER BY startTime DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>
    
    /**
     * Get a specific recording by its ID
     */
    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): RecordingEntity?
    
    /**
     * Get all recordings for a specific phone number
     */
    @Query("SELECT * FROM recordings WHERE phoneNumber = :phoneNumber ORDER BY startTime DESC")
    suspend fun getRecordingsByPhone(phoneNumber: String): List<RecordingEntity>
    
    /**
     * Insert a new recording (replaces if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)
    
    /**
     * Update an existing recording
     */
    @Update
    suspend fun updateRecording(recording: RecordingEntity)
    
    /**
     * Delete a recording entity
     */
    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)
    
    /**
     * Delete a recording by its ID
     */
    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: String)
    
    /**
     * Get all recordings that haven't been uploaded yet
     */
    @Query("SELECT * FROM recordings WHERE isUploaded = 0")
    suspend fun getUnuploadedRecordings(): List<RecordingEntity>
    
    /**
     * Mark a recording as uploaded
     */
    @Query("UPDATE recordings SET isUploaded = 1, uploadTime = :uploadTime WHERE id = :id")
    suspend fun markAsUploaded(id: String, uploadTime: Long)
    
    /**
     * Get recordings within a date range
     */
    @Query("SELECT * FROM recordings WHERE startTime >= :fromTime AND startTime <= :toTime ORDER BY startTime DESC")
    suspend fun getRecordingsByDateRange(fromTime: Long, toTime: Long): List<RecordingEntity>
    
    /**
     * Get total count of recordings
     */
    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getRecordingCount(): Int
    
    /**
     * Get total size of all recordings
     */
    @Query("SELECT SUM(fileSize) FROM recordings")
    suspend fun getTotalFileSize(): Long?
}