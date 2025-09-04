package com.example.callrecode.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a call recording in the database.
 * This table stores metadata about call recordings including file information,
 * call details, and upload status.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey 
    val id: String,
    
    // File information
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val quality: String, // "STANDARD", "HIGH"
    
    // Call information
    val phoneNumber: String,
    val contactName: String?,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val recordingMode: String, // "AUTO", "MANUAL"
    
    // Upload status
    val isUploaded: Boolean = false,
    val uploadTime: Long? = null,
    
    // Audit fields
    val createdAt: Long,
    val updatedAt: Long
)