package com.example.callrecode.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.example.callrecode.ui.base.BaseFragment

/**
 * Main fragment for the Call Recorder application
 * This is a placeholder class for the MVVM architecture directory structure
 */
class MainFragment : BaseFragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MainScreen()
            }
        }
    }
    
    companion object {
        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }
}