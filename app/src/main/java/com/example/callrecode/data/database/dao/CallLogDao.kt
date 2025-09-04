package com.example.callrecode.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.callrecode.data.database.entity.CallLogEntity

/**
 * Data Access Object (DAO) for CallLogEntity.
 * Provides database operations for call logs.
 */
@Dao
interface CallLogDao {
    
    /**
     * Get recent call logs with a specified limit
     */
    @Query("SELECT * FROM call_logs ORDER BY startTime DESC LIMIT :limit")
    fun getRecentCallLogs(limit: Int = 100): Flow<List<CallLogEntity>>
    
    /**
     * Get all call logs for a specific phone number
     */
    @Query("SELECT * FROM call_logs WHERE phoneNumber = :phoneNumber ORDER BY startTime DESC")
    suspend fun getCallLogsByPhone(phoneNumber: String): List<CallLogEntity>
    
    /**
     * Get a specific call log by ID
     */
    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getCallLogById(id: String): CallLogEntity?
    
    /**
     * Insert a new call log
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)
    
    /**
     * Insert multiple call logs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(callLogs: List<CallLogEntity>)
    
    /**
     * Update a call log
     */
    @Update
    suspend fun updateCallLog(callLog: CallLogEntity)
    
    /**
     * Delete a call log
     */
    @Delete
    suspend fun deleteCallLog(callLog: CallLogEntity)
    
    /**
     * Delete call logs older than the specified timestamp
     */
    @Query("DELETE FROM call_logs WHERE startTime < :timestamp")
    suspend fun deleteOldCallLogs(timestamp: Long)
    
    /**
     * Get call logs within a date range
     */
    @Query("SELECT * FROM call_logs WHERE startTime >= :fromTime AND startTime <= :toTime ORDER BY startTime DESC")
    suspend fun getCallLogsByDateRange(fromTime: Long, toTime: Long): List<CallLogEntity>
    
    /**
     * Get recorded call logs only
     */
    @Query("SELECT * FROM call_logs WHERE isRecorded = 1 ORDER BY startTime DESC")
    suspend fun getRecordedCallLogs(): List<CallLogEntity>
    
    /**
     * Get call logs by call type
     */
    @Query("SELECT * FROM call_logs WHERE callType = :callType ORDER BY startTime DESC")
    suspend fun getCallLogsByType(callType: String): List<CallLogEntity>
    
    /**
     * Get total count of call logs
     */
    @Query("SELECT COUNT(*) FROM call_logs")
    suspend fun getCallLogCount(): Int
    
    /**
     * Get count of recorded calls
     */
    @Query("SELECT COUNT(*) FROM call_logs WHERE isRecorded = 1")
    suspend fun getRecordedCallCount(): Int
    
    /**
     * Update recording status for a call log
     */
    @Query("UPDATE call_logs SET isRecorded = :isRecorded, recordingId = :recordingId WHERE id = :id")
    suspend fun updateRecordingStatus(id: String, isRecorded: Boolean, recordingId: String?)
    
    /**
     * Get call logs that have recordings
     */
    @Query("SELECT * FROM call_logs WHERE recordingId IS NOT NULL ORDER BY startTime DESC")
    suspend fun getCallLogsWithRecordings(): List<CallLogEntity>
    
    /**
     * Search call logs by contact name or phone number
     */
    @Query("""
        SELECT * FROM call_logs 
        WHERE contactName LIKE '%' || :searchQuery || '%' 
           OR phoneNumber LIKE '%' || :searchQuery || '%'
        ORDER BY startTime DESC
    """)
    suspend fun searchCallLogs(searchQuery: String): List<CallLogEntity>
}