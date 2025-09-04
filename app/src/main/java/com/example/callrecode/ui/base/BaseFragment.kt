package com.example.callrecode.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Base Fragment class providing common functionality for all fragments in the app
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    /**
     * Abstract method to create the view binding
     */
    abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * Abstract method to set up the UI
     */
    abstract fun setupUI()

    /**
     * Abstract method to observe data changes
     */
    abstract fun observeData()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}