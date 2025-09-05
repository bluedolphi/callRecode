package com.example.callrecode.data.repository

import kotlinx.coroutines.flow.Flow
import com.example.callrecode.data.database.dao.CallLogDao
import com.example.callrecode.data.database.entity.CallLogEntity
import java.util.UUID

/**
 * Repository class for call log data operations.
 * Implements the Repository pattern to provide a clean API for data access
 * and abstracts the data source from the business logic.
 */
class CallLogRepository(
    private val callLogDao: CallLogDao
) {
    
    /**
     * Get recent call logs with specified limit as a reactive Flow
     */
    fun getRecentCallLogs(limit: Int = 100): Flow<List<CallLogEntity>> {
        return callLogDao.getRecentCallLogs(limit)
    }
    
    /**
     * Get all call logs for a specific phone number
     */
    suspend fun getCallLogsByPhone(phoneNumber: String): List<CallLogEntity> {
        return callLogDao.getCallLogsByPhone(phoneNumber)
    }
    
    /**
     * Get a specific call log by ID
     */
    suspend fun getCallLogById(id: String): CallLogEntity? {
        return callLogDao.getCallLogById(id)
    }
    
    /**
     * Insert a new call log
     */
    suspend fun insertCallLog(callLog: CallLogEntity) {
        callLogDao.insertCallLog(callLog)
    }
    
    /**
     * Insert multiple call logs
     */
    suspend fun insertCallLogs(callLogs: List<CallLogEntity>) {
        callLogDao.insertCallLogs(callLogs)
    }
    
    /**
     * Update an existing call log
     */
    suspend fun updateCallLog(callLog: CallLogEntity) {
        callLogDao.updateCallLog(callLog)
    }
    
    /**
     * Delete a call log
     */
    suspend fun deleteCallLog(callLog: CallLogEntity) {
        callLogDao.deleteCallLog(callLog)
    }
    
    /**
     * Delete call logs older than the specified timestamp
     */
    suspend fun deleteOldCallLogs(timestamp: Long) {
        callLogDao.deleteOldCallLogs(timestamp)
    }
    
    /**
     * Get call logs within a date range
     */
    suspend fun getCallLogsByDateRange(fromTime: Long, toTime: Long): List<CallLogEntity> {
        return callLogDao.getCallLogsByDateRange(fromTime, toTime)
    }
    
    /**
     * Get recorded call logs only
     */
    suspend fun getRecordedCallLogs(): List<CallLogEntity> {
        return callLogDao.getRecordedCallLogs()
    }
    
    /**
     * Get call logs by call type
     */
    suspend fun getCallLogsByType(callType: String): List<CallLogEntity> {
        return callLogDao.getCallLogsByType(callType)
    }
    
    /**
     * Get total count of call logs
     */
    suspend fun getCallLogCount(): Int {
        return callLogDao.getCallLogCount()
    }
    
    /**
     * Get count of recorded calls
     */
    suspend fun getRecordedCallCount(): Int {
        return callLogDao.getRecordedCallCount()
    }
    
    /**
     * Update recording status for a call log
     */
    suspend fun updateRecordingStatus(id: String, isRecorded: Boolean, recordingId: String?) {
        callLogDao.updateRecordingStatus(id, isRecorded, recordingId)
    }
    
    /**
     * Get call logs that have recordings
     */
    suspend fun getCallLogsWithRecordings(): List<CallLogEntity> {
        return callLogDao.getCallLogsWithRecordings()
    }
    
    /**
     * Search call logs by contact name or phone number
     */
    suspend fun searchCallLogs(searchQuery: String): List<CallLogEntity> {
        return callLogDao.searchCallLogs(searchQuery)
    }
    
    /**
     * Create a new call log with current timestamp
     */
    suspend fun createCallLog(
        phoneNumber: String,
        contactName: String?,
        callType: String,
        startTime: Long,
        endTime: Long?,
        duration: Long,
        isRecorded: Boolean = false,
        recordingId: String? = null
    ) {
        val callLog = CallLogEntity(
            id = UUID.randomUUID().toString(),
            phoneNumber = phoneNumber,
            contactName = contactName,
            callType = callType,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            isRecorded = isRecorded,
            recordingId = recordingId,
            createdAt = System.currentTimeMillis()
        )
        insertCallLog(callLog)
    }
    
    /**
     * Link a recording to a call log
     */
    suspend fun linkRecordingToCall(callLogId: String, recordingId: String) {
        updateRecordingStatus(callLogId, true, recordingId)
    }
    
    /**
     * Unlink recording from a call log
     */
    suspend fun unlinkRecordingFromCall(callLogId: String) {
        updateRecordingStatus(callLogId, false, null)
    }
    
    /**
     * Cleanup old call logs (older than specified days)
     */
    suspend fun cleanupOldCallLogs(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        deleteOldCallLogs(cutoffTime)
    }
    
    /**
     * Get call statistics for a given time period
     */
    suspend fun getCallStatistics(fromTime: Long, toTime: Long): CallLogStatistics {
        val allCalls = getCallLogsByDateRange(fromTime, toTime)
        val recordedCalls = allCalls.filter { it.isRecorded }
        
        val incomingCalls = allCalls.filter { it.callType == "INCOMING" }
        val outgoingCalls = allCalls.filter { it.callType == "OUTGOING" }
        val missedCalls = allCalls.filter { it.callType == "MISSED" }
        
        val totalDuration = allCalls.sumOf { it.duration }
        val recordedDuration = recordedCalls.sumOf { it.duration }
        
        return CallLogStatistics(
            totalCalls = allCalls.size,
            recordedCalls = recordedCalls.size,
            incomingCalls = incomingCalls.size,
            outgoingCalls = outgoingCalls.size,
            missedCalls = missedCalls.size,
            totalDuration = totalDuration,
            recordedDuration = recordedDuration
        )
    }
}

/**
 * Data class for call log statistics
 */
data class CallLogStatistics(
    val totalCalls: Int,
    val recordedCalls: Int,
    val incomingCalls: Int,
    val outgoingCalls: Int,
    val missedCalls: Int,
    val totalDuration: Long,
    val recordedDuration: Long
)