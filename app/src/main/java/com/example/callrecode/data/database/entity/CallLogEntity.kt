package com.example.callrecode.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing call logs in the database.
 * This table stores information about phone calls and their recording status.
 */
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey 
    val id: String,
    
    // Call information
    val phoneNumber: String,
    val contactName: String?,
    val callType: String, // "INCOMING", "OUTGOING", "MISSED"
    val startTime: Long,
    val endTime: Long?,
    val duration: Long,
    
    // Recording association
    val isRecorded: Boolean = false,
    val recordingId: String?,
    
    // Audit fields
    val createdAt: Long
)