package com.example.callrecode.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.callrecode.data.database.entity.UploadConfigEntity

/**
 * Data Access Object (DAO) for UploadConfigEntity.
 * Provides database operations for upload configuration settings.
 */
@Dao
interface UploadConfigDao {
    
    /**
     * Get the current upload configuration
     * Since there's typically only one config, this returns the first one
     */
    @Query("SELECT * FROM upload_configs LIMIT 1")
    suspend fun getUploadConfig(): UploadConfigEntity?
    
    /**
     * Get the current upload configuration as Flow for reactive updates
     */
    @Query("SELECT * FROM upload_configs LIMIT 1")
    fun getUploadConfigFlow(): Flow<UploadConfigEntity?>
    
    /**
     * Insert or update upload configuration
     * Uses REPLACE strategy to ensure there's only one config
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: UploadConfigEntity)
    
    /**
     * Update upload configuration
     */
    @Update
    suspend fun updateConfig(config: UploadConfigEntity)
    
    /**
     * Clear all upload configurations
     */
    @Query("DELETE FROM upload_configs")
    suspend fun clearConfigs()
    
    /**
     * Check if upload is enabled
     */
    @Query("SELECT uploadEnabled FROM upload_configs LIMIT 1")
    suspend fun isUploadEnabled(): Boolean?
    
    /**
     * Check if auto-upload is enabled
     */
    @Query("SELECT autoUpload FROM upload_configs LIMIT 1")
    suspend fun isAutoUploadEnabled(): Boolean?
    
    /**
     * Check if WiFi-only upload is enabled
     */
    @Query("SELECT wifiOnly FROM upload_configs LIMIT 1")
    suspend fun isWifiOnlyEnabled(): Boolean?
    
    /**
     * Update server URL
     */
    @Query("UPDATE upload_configs SET serverUrl = :serverUrl, updatedAt = :updatedAt")
    suspend fun updateServerUrl(serverUrl: String, updatedAt: Long)
    
    /**
     * Update API key
     */
    @Query("UPDATE upload_configs SET apiKey = :apiKey, updatedAt = :updatedAt")
    suspend fun updateApiKey(apiKey: String, updatedAt: Long)
    
    /**
     * Enable/disable upload
     */
    @Query("UPDATE upload_configs SET uploadEnabled = :enabled, updatedAt = :updatedAt")
    suspend fun setUploadEnabled(enabled: Boolean, updatedAt: Long)
    
    /**
     * Enable/disable auto-upload
     */
    @Query("UPDATE upload_configs SET autoUpload = :enabled, updatedAt = :updatedAt")
    suspend fun setAutoUploadEnabled(enabled: Boolean, updatedAt: Long)
}