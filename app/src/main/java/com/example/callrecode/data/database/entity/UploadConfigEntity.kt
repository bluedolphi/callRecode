package com.example.callrecode.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing upload configuration settings in the database.
 * This table stores cloud upload settings and preferences.
 */
@Entity(tableName = "upload_configs")
data class UploadConfigEntity(
    @PrimaryKey 
    val id: String,
    
    // Server configuration
    val serverUrl: String,
    val apiKey: String,
    
    // Upload settings
    val uploadEnabled: Boolean = true,
    val autoUpload: Boolean = false,
    val wifiOnly: Boolean = true,
    val compressionEnabled: Boolean = true,
    val retryAttempts: Int = 3,
    
    // Audit fields
    val createdAt: Long,
    val updatedAt: Long
)