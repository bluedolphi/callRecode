package com.example.callrecode.accessibility

import android.util.Log
import java.util.regex.Pattern

/**
 * Matches patterns in WeChat interface to identify call states and types
 */
class CallPatternMatcher {
    
    companion object {
        private const val TAG = "CallPatternMatcher"
    }
    
    // Compiled patterns for better performance
    private val phoneNumberPattern = Pattern.compile("\\+?\\d[\\d\\s\\-()]{7,}")
    private val timePattern = Pattern.compile("\\d{1,2}:\\d{2}(:\\d{2})?")
    private val durationPattern = Pattern.compile("(\\d+)分(\\d+)秒")
    
    // WeChat-specific patterns
    private val contactNamePattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,8}|[a-zA-Z\\s]{2,20}")
    private val wechatIdPattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{5,19}$")
    
    /**
     * Match contact information from text
     */
    fun matchContactInfo(text: String): ContactMatchResult? {
        try {
            val cleanText = text.trim()
            
            // Try to match phone number
            val phoneMatch = phoneNumberPattern.matcher(cleanText)
            if (phoneMatch.find()) {
                return ContactMatchResult(
                    type = ContactType.PHONE_NUMBER,
                    value = phoneMatch.group(),
                    confidence = 0.9f
                )
            }
            
            // Try to match WeChat ID
            val wechatIdMatch = wechatIdPattern.matcher(cleanText)
            if (wechatIdMatch.find()) {
                return ContactMatchResult(
                    type = ContactType.WECHAT_ID,
                    value = wechatIdMatch.group(),
                    confidence = 0.8f
                )
            }
            
            // Try to match Chinese or English name
            val nameMatch = contactNamePattern.matcher(cleanText)
            if (nameMatch.find() && !isCommonUIText(cleanText)) {
                return ContactMatchResult(
                    type = ContactType.DISPLAY_NAME,
                    value = nameMatch.group(),
                    confidence = 0.7f
                )
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error matching contact info: $text", e)
            return null
        }
    }
    
    /**
     * Match call duration from text
     */
    fun matchCallDuration(text: String): CallDurationMatch? {
        try {
            // Match MM:SS format
            val timeMatch = timePattern.matcher(text)
            if (timeMatch.find()) {
                val timeStr = timeMatch.group()
                val parts = timeStr.split(":")
                
                if (parts.size >= 2) {
                    val minutes = parts[0].toIntOrNull() ?: 0
                    val seconds = parts[1].toIntOrNull() ?: 0
                    val totalSeconds = minutes * 60 + seconds
                    
                    return CallDurationMatch(
                        durationSeconds = totalSeconds,
                        formattedDuration = timeStr,
                        confidence = 0.9f
                    )
                }
            }
            
            // Match Chinese duration format (X分Y秒)
            val durationMatch = durationPattern.matcher(text)
            if (durationMatch.find()) {
                val minutes = durationMatch.group(1)?.toIntOrNull() ?: 0
                val seconds = durationMatch.group(2)?.toIntOrNull() ?: 0
                val totalSeconds = minutes * 60 + seconds
                
                return CallDurationMatch(
                    durationSeconds = totalSeconds,
                    formattedDuration = durationMatch.group(),
                    confidence = 0.8f
                )
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error matching call duration: $text", e)
            return null
        }
    }
    
    /**
     * Match call status indicators
     */
    fun matchCallStatus(text: String): CallStatusMatch? {
        val lowerText = text.lowercase()
        
        return when {
            lowerText.contains("正在连接") || lowerText.contains("connecting") -> {
                CallStatusMatch(WeChatCallState.CALL_CONNECTING, 0.9f)
            }
            lowerText.contains("正在通话") || lowerText.contains("通话中") -> {
                CallStatusMatch(WeChatCallState.CALL_STARTED, 0.9f)
            }
            lowerText.contains("通话结束") || lowerText.contains("已结束") -> {
                CallStatusMatch(WeChatCallState.CALL_ENDED, 0.9f)
            }
            lowerText.contains("等待对方接受") || lowerText.contains("拨号中") -> {
                CallStatusMatch(WeChatCallState.CALL_CONNECTING, 0.8f)
            }
            lowerText.contains("对方忙") || lowerText.contains("无人接听") -> {
                CallStatusMatch(WeChatCallState.CALL_ENDED, 0.8f)
            }
            else -> null
        }
    }
    
    /**
     * Match call type indicators
     */
    fun matchCallType(text: String): CallTypeMatch? {
        val lowerText = text.lowercase()
        
        return when {
            lowerText.contains("视频通话") || lowerText.contains("video call") -> {
                CallTypeMatch(WeChatCallType.VIDEO_CALL, 0.9f)
            }
            lowerText.contains("语音通话") || lowerText.contains("voice call") -> {
                CallTypeMatch(WeChatCallType.VOICE_CALL, 0.9f)
            }
            lowerText.contains("群通话") || lowerText.contains("group call") -> {
                CallTypeMatch(WeChatCallType.GROUP_CALL, 0.8f)
            }
            lowerText.contains("摄像头") || lowerText.contains("camera") -> {
                CallTypeMatch(WeChatCallType.VIDEO_CALL, 0.7f)
            }
            lowerText.contains("免提") || lowerText.contains("扬声器") -> {
                CallTypeMatch(WeChatCallType.VOICE_CALL, 0.6f)
            }
            else -> null
        }
    }
    
    /**
     * Check if text is a common UI element (not a contact name)
     */
    private fun isCommonUIText(text: String): Boolean {
        val commonTexts = setOf(
            "接听", "挂断", "拒接", "免提", "静音", "扬声器", "切换", "返回",
            "最小化", "摄像头", "视频", "语音", "更多", "设置", "微信", "通话",
            "正在连接", "正在通话", "通话中", "通话结束", "拨号中"
        )
        
        return commonTexts.any { commonText -> 
            text.contains(commonText, ignoreCase = true) 
        }
    }
    
    /**
     * Analyze text patterns to extract all relevant information
     */
    fun analyzeTextPatterns(allText: String): PatternAnalysisResult {
        val result = PatternAnalysisResult()
        
        // Split text into meaningful chunks
        val textChunks = allText.split("\\s+".toRegex())
            .filter { it.trim().isNotEmpty() }
        
        for (chunk in textChunks) {
            // Try to match different patterns
            matchContactInfo(chunk)?.let { result.contactMatches.add(it) }
            matchCallDuration(chunk)?.let { result.durationMatch = it }
            matchCallStatus(chunk)?.let { result.statusMatch = it }
            matchCallType(chunk)?.let { result.typeMatch = it }
        }
        
        // Also analyze the full text for multi-word patterns
        matchCallStatus(allText)?.let { result.statusMatch = it }
        matchCallType(allText)?.let { result.typeMatch = it }
        
        return result
    }
}

/**
 * Contact matching result
 */
data class ContactMatchResult(
    val type: ContactType,
    val value: String,
    val confidence: Float
)

/**
 * Call duration matching result
 */
data class CallDurationMatch(
    val durationSeconds: Int,
    val formattedDuration: String,
    val confidence: Float
)

/**
 * Call status matching result
 */
data class CallStatusMatch(
    val status: WeChatCallState,
    val confidence: Float
)

/**
 * Call type matching result
 */
data class CallTypeMatch(
    val callType: WeChatCallType,
    val confidence: Float
)

/**
 * Complete pattern analysis result
 */
data class PatternAnalysisResult(
    val contactMatches: MutableList<ContactMatchResult> = mutableListOf(),
    var durationMatch: CallDurationMatch? = null,
    var statusMatch: CallStatusMatch? = null,
    var typeMatch: CallTypeMatch? = null
)

/**
 * Types of contact information
 */
enum class ContactType {
    PHONE_NUMBER,
    WECHAT_ID,
    DISPLAY_NAME
}