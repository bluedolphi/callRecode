package com.example.callrecode.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Base Activity class providing common functionality for all activities in the app
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    /**
     * Abstract method to create the view binding
     */
    abstract fun createBinding(): VB

    /**
     * Abstract method to set up the UI
     */
    abstract fun setupUI()

    /**
     * Abstract method to observe data changes
     */
    abstract fun observeData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = createBinding()
        setContentView(binding.root)
        
        setupUI()
        observeData()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}