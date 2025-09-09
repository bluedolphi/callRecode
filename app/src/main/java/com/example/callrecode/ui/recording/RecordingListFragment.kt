package com.example.callrecode.ui.recording

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.callrecode.data.repository.RecordingRepository
import com.example.callrecode.databinding.FragmentRecordingListBinding
import com.example.callrecode.ui.base.BaseFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * 录音列表Fragment
 * 显示所有录音文件，支持搜索、筛选和播放控制
 */
class RecordingListFragment : BaseFragment() {

    private var _binding: FragmentRecordingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordingListViewModel by viewModels()
    private lateinit var recordingAdapter: RecordingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchAndFilter()
        setupClickListeners()
        observeViewModel()
        
        // Load recordings
        viewModel.loadRecordings()
    }

    private fun setupRecyclerView() {
        recordingAdapter = RecordingAdapter { recording ->
            // Handle recording item click
            // TODO: Navigate to recording details or play recording
        }

        binding.recordingList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordingAdapter
            
            // Add item decoration for spacing
            addItemDecoration(RecordingItemDecoration())
        }
    }

    private fun setupSearchAndFilter() {
        // Setup search functionality
        binding.searchEditText.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: "")
                hideKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        // Setup filter chips
        binding.filterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chip_all -> viewModel.setFilter(RecordingListViewModel.Filter.ALL)
                R.id.chip_today -> viewModel.setFilter(RecordingListViewModel.Filter.TODAY)
                R.id.chip_week -> viewModel.setFilter(RecordingListViewModel.Filter.THIS_WEEK)
                R.id.chip_unknown -> viewModel.setFilter(RecordingListViewModel.Filter.UNKNOWN_NUMBERS)
                else -> viewModel.setFilter(RecordingListViewModel.Filter.ALL)
            }
        }

        // Setup filter button
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupClickListeners() {
        // Manual recording FAB
        binding.manualRecordFab.setOnClickListener {
            startManualRecording()
        }
    }

    private fun observeViewModel() {
        viewModel.recordings.observe(viewLifecycleOwner) { recordings ->
            recordingAdapter.submitList(recordings)
            
            // Update empty state
            if (recordings.isEmpty()) {
                binding.recordingList.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            } else {
                binding.recordingList.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
            }
            
            // Update stats
            updateStats(recordings)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide loading indicator if needed
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            // Show error message
            showErrorToast(error)
        }
    }

    private fun updateStats(recordings: List<RecordingItem>) {
        val totalRecordings = recordings.size
        val totalDuration = recordings.sumOf { it.duration }
        val totalSize = recordings.sumOf { it.fileSize }

        binding.statsText.text = "共 $totalRecordings 个录音 • 总时长 ${formatDuration(totalDuration)} • 总大小 ${formatFileSize(totalSize)}"
    }

    private fun startManualRecording() {
        // TODO: Implement manual recording
        showInfoToast("手动录音功能开发中")
    }

    private fun showFilterDialog() {
        // TODO: Implement advanced filter dialog
        showInfoToast("高级筛选功能开发中")
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h${minutes % 60}m"
            minutes > 0 -> "${minutes}m${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = RecordingListFragment()
    }
}