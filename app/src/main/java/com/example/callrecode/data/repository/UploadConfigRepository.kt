package com.example.callrecode.data.repository

import kotlinx.coroutines.flow.Flow
import com.example.callrecode.data.database.dao.UploadConfigDao
import com.example.callrecode.data.database.entity.UploadConfigEntity
import java.util.UUID

/**
 * Repository class for upload configuration data operations.
 * Implements the Repository pattern to provide a clean API for data access
 * and abstracts the data source from the business logic.
 */
class UploadConfigRepository(
    private val uploadConfigDao: UploadConfigDao
) {
    
    /**
     * Get the current upload configuration
     */
    suspend fun getUploadConfig(): UploadConfigEntity? {
        return uploadConfigDao.getUploadConfig()
    }
    
    /**
     * Get the current upload configuration as Flow for reactive updates
     */
    fun getUploadConfigFlow(): Flow<UploadConfigEntity?> {
        return uploadConfigDao.getUploadConfigFlow()
    }
    
    /**
     * Insert or update upload configuration
     */
    suspend fun saveUploadConfig(config: UploadConfigEntity) {
        uploadConfigDao.insertOrUpdateConfig(config)
    }
    
    /**
     * Update existing upload configuration
     */
    suspend fun updateUploadConfig(config: UploadConfigEntity) {
        val updatedConfig = config.copy(
            updatedAt = System.currentTimeMillis()
        )
        uploadConfigDao.updateConfig(updatedConfig)
    }
    
    /**
     * Clear all upload configurations
     */
    suspend fun clearConfigs() {
        uploadConfigDao.clearConfigs()
    }
    
    /**
     * Check if upload is enabled
     */
    suspend fun isUploadEnabled(): Boolean {
        return uploadConfigDao.isUploadEnabled() ?: false
    }
    
    /**
     * Check if auto-upload is enabled
     */
    suspend fun isAutoUploadEnabled(): Boolean {
        return uploadConfigDao.isAutoUploadEnabled() ?: false
    }
    
    /**
     * Check if WiFi-only upload is enabled
     */
    suspend fun isWifiOnlyEnabled(): Boolean {
        return uploadConfigDao.isWifiOnlyEnabled() ?: true // Default to WiFi-only
    }
    
    /**
     * Update server URL
     */
    suspend fun updateServerUrl(serverUrl: String) {
        uploadConfigDao.updateServerUrl(serverUrl, System.currentTimeMillis())
    }
    
    /**
     * Update API key
     */
    suspend fun updateApiKey(apiKey: String) {
        uploadConfigDao.updateApiKey(apiKey, System.currentTimeMillis())
    }
    
    /**
     * Enable or disable upload
     */
    suspend fun setUploadEnabled(enabled: Boolean) {
        uploadConfigDao.setUploadEnabled(enabled, System.currentTimeMillis())
    }
    
    /**
     * Enable or disable auto-upload
     */
    suspend fun setAutoUploadEnabled(enabled: Boolean) {
        uploadConfigDao.setAutoUploadEnabled(enabled, System.currentTimeMillis())
    }
    
    /**
     * Create default upload configuration
     */
    suspend fun createDefaultConfig(): UploadConfigEntity {
        val currentTime = System.currentTimeMillis()
        val defaultConfig = UploadConfigEntity(
            id = UUID.randomUUID().toString(),
            serverUrl = "",
            apiKey = "",
            uploadEnabled = false,
            autoUpload = false,
            wifiOnly = true,
            compressionEnabled = true,
            retryAttempts = 3,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        saveUploadConfig(defaultConfig)
        return defaultConfig
    }
    
    /**
     * Get or create upload configuration
     * Returns existing config or creates a default one if none exists
     */
    suspend fun getOrCreateUploadConfig(): UploadConfigEntity {
        return getUploadConfig() ?: createDefaultConfig()
    }
    
    /**
     * Update upload settings in bulk
     */
    suspend fun updateUploadSettings(
        serverUrl: String? = null,
        apiKey: String? = null,
        uploadEnabled: Boolean? = null,
        autoUpload: Boolean? = null,
        wifiOnly: Boolean? = null,
        compressionEnabled: Boolean? = null,
        retryAttempts: Int? = null
    ) {
        val existingConfig = getOrCreateUploadConfig()
        val currentTime = System.currentTimeMillis()
        
        val updatedConfig = existingConfig.copy(
            serverUrl = serverUrl ?: existingConfig.serverUrl,
            apiKey = apiKey ?: existingConfig.apiKey,
            uploadEnabled = uploadEnabled ?: existingConfig.uploadEnabled,
            autoUpload = autoUpload ?: existingConfig.autoUpload,
            wifiOnly = wifiOnly ?: existingConfig.wifiOnly,
            compressionEnabled = compressionEnabled ?: existingConfig.compressionEnabled,
            retryAttempts = retryAttempts ?: existingConfig.retryAttempts,
            updatedAt = currentTime
        )
        
        updateUploadConfig(updatedConfig)
    }
}