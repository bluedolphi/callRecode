package com.example.callrecode.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.callrecode.data.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 录音列表ViewModel
 * 管理录音列表的数据和状态
 */
class RecordingListViewModel(
    private val recordingRepository: RecordingRepository = RecordingRepository()
) : ViewModel() {

    data class RecordingItem(
        val id: String,
        val fileName: String,
        val filePath: String,
        val phoneNumber: String?,
        val contactName: String?,
        val startTime: Long,
        val duration: Long,
        val fileSize: Long,
        val quality: String,
        val recordingMode: String,
        val isUploaded: Boolean
    )

    sealed class Filter {
        object ALL : Filter()
        object TODAY : Filter()
        object THIS_WEEK : Filter()
        object UNKNOWN_NUMBERS : Filter()
    }

    private val _recordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    val recordings: StateFlow<List<RecordingItem>> = _recordings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentFilter: Filter = Filter.ALL
    private var currentSearchQuery: String = ""

    fun loadRecordings() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Load recordings from repository
                val recordingEntities = recordingRepository.getAllRecordings()
                
                // Convert to UI items
                val recordingItems = recordingEntities.map { entity ->
                    RecordingItem(
                        id = entity.id,
                        fileName = entity.fileName,
                        filePath = entity.filePath,
                        phoneNumber = entity.phoneNumber,
                        contactName = entity.contactName,
                        startTime = entity.startTime,
                        duration = entity.duration,
                        fileSize = entity.fileSize,
                        quality = entity.quality,
                        recordingMode = entity.recordingMode,
                        isUploaded = entity.isUploaded
                    )
                }

                // Apply current filter and search
                val filteredRecordings = applyFilterAndSearch(recordingItems)
                _recordings.value = filteredRecordings

            } catch (e: Exception) {
                _error.value = "加载录音失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: Filter) {
        currentFilter = filter
        // Reload recordings with new filter
        loadRecordings()
    }

    fun setSearchQuery(query: String) {
        currentSearchQuery = query
        // Apply search to current recordings
        viewModelScope.launch {
            val currentRecordings = recordingRepository.getAllRecordings()
            val recordingItems = currentRecordings.map { entity ->
                RecordingItem(
                    id = entity.id,
                    fileName = entity.fileName,
                    filePath = entity.filePath,
                    phoneNumber = entity.phoneNumber,
                    contactName = entity.contactName,
                    startTime = entity.startTime,
                    duration = entity.duration,
                    fileSize = entity.fileSize,
                    quality = entity.quality,
                    recordingMode = entity.recordingMode,
                    isUploaded = entity.isUploaded
                )
            }
            val filteredRecordings = applyFilterAndSearch(recordingItems)
            _recordings.value = filteredRecordings
        }
    }

    private fun applyFilterAndSearch(recordings: List<RecordingItem>): List<RecordingItem> {
        var filtered = recordings

        // Apply time filter
        filtered = when (currentFilter) {
            Filter.ALL -> filtered
            Filter.TODAY -> {
                val todayStart = System.currentTimeMillis() / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000)
                filtered.filter { it.startTime >= todayStart }
            }
            Filter.THIS_WEEK -> {
                val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                filtered.filter { it.startTime >= weekStart }
            }
            Filter.UNKNOWN_NUMBERS -> {
                filtered.filter { it.contactName == null }
            }
        }

        // Apply search query
        if (currentSearchQuery.isNotBlank()) {
            val query = currentSearchQuery.lowercase()
            filtered = filtered.filter { recording ->
                recording.contactName?.lowercase()?.contains(query) == true ||
                recording.phoneNumber?.lowercase()?.contains(query) == true ||
                recording.fileName.lowercase().contains(query)
            }
        }

        // Sort by start time (newest first)
        return filtered.sortedByDescending { it.startTime }
    }

    fun refreshRecordings() {
        loadRecordings()
    }

    fun clearError() {
        _error.value = null
    }
}