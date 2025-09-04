package com.example.callrecode.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base Repository class providing common functionality for all repositories in the app
 */
abstract class BaseRepository {

    /**
     * Execute a database operation on IO dispatcher
     */
    protected suspend fun <T> ioOperation(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }

    /**
     * Execute a network operation on IO dispatcher
     */
    protected suspend fun <T> networkOperation(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }

    /**
     * Handle repository errors
     */
    protected fun handleRepositoryError(exception: Exception): Nothing {
        // Log error and re-throw
        exception.printStackTrace()
        throw exception
    }
}