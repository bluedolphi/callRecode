package com.example.callrecode.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Base ViewModel class providing common functionality for all ViewModels in the app
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * Launch a coroutine in viewModelScope with proper exception handling
     */
    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Handle errors that occur during coroutine execution
     * Override this method in child classes to provide specific error handling
     */
    protected open fun handleError(exception: Exception) {
        // Default error handling - log the exception
        exception.printStackTrace()
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup resources if needed
    }
}