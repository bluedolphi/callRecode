package com.example.callrecode.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.callrecode.MainActivity
import com.example.callrecode.R
import com.example.callrecode.recording.CallRecordingPhoneStateListener
import com.example.callrecode.recording.RecordingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Foreground service for call recording functionality
 * Monitors phone state and manages recording sessions
 */
class CallRecordingService : Service() {
    
    companion object {
        private const val TAG = "CallRecordingService"
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "call_recording_channel"
        private const val CHANNEL_NAME = "通话录音服务"
        
        const val ACTION_START_SERVICE = "com.example.callrecode.START_RECORDING_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.callrecode.STOP_RECORDING_SERVICE"
        const val ACTION_START_RECORDING = "com.example.callrecode.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.callrecode.STOP_RECORDING"
        
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_IS_MANUAL = "is_manual"
        
        fun startService(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
        
        fun startRecording(context: Context, phoneNumber: String?, isManual: Boolean = false) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_IS_MANUAL, isManual)
            }
            context.startService(intent)
        }
        
        fun stopRecording(context: Context) {
            val intent = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }
    
    private lateinit var recordingEngine: RecordingEngine
    private lateinit var phoneStateListener: CallRecordingPhoneStateListener
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var notificationManager: NotificationManager
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var recordingStatusJob: Job? = null
    
    private var isServiceRunning = false
    private var isRecordingActive = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallRecordingService created")
        
        // Initialize components
        recordingEngine = RecordingEngine(this)
        phoneStateListener = CallRecordingPhoneStateListener(this, recordingEngine)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel
        createNotificationChannel()
        
        // Start monitoring recording status
        startRecordingStatusMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startRecordingService()
            }
            ACTION_STOP_SERVICE -> {
                stopRecordingService()
            }
            ACTION_START_RECORDING -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                val isManual = intent.getBooleanExtra(EXTRA_IS_MANUAL, false)
                startRecording(phoneNumber, isManual)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }
        
        return START_STICKY // Restart service if killed by system
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // This is a started service, not a bound service
    }
    
    override fun onDestroy() {
        Log.d(TAG, "CallRecordingService destroyed")
        
        stopRecordingService()
        serviceScope.cancel()
        recordingEngine.release()
        
        super.onDestroy()
    }
    
    private fun startRecordingService() {
        if (isServiceRunning) {
            Log.d(TAG, "Recording service already running")
            return
        }
        
        Log.i(TAG, "Starting call recording service")
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createServiceNotification("录音服务已启动", false))
        
        // Register phone state listener
        try {
            telephonyManager.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "Phone state listener registered")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to register phone state listener - permission denied", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register phone state listener", e)
        }
        
        isServiceRunning = true
    }
    
    private fun stopRecordingService() {
        if (!isServiceRunning) return
        
        Log.i(TAG, "Stopping call recording service")
        
        // Stop any active recording
        if (isRecordingActive) {
            stopRecording()
        }
        
        // Unregister phone state listener
        try {
            telephonyManager.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_NONE)
            Log.d(TAG, "Phone state listener unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering phone state listener", e)
        }
        
        // Stop monitoring
        recordingStatusJob?.cancel()
        
        isServiceRunning = false
        stopSelf()
    }
    
    private fun startRecording(phoneNumber: String?, isManual: Boolean) {
        if (isRecordingActive) {
            Log.w(TAG, "Recording already active")
            return
        }
        
        Log.d(TAG, "Starting recording: phone=$phoneNumber, manual=$isManual")
        
        serviceScope.let { scope ->
            kotlinx.coroutines.launch(Dispatchers.IO) {
                val result = recordingEngine.startRecording(phoneNumber, isManual = isManual)
                
                result.onSuccess { session ->
                    Log.i(TAG, "Recording started successfully: ${session.id}")
                    isRecordingActive = true
                    
                    // Update notification
                    kotlinx.coroutines.launch(Dispatchers.Main) {
                        updateNotification("正在录音: ${phoneNumber ?: "未知号码"}", true)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to start recording", error)
                    
                    // Update notification with error
                    kotlinx.coroutines.launch(Dispatchers.Main) {
                        updateNotification("录音启动失败", false)
                    }
                }
            }
        }
    }
    
    private fun stopRecording() {
        if (!isRecordingActive) {
            Log.w(TAG, "No active recording to stop")
            return
        }
        
        Log.d(TAG, "Stopping recording")
        
        serviceScope.let { scope ->
            kotlinx.coroutines.launch(Dispatchers.IO) {
                val result = recordingEngine.stopRecording()
                
                result.onSuccess { session ->
                    Log.i(TAG, "Recording stopped successfully: ${session.id}, duration: ${session.duration}ms")
                    isRecordingActive = false
                    
                    // Update notification
                    kotlinx.coroutines.launch(Dispatchers.Main) {
                        updateNotification("录音服务已启动", false)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to stop recording", error)
                    isRecordingActive = false
                    
                    // Update notification
                    kotlinx.coroutines.launch(Dispatchers.Main) {
                        updateNotification("录音停止失败", false)
                    }
                }
            }
        }
    }
    
    private fun startRecordingStatusMonitoring() {
        recordingStatusJob = recordingEngine.isRecording
            .onEach { isRecording ->
                isRecordingActive = isRecording
                Log.d(TAG, "Recording status changed: $isRecording")
            }
            .launchIn(serviceScope)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "通话录音后台服务通知"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createServiceNotification(message: String, isRecording: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val iconRes = if (isRecording) {
            android.R.drawable.ic_btn_speak_now // Recording icon
        } else {
            android.R.drawable.ic_menu_call // Service icon
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("智能通话录音")
            .setContentText(message)
            .setSmallIcon(iconRes)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    private fun updateNotification(message: String, isRecording: Boolean) {
        if (isServiceRunning) {
            val notification = createServiceNotification(message, isRecording)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}