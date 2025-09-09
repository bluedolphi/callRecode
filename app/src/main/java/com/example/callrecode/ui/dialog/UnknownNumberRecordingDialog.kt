package com.example.callrecode.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.example.callrecode.R
import com.example.callrecode.recording.UnknownNumberManager

/**
 * Dialog to prompt user for recording unknown numbers
 * Auto-dismisses after 10 seconds if no action taken
 */
class UnknownNumberRecordingDialog(
    private val context: Context,
    private val phoneNumber: String,
    private val onConfirm: () -> Unit,
    private val onDeny: () -> Unit,
    private val onRemember: (shouldRemember: Boolean) -> Unit
) {
    
    companion object {
        private const val AUTO_DISMISS_SECONDS = 10L
        private const val TAG = "UnknownNumberDialog"
    }
    
    private var dialog: AlertDialog? = null
    private var countDownTimer: CountDownTimer? = null
    private var rememberCheckbox: CheckBox? = null
    
    fun show() {
        if (dialog?.isShowing == true) {
            return // Dialog already showing
        }
        
        val dialogView = LayoutInflater.from(context).inflate(
            R.layout.dialog_unknown_number_recording, null
        )
        
        // Set up views
        setupViews(dialogView)
        
        // Create dialog
        dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        // Start countdown timer
        startCountdownTimer()
        
        dialog?.show()
    }
    
    fun dismiss() {
        countDownTimer?.cancel()
        dialog?.dismiss()
    }
    
    private fun setupViews(dialogView: android.view.View) {
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val messageText = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val countdownText = dialogView.findViewById<TextView>(R.id.countdownText)
        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.confirmButton)
        val denyButton = dialogView.findViewById<MaterialButton>(R.id.denyButton)
        rememberCheckbox = dialogView.findViewById<CheckBox>(R.id.rememberCheckbox)
        
        // Set texts
        titleText.text = "陌生号码录音确认"
        messageText.text = "陌生号码 $phoneNumber 正在呼入，是否录音？"
        countdownText.text = "将在 $AUTO_DISMISS_SECONDS 秒后自动拒绝"
        
        // Set button actions
        confirmButton.setOnClickListener {
            handleConfirm()
        }
        
        denyButton.setOnClickListener {
            handleDeny()
        }
    }
    
    private fun startCountdownTimer() {
        countDownTimer = object : CountDownTimer(AUTO_DISMISS_SECONDS * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                dialog?.findViewById<TextView>(R.id.countdownText)?.text = 
                    "将在 $secondsLeft 秒后自动拒绝"
            }
            
            override fun onFinish() {
                // Auto-deny if no action taken
                handleDeny()
            }
        }
        
        countDownTimer?.start()
    }
    
    private fun handleConfirm() {
        val shouldRemember = rememberCheckbox?.isChecked == true
        
        // Save decision if requested
        if (shouldRemember) {
            val unknownNumberManager = UnknownNumberManager(context)
            unknownNumberManager.rememberDecision(phoneNumber, allow = true, remember = true)
        }
        
        onConfirm()
        onRemember(shouldRemember)
        
        dismiss()
    }
    
    private fun handleDeny() {
        val shouldRemember = rememberCheckbox?.isChecked == true
        
        // Save decision if requested
        if (shouldRemember) {
            val unknownNumberManager = UnknownNumberManager(context)
            unknownNumberManager.rememberDecision(phoneNumber, allow = false, remember = true)
        }
        
        onDeny()
        onRemember(shouldRemember)
        
        dismiss()
    }
}