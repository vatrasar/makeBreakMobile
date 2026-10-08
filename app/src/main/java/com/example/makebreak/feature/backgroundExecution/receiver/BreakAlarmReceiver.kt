package com.example.makebreak.feature.backgroundExecution.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.makebreak.feature.backgroundExecution.manager.TimerEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BreakAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerEngine: TimerEngine

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MakeBreak:AlarmWakeLock"
        )
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
        try {
            timerEngine.onAlarmTriggered()
        } finally {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        }
    }

    companion object {
        const val ACTION_TIMER_ALARM = "com.example.makebreak.ACTION_TIMER_ALARM"
        private const val WAKE_LOCK_TIMEOUT_MS = 5000L
    }
}
