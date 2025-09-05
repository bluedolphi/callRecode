package com.example.callrecode.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Analyzes WeChat UI structure to identify call-related elements
 */
class WeChatUIAnalyzer {
    
    companion object {
        private const val TAG = "WeChatUIAnalyzer"
        private const val MAX_ANALYSIS_DEPTH = 10
    }
    
    /**
     * Analyze WeChat interface structure
     */
    fun analyzeWeChatInterface(rootNode: AccessibilityNodeInfo): WeChatUIAnalysis {
        val analysis = WeChatUIAnalysis()
        
        try {
            // Traverse UI tree and collect information
            analyzeNodeRecursively(rootNode, analysis, 0)
            
            // Post-process analysis results
            finalizeAnalysis(analysis)
            
            Log.d(TAG, "UI Analysis complete - visible texts: ${analysis.allVisibleText.length}, " +
                      "buttons: ${analysis.buttonTexts.size}, " +
                      "call interface: ${analysis.isInCallInterface}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during UI analysis", e)
        }
        
        return analysis
    }
    
    /**
     * Recursively analyze UI nodes
     */
    private fun analyzeNodeRecursively(
        node: AccessibilityNodeInfo?, 
        analysis: WeChatUIAnalysis, 
        depth: Int
    ) {
        if (node == null || depth > MAX_ANALYSIS_DEPTH) return
        
        try {
            // Extract text content
            val nodeText = node.text?.toString()?.trim()
            val nodeDescription = node.contentDescription?.toString()?.trim()
            val nodeClassName = node.className?.toString()
            
            // Collect visible text
            if (!nodeText.isNullOrEmpty()) {
                analysis.allVisibleText.append(nodeText).append(" ")
                
                // Check if this is prominent text (large or important)
                if (isProminentText(node, nodeText)) {
                    analysis.prominentTexts.add(nodeText)
                }
            }
            
            if (!nodeDescription.isNullOrEmpty()) {
                analysis.allVisibleText.append(nodeDescription).append(" ")
            }
            
            // Analyze specific node types
            when {
                isButton(node, nodeClassName) -> {
                    analyzeButton(node, nodeText, nodeDescription, analysis)
                }
                isImageButton(node, nodeClassName) -> {
                    analyzeImageButton(node, nodeDescription, analysis)
                }
                isTextView(node, nodeClassName) -> {
                    analyzeTextView(node, nodeText, analysis)
                }
            }
            
            // Check for call-specific indicators
            checkCallIndicators(node, nodeText, nodeDescription, analysis)
            
            // Recursively analyze child nodes
            val childCount = node.childCount
            for (i in 0 until childCount) {
                val childNode = node.getChild(i)
                analyzeNodeRecursively(childNode, analysis, depth + 1)
                childNode?.recycle()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing node at depth $depth", e)
        }
    }
    
    /**
     * Check if node represents a button
     */
    private fun isButton(node: AccessibilityNodeInfo, className: String?): Boolean {
        return className?.contains("Button") == true || 
               node.isClickable
    }
    
    /**
     * Check if node is an ImageButton
     */
    private fun isImageButton(node: AccessibilityNodeInfo, className: String?): Boolean {
        return className?.contains("ImageButton") == true ||
               className?.contains("ImageView") == true && node.isClickable
    }
    
    /**
     * Check if node is a TextView
     */
    private fun isTextView(node: AccessibilityNodeInfo, className: String?): Boolean {
        return className?.contains("TextView") == true ||
               className?.contains("Text") == true
    }
    
    /**
     * Check if text is prominent (likely to be contact name or important info)
     */
    private fun isProminentText(node: AccessibilityNodeInfo, text: String): Boolean {
        // Text that's longer than common button text but not too long
        if (text.length < 2 || text.length > 30) return false
        
        // Skip common button/interface texts
        val commonInterfaceTexts = setOf(
            "接听", "挂断", "拒接", "免提", "静音", "扬声器", "切换", "返回",
            "最小化", "摄像头", "视频", "语音", "更多", "设置"
        )
        
        return !commonInterfaceTexts.contains(text)
    }
    
    /**
     * Analyze button elements
     */
    private fun analyzeButton(
        node: AccessibilityNodeInfo, 
        text: String?, 
        description: String?,
        analysis: WeChatUIAnalysis
    ) {
        val buttonText = text ?: description ?: ""
        if (buttonText.isNotEmpty()) {
            analysis.buttonTexts.add(buttonText)
            
            // Check for call-specific buttons
            val lowerText = buttonText.lowercase()
            when {
                lowerText.contains("接听") || lowerText.contains("answer") -> {
                    analysis.hasAnswerButton = true
                    analysis.hasCallButtons = true
                }
                lowerText.contains("挂断") || lowerText.contains("hang") -> {
                    analysis.hasHangupButton = true
                    analysis.hasCallButtons = true
                }
                lowerText.contains("拒接") || lowerText.contains("decline") -> {
                    analysis.hasDeclineButton = true
                    analysis.hasCallButtons = true
                }
                lowerText.contains("免提") || lowerText.contains("speaker") -> {
                    analysis.hasSpeakerButton = true
                    analysis.hasCallButtons = true
                }
                lowerText.contains("静音") || lowerText.contains("mute") -> {
                    analysis.hasMuteButton = true
                    analysis.hasCallButtons = true
                }
            }
        }
    }
    
    /**
     * Analyze image button elements
     */
    private fun analyzeImageButton(
        node: AccessibilityNodeInfo,
        description: String?,
        analysis: WeChatUIAnalysis
    ) {
        if (description != null) {
            analysis.buttonTexts.add(description)
            
            val lowerDesc = description.lowercase()
            if (lowerDesc.contains("通话") || lowerDesc.contains("call") ||
                lowerDesc.contains("接听") || lowerDesc.contains("挂断")) {
                analysis.hasCallButtons = true
            }
        }
    }
    
    /**
     * Analyze text view elements
     */
    private fun analyzeTextView(
        node: AccessibilityNodeInfo,
        text: String?,
        analysis: WeChatUIAnalysis
    ) {
        if (text != null && text.isNotEmpty()) {
            val lowerText = text.lowercase()
            
            // Check for call duration or time indicators
            if (lowerText.matches(Regex("\\d{2}:\\d{2}")) || 
                lowerText.matches(Regex("\\d{1,2}分\\d{1,2}秒"))) {
                analysis.hasCallDuration = true
                analysis.hasActiveCallIndicators = true
            }
            
            // Check for "正在通话" type indicators
            if (lowerText.contains("正在") || lowerText.contains("通话中")) {
                analysis.hasActiveCallIndicators = true
            }
        }
    }
    
    /**
     * Check for general call indicators
     */
    private fun checkCallIndicators(
        node: AccessibilityNodeInfo,
        text: String?,
        description: String?,
        analysis: WeChatUIAnalysis
    ) {
        val combinedText = "${text ?: ""} ${description ?: ""}".lowercase()
        
        // Check for call-related keywords
        val callKeywords = listOf(
            "通话", "call", "语音", "voice", "视频", "video",
            "正在连接", "connecting", "等待", "waiting"
        )
        
        if (callKeywords.any { keyword -> combinedText.contains(keyword) }) {
            analysis.isInCallInterface = true
        }
    }
    
    /**
     * Finalize analysis results
     */
    private fun finalizeAnalysis(analysis: WeChatUIAnalysis) {
        // Determine if we're in a call interface based on collected evidence
        if (analysis.hasCallButtons || 
            analysis.hasActiveCallIndicators || 
            analysis.hasCallDuration) {
            analysis.isInCallInterface = true
        }
        
        // Clean up text
        analysis.allVisibleText = StringBuilder(analysis.allVisibleText.toString().trim())
    }
}

/**
 * Results of WeChat UI analysis
 */
data class WeChatUIAnalysis(
    var allVisibleText: StringBuilder = StringBuilder(),
    val prominentTexts: MutableList<String> = mutableListOf(),
    val buttonTexts: MutableList<String> = mutableListOf(),
    
    // Call interface indicators
    var isInCallInterface: Boolean = false,
    var hasCallButtons: Boolean = false,
    var hasActiveCallIndicators: Boolean = false,
    
    // Specific button types
    var hasAnswerButton: Boolean = false,
    var hasHangupButton: Boolean = false,
    var hasDeclineButton: Boolean = false,
    var hasSpeakerButton: Boolean = false,
    var hasMuteButton: Boolean = false,
    var hasCallDuration: Boolean = false
)