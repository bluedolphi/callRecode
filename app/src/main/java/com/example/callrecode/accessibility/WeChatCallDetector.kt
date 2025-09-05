package com.example.callrecode.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.regex.Pattern

/**
 * Detects WeChat call interfaces and states through accessibility events
 */
class WeChatCallDetector(private val accessibilityService: AccessibilityService) {
    
    companion object {
        private const val TAG = "WeChatCallDetector"
        
        // WeChat call-related UI text patterns
        private val VOICE_CALL_PATTERNS = listOf(
            "语音通话", "voice call", "正在通话", "通话中",
            "接听", "挂断", "拒接", "免提"
        )
        
        private val VIDEO_CALL_PATTERNS = listOf(
            "视频通话", "video call", "视频", "摄像头",
            "切换摄像头", "开启摄像头", "关闭摄像头"
        )
        
        private val CALL_CONNECTING_PATTERNS = listOf(
            "正在连接", "connecting", "拨号中", "等待对方接受",
            "邀请", "waiting", "ringing"
        )
        
        private val CALL_ENDED_PATTERNS = listOf(
            "通话结束", "call ended", "已挂断", "通话时长",
            "重新拨打", "再次拨打"
        )
        
        // WeChat Activity class names that indicate call interface
        private val WECHAT_CALL_ACTIVITIES = setOf(
            "com.tencent.mm.plugin.voip.ui.VideoActivity",
            "com.tencent.mm.plugin.voip.ui.VoipActivity", 
            "com.tencent.mm.plugin.voip.ui.VideoCallActivity",
            "com.tencent.mm.plugin.voip.ui.VoiceCallActivity",
            "com.tencent.mm.plugin.voip.ui.VideoCallUI",
            "com.tencent.mm.ui.chatting.gallery.VideoCallUI"
        )
    }
    
    private val uiAnalyzer = WeChatUIAnalyzer()
    private val patternMatcher = CallPatternMatcher()
    
    /**
     * Detect WeChat call interface from accessibility event
     */
    fun detectCallInterface(rootNode: AccessibilityNodeInfo, event: AccessibilityEvent): WeChatCallDetection? {
        try {
            Log.d(TAG, "Detecting call interface, event type: ${event.eventType}")
            
            // First check if we're in a known WeChat call activity
            val activityName = event.className?.toString()
            val isInCallActivity = isWeChatCallActivity(activityName)
            
            if (isInCallActivity) {
                Log.d(TAG, "Detected WeChat call activity: $activityName")
            }
            
            // Analyze UI structure
            val uiAnalysis = uiAnalyzer.analyzeWeChatInterface(rootNode)
            
            // Detect call type and state
            val callType = detectCallType(rootNode, uiAnalysis)
            val callState = detectCallState(rootNode, uiAnalysis, event)
            
            if (callType != WeChatCallType.UNKNOWN || isInCallActivity) {
                val contactName = extractContactName(rootNode, uiAnalysis)
                
                return WeChatCallDetection(
                    callType = callType,
                    callState = callState,
                    contactName = contactName,
                    isInCallActivity = isInCallActivity,
                    confidence = calculateConfidence(callType, callState, isInCallActivity, uiAnalysis)
                )
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting WeChat call interface", e)
            return null
        }
    }
    
    /**
     * Check if the current activity is a WeChat call activity
     */
    private fun isWeChatCallActivity(activityName: String?): Boolean {
        if (activityName == null) return false
        
        return WECHAT_CALL_ACTIVITIES.any { pattern ->
            activityName.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Detect WeChat call type (voice/video)
     */
    private fun detectCallType(rootNode: AccessibilityNodeInfo, uiAnalysis: WeChatUIAnalysis): WeChatCallType {
        val allText = uiAnalysis.allVisibleText.lowercase()
        
        // Check for video call indicators
        if (VIDEO_CALL_PATTERNS.any { pattern -> 
            allText.contains(pattern.lowercase()) 
        }) {
            Log.d(TAG, "Detected video call indicators")
            return WeChatCallType.VIDEO_CALL
        }
        
        // Check for voice call indicators
        if (VOICE_CALL_PATTERNS.any { pattern -> 
            allText.contains(pattern.lowercase()) 
        }) {
            Log.d(TAG, "Detected voice call indicators")
            return WeChatCallType.VOICE_CALL
        }
        
        // Check UI structure for call-specific elements
        if (uiAnalysis.hasCallButtons) {
            Log.d(TAG, "Detected call buttons, assuming voice call")
            return WeChatCallType.VOICE_CALL
        }
        
        return WeChatCallType.UNKNOWN
    }
    
    /**
     * Detect current call state
     */
    private fun detectCallState(
        rootNode: AccessibilityNodeInfo, 
        uiAnalysis: WeChatUIAnalysis,
        event: AccessibilityEvent
    ): WeChatCallState {
        val allText = uiAnalysis.allVisibleText.lowercase()
        
        // Check for call ended state
        if (CALL_ENDED_PATTERNS.any { pattern -> 
            allText.contains(pattern.lowercase()) 
        }) {
            Log.d(TAG, "Detected call ended state")
            return WeChatCallState.CALL_ENDED
        }
        
        // Check for connecting state
        if (CALL_CONNECTING_PATTERNS.any { pattern -> 
            allText.contains(pattern.lowercase()) 
        }) {
            Log.d(TAG, "Detected call connecting state")
            return WeChatCallState.CALL_CONNECTING
        }
        
        // Check for active call indicators
        if (allText.contains("正在通话") || 
            allText.contains("通话中") ||
            uiAnalysis.hasActiveCallIndicators) {
            Log.d(TAG, "Detected active call state")
            return WeChatCallState.CALL_STARTED
        }
        
        // Default to connecting if we're in a call activity but no clear state
        if (uiAnalysis.hasCallButtons || uiAnalysis.isInCallInterface) {
            Log.d(TAG, "In call interface but unclear state, defaulting to connecting")
            return WeChatCallState.CALL_CONNECTING
        }
        
        return WeChatCallState.IDLE
    }
    
    /**
     * Extract contact name from call interface
     */
    private fun extractContactName(rootNode: AccessibilityNodeInfo, uiAnalysis: WeChatUIAnalysis): String? {
        // Try to find contact name in prominent text elements
        val candidates = uiAnalysis.prominentTexts
        
        for (text in candidates) {
            // Skip common call interface texts
            if (isCallInterfaceText(text)) continue
            
            // If text looks like a name or phone number
            if (text.length > 1 && text.length < 30) {
                Log.d(TAG, "Found potential contact name: $text")
                return text
            }
        }
        
        return null
    }
    
    /**
     * Calculate confidence score for detection
     */
    private fun calculateConfidence(
        callType: WeChatCallType,
        callState: WeChatCallState,
        isInCallActivity: Boolean,
        uiAnalysis: WeChatUIAnalysis
    ): Float {
        var confidence = 0f
        
        // Base confidence from call type detection
        if (callType != WeChatCallType.UNKNOWN) confidence += 0.3f
        
        // Confidence from call state detection
        if (callState != WeChatCallState.IDLE) confidence += 0.2f
        
        // Strong indicator if in known call activity
        if (isInCallActivity) confidence += 0.4f
        
        // UI analysis indicators
        if (uiAnalysis.hasCallButtons) confidence += 0.1f
        if (uiAnalysis.hasActiveCallIndicators) confidence += 0.2f
        if (uiAnalysis.isInCallInterface) confidence += 0.2f
        
        return confidence.coerceAtMost(1.0f)
    }
    
    /**
     * Check if text is a common call interface element
     */
    private fun isCallInterfaceText(text: String): Boolean {
        val lowerText = text.lowercase()
        
        val commonTexts = listOf(
            "接听", "挂断", "拒接", "免提", "静音", "扬声器",
            "切换", "摄像头", "视频", "语音", "返回", "最小化"
        )
        
        return commonTexts.any { commonText ->
            lowerText.contains(commonText)
        }
    }
}

/**
 * WeChat call detection result
 */
data class WeChatCallDetection(
    val callType: WeChatCallType,
    val callState: WeChatCallState,
    val contactName: String?,
    val isInCallActivity: Boolean,
    val confidence: Float
)