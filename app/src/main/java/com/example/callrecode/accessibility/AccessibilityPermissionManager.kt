package com.example.callrecode.accessibility

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

/**
 * Manages AccessibilityService permissions and user guidance
 */
class AccessibilityPermissionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AccessibilityPermissionManager"
    }
    
    /**
     * Check if AccessibilityService is enabled for this app
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val serviceId = "${context.packageName}/${WeChatAccessibilityService::class.java.name}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            val isEnabled = !TextUtils.isEmpty(enabledServices) && 
                           enabledServices.contains(serviceId)
            
            Log.d(TAG, "AccessibilityService enabled: $isEnabled")
            Log.d(TAG, "Service ID: $serviceId")
            Log.d(TAG, "Enabled services: $enabledServices")
            
            isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking AccessibilityService permission", e)
            false
        }
    }
    
    /**
     * Check if AccessibilityService is running
     */
    fun isAccessibilityServiceRunning(): Boolean {
        return WeChatAccessibilityService.isServiceRunning()
    }
    
    /**
     * Open AccessibilityService settings page
     */
    fun openAccessibilitySettings(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    /**
     * Create intent to open app-specific accessibility settings
     */
    fun openAppAccessibilitySettings(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Unfortunately, we can't directly navigate to our specific service
            // Users need to find our app in the accessibility settings manually
        }
    }
    
    /**
     * Get user-friendly instructions for enabling accessibility service
     */
    fun getAccessibilityInstructions(): AccessibilityInstructions {
        return AccessibilityInstructions(
            title = "启用无障碍服务",
            steps = listOf(
                "1. 打开手机设置",
                "2. 进入"无障碍"或"辅助功能"设置",
                "3. 找到"智能通话录音助手"",
                "4. 开启服务并授予权限",
                "5. 返回应用完成设置"
            ),
            warning = "无障碍服务仅用于检测微信通话状态，不会收集其他信息",
            importance = "此权限是微信通话录音功能必需的"
        )
    }
    
    /**
     * Check if user needs to be guided to enable accessibility service
     */
    fun shouldShowAccessibilityGuide(): Boolean {
        return !isAccessibilityServiceEnabled() || !isAccessibilityServiceRunning()
    }
    
    /**
     * Get accessibility service status for UI display
     */
    fun getAccessibilityServiceStatus(): AccessibilityServiceStatus {
        val enabled = isAccessibilityServiceEnabled()
        val running = isAccessibilityServiceRunning()
        
        return when {
            enabled && running -> AccessibilityServiceStatus.ACTIVE
            enabled && !running -> AccessibilityServiceStatus.ENABLED_NOT_RUNNING
            !enabled -> AccessibilityServiceStatus.DISABLED
            else -> AccessibilityServiceStatus.UNKNOWN
        }
    }
    
    /**
     * Get status message for current accessibility service state
     */
    fun getStatusMessage(): String {
        return when (getAccessibilityServiceStatus()) {
            AccessibilityServiceStatus.ACTIVE -> "微信监听服务正常运行"
            AccessibilityServiceStatus.ENABLED_NOT_RUNNING -> "服务已启用但未运行，请重启应用"
            AccessibilityServiceStatus.DISABLED -> "请启用无障碍服务以使用微信录音功能"
            AccessibilityServiceStatus.UNKNOWN -> "无法确定服务状态"
        }
    }
    
    /**
     * Check if the device supports accessibility services
     */
    fun isAccessibilitySupported(): Boolean {
        return try {
            // Try to access accessibility settings - if this fails, accessibility might not be supported
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.resolveActivity(context.packageManager) != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility support", e)
            false
        }
    }
}

/**
 * Instructions for enabling accessibility service
 */
data class AccessibilityInstructions(
    val title: String,
    val steps: List<String>,
    val warning: String,
    val importance: String
)

/**
 * Accessibility service status states
 */
enum class AccessibilityServiceStatus(val displayName: String) {
    ACTIVE("服务活跃"),
    ENABLED_NOT_RUNNING("已启用未运行"),
    DISABLED("未启用"),
    UNKNOWN("未知状态")
}