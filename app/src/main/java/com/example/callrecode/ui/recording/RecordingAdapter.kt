package com.example.callrecode.ui.recording

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.callrecode.databinding.ItemRecordingBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * 录音列表适配器
 * 使用ListAdapter实现高效的列表更新
 */
class RecordingAdapter(
    private val onRecordingClick: (RecordingListViewModel.RecordingItem) -> Unit
) : ListAdapter<RecordingListViewModel.RecordingItem, RecordingAdapter.RecordingViewHolder>(RecordingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val binding = ItemRecordingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecordingViewHolder(
        private val binding: ItemRecordingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(recording: RecordingListViewModel.RecordingItem) {
            // Set contact info
            binding.contactName.text = recording.contactName ?: "未知号码"
            binding.phoneNumber.text = recording.phoneNumber ?: ""

            // Set time and duration
            binding.timeText.text = dateFormat.format(Date(recording.startTime))
            binding.durationText.text = formatDuration(recording.duration)

            // Set file info
            binding.fileSizeText.text = formatFileSize(recording.fileSize)
            binding.qualityText.text = recording.quality

            // Set upload status
            if (recording.isUploaded) {
                binding.uploadStatusContainer.visibility = View.VISIBLE
                binding.uploadStatusText.text = "已上传"
                binding.uploadStatusIcon.setImageResource(R.drawable.ic_cloud_done)
            } else {
                binding.uploadStatusContainer.visibility = View.GONE
            }

            // Set status icon based on recording mode
            val statusIcon = when (recording.recordingMode) {
                "AUTO" -> R.drawable.ic_auto_record
                "MANUAL" -> R.drawable.ic_manual_record
                else -> R.drawable.ic_audio_file
            }
            binding.statusIcon.setImageResource(statusIcon)

            // Set click listeners
            binding.root.setOnClickListener {
                onRecordingClick(recording)
            }

            binding.playButton.setOnClickListener {
                // TODO: Implement play functionality
            }

            binding.shareButton.setOnClickListener {
                // TODO: Implement share functionality
            }

            binding.deleteButton.setOnClickListener {
                // TODO: Implement delete functionality
            }
        }

        private fun formatDuration(milliseconds: Long): String {
            val seconds = milliseconds / 1000
            val minutes = seconds / 60
            val hours = minutes / 60

            return when {
                hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
                else -> String.format("%02d:%02d", minutes, seconds % 60)
            }
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
            }
        }
    }

    /**
     * DiffUtil回调用于高效更新列表
     */
    private class RecordingDiffCallback : DiffUtil.ItemCallback<RecordingListViewModel.RecordingItem>() {
        override fun areItemsTheSame(
            oldItem: RecordingListViewModel.RecordingItem,
            newItem: RecordingListViewModel.RecordingItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: RecordingListViewModel.RecordingItem,
            newItem: RecordingListViewModel.RecordingItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}