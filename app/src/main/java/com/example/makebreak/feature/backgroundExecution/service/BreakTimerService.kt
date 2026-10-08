package com.example.makebreak.feature.backgroundExecution.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.makebreak.MainActivity
import com.example.makebreak.R
import com.example.makebreak.core.domain.enums.SessionState
import com.example.makebreak.core.domain.model.TimerSession
import com.example.makebreak.feature.backgroundExecution.manager.TimerEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BreakTimerService : Service {

    @Inject
    lateinit var timerEngine: TimerEngine

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateObserverJob: Job? = null

    constructor() : super()

    /**
     * Initializes the service and creates the notification channel.
     * Invoked by: Android system framework upon service creation.
     */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Handles service start commands and begins observing timer state.
     * Invoked by: Android system framework when started via startForegroundService or startService.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = buildNotification(timerEngine.sessionState.value)
        startForeground(NOTIFICATION_ID, initialNotification)
        observeSessionState()
        return START_STICKY
    }

    /**
     * Cleans up coroutine scope and cancels observers when service is destroyed.
     * Invoked by: Android system framework upon service destruction.
     */
    override fun onDestroy() {
        super.onDestroy()
        stateObserverJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeSessionState() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            timerEngine.sessionState.collect { session ->
                handleStateUpdate(session)
            }
        }
    }

    private fun handleStateUpdate(session: TimerSession) {
        if (session.state == SessionState.READY) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val notification = buildNotification(session)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(session: TimerSession): Notification {
        val contentIntent = createContentPendingIntent()
        val title = determineNotificationTitle(session)
        val text = determineNotificationContent(session)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(session.state != SessionState.READY)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createContentPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun determineNotificationTitle(session: TimerSession): String {
        return when (session.state) {
            SessionState.READY -> getString(R.string.status_ready)
            SessionState.WORKING -> getString(R.string.notification_working_title)
            SessionState.BREAK_PENDING -> getString(R.string.notification_break_pending_title)
            SessionState.SHORT_BREAK -> getString(R.string.notification_short_break_title)
            SessionState.LONG_BREAK -> getString(R.string.notification_long_break_title)
            SessionState.BREAK_FINISHED -> getString(R.string.notification_break_finished_title)
            SessionState.PAUSED -> getString(R.string.notification_paused_title)
        }
    }

    private fun determineNotificationContent(session: TimerSession): String {
        if (session.state == SessionState.BREAK_PENDING) {
            return getString(R.string.notification_break_pending_text)
        }
        if (session.state == SessionState.BREAK_FINISHED) {
            return getString(R.string.notification_break_finished_text)
        }
        val minutes = session.remainingSeconds / 60
        val seconds = session.remainingSeconds % 60
        val formattedTime = String.format("%02d:%02d", minutes, seconds)
        return "Remaining: $formattedTime"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val description = getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                this.description = description
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "make_break_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "com.example.makebreak.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.makebreak.ACTION_STOP_SERVICE"
    }
}
