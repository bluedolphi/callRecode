package com.example.callrecode.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Utility class for handling runtime permissions
 */
object PermissionUtil {

    const val REQUEST_CODE_PERMISSIONS = 100

    /**
     * Array of all required permissions for the app
     */
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    /**
     * Check if all required permissions are granted
     */
    fun allPermissionsGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if a specific permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get list of permissions that are not granted
     */
    fun getNotGrantedPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            !isPermissionGranted(context, permission)
        }
    }
}