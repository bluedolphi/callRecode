package com.example.callrecode.ui.recording

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.example.callrecode.ui.base.BaseFragment

/**
 * Recording fragment for call recording functionality
 * This is a placeholder class for the MVVM architecture directory structure
 */
class RecordingFragment : BaseFragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                RecordingScreen()
            }
        }
    }
    
    companion object {
        fun newInstance(): RecordingFragment {
            return RecordingFragment()
        }
    }
}