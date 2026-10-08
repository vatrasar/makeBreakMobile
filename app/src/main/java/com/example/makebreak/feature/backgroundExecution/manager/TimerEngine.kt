package com.example.makebreak.feature.backgroundExecution.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.makebreak.core.domain.enums.NextBreakType
import com.example.makebreak.core.domain.enums.SessionState
import com.example.makebreak.core.domain.model.TimerSession
import com.example.makebreak.core.domain.model.UserSettings
import com.example.makebreak.core.domain.repository.UserSettingsRepository
import com.example.makebreak.core.domain.service.AudioAnnouncementPlayer
import com.example.makebreak.feature.backgroundExecution.receiver.BreakAlarmReceiver
import com.example.makebreak.feature.backgroundExecution.service.BreakTimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsRepository: UserSettingsRepository,
    private val audioAnnouncementPlayer: AudioAnnouncementPlayer
) {

    private val engineScope = CoroutineScope(Dispatchers.Main + Job())
    private val _sessionState = MutableStateFlow(TimerSession())
    val sessionState: StateFlow<TimerSession> = _sessionState.asStateFlow()

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private var currentSettings: UserSettings = UserSettings()
    private var workedSecondsInCycle: Int = 0
    private var previousStateBeforePause: SessionState = SessionState.WORKING
    private var pendingBreakState: SessionState = SessionState.SHORT_BREAK
    private var pendingBreakDuration: Int = 0
    private var tickerJob: Job? = null
    private var pendingVoiceJob: Job? = null

    init {
        observeSettings()
    }

    /**
     * Starts a new work session countdown and activates background foreground service.
     * Invoked by: BreakTimerViewModel.
     */
    fun startSession() {
        startTimerService()
        workedSecondsInCycle = 0
        beginWorkingPhase()
    }

    /**
     * Starts the pending break countdown after user confirmation.
     * Invoked by: BreakTimerViewModel.
     */
    fun startBreak() {
        if (_sessionState.value.state != SessionState.BREAK_PENDING) {
            return
        }
        cancelPendingVoiceJob()
        cancelAlarm()
        audioAnnouncementPlayer.stop()
        beginBreakPhase(pendingBreakState, pendingBreakDuration)
    }

    /**
     * Pauses the currently running countdown timer.
     * Invoked by: BreakTimerViewModel.
     */
    fun pauseSession() {
        cancelTicker()
        cancelPendingVoiceJob()
        cancelAlarm()
        audioAnnouncementPlayer.stop()
        previousStateBeforePause = _sessionState.value.state
        _sessionState.value = _sessionState.value.copy(
            state = SessionState.PAUSED,
            isPaused = true
        )
    }

    /**
     * Resumes the paused countdown timer.
     * Invoked by: BreakTimerViewModel.
     */
    fun resumeSession() {
        if (_sessionState.value.state != SessionState.PAUSED) {
            return
        }
        val restoredState = previousStateBeforePause
        _sessionState.value = _sessionState.value.copy(
            state = restoredState,
            isPaused = false
        )
        if (restoredState == SessionState.BREAK_PENDING) {
            startPendingVoiceLoop()
            cancelAlarm()
            scheduleAlarm(System.currentTimeMillis() + VOICE_REPEAT_INTERVAL_MS)
        } else {
            val remaining = _sessionState.value.remainingSeconds
            startTicker()
            scheduleAlarm(System.currentTimeMillis() + remaining * 1000L)
        }
    }

    /**
     * Resets the entire session back to ready state and cancels all timers and alarms.
     * Invoked by: BreakTimerViewModel.
     */
    fun resetSession() {
        cancelTicker()
        cancelPendingVoiceJob()
        cancelAlarm()
        audioAnnouncementPlayer.stop()
        workedSecondsInCycle = 0
        _sessionState.value = TimerSession(state = SessionState.READY)
        stopTimerService()
    }

    /**
     * Skips the current break and immediately transitions back into working session.
     * Invoked by: BreakTimerViewModel.
     */
    fun skipBreak() {
        if (_sessionState.value.state != SessionState.SHORT_BREAK &&
            _sessionState.value.state != SessionState.LONG_BREAK &&
            _sessionState.value.state != SessionState.BREAK_PENDING &&
            _sessionState.value.state != SessionState.BREAK_FINISHED
        ) {
            return
        }
        cancelTicker()
        cancelPendingVoiceJob()
        cancelAlarm()
        audioAnnouncementPlayer.stop()
        beginWorkingPhase()
    }

    /**
     * Completes a finished break and resumes the work countdown.
     * Invoked by: BreakTimerViewModel.
     */
    fun finishBreak() {
        if (_sessionState.value.state != SessionState.BREAK_FINISHED) {
            return
        }
        cancelTicker()
        cancelPendingVoiceJob()
        cancelAlarm()
        audioAnnouncementPlayer.stop()
        beginWorkingPhase()
    }

    /**
     * Handles exact alarm trigger from system alarm receiver.
     * Invoked by: BreakAlarmReceiver.
     */
    fun onAlarmTriggered() {
        if (_sessionState.value.state == SessionState.BREAK_PENDING) {
            cancelPendingVoiceJob()
            playBreakAnnouncement()
            startPendingVoiceLoop()
            cancelAlarm()
            scheduleAlarm(System.currentTimeMillis() + VOICE_REPEAT_INTERVAL_MS)
            return
        }
        handlePhaseCompleted()
    }

    private fun observeSettings() {
        engineScope.launch {
            userSettingsRepository.getUserSettingsFlow().collect { settings ->
                currentSettings = settings
            }
        }
    }

    private fun beginWorkingPhase() {
        cancelTicker()
        cancelAlarm()

        val shortInterval = currentSettings.shortBreakIntervalMinutes * 60
        val longInterval = currentSettings.longBreakIntervalMinutes * 60

        if (workedSecondsInCycle >= longInterval) {
            workedSecondsInCycle = 0
        }

        val timeUntilShort = shortInterval - (workedSecondsInCycle % shortInterval)
        val timeUntilLong = (longInterval - workedSecondsInCycle).coerceAtLeast(0)

        val nextBreakType = if (timeUntilLong <= timeUntilShort) {
            NextBreakType.LONG
        } else {
            NextBreakType.SHORT
        }

        val phaseSeconds = minOf(timeUntilShort, timeUntilLong).coerceAtLeast(1)

        _sessionState.value = TimerSession(
            state = SessionState.WORKING,
            remainingSeconds = phaseSeconds,
            totalPhaseSeconds = phaseSeconds,
            nextBreakType = nextBreakType,
            secondsUntilShortBreak = timeUntilShort,
            secondsUntilLongBreak = timeUntilLong,
            isPaused = false
        )

        startTicker()
        scheduleAlarm(System.currentTimeMillis() + phaseSeconds * 1000L)
    }

    private fun beginBreakPendingPhase(breakState: SessionState, durationSeconds: Int) {
        cancelTicker()
        cancelAlarm()
        cancelPendingVoiceJob()

        pendingBreakState = breakState
        pendingBreakDuration = durationSeconds

        _sessionState.value = TimerSession(
            state = SessionState.BREAK_PENDING,
            remainingSeconds = durationSeconds,
            totalPhaseSeconds = durationSeconds,
            nextBreakType = if (breakState == SessionState.LONG_BREAK) NextBreakType.LONG else NextBreakType.SHORT,
            secondsUntilShortBreak = 0,
            secondsUntilLongBreak = 0,
            isPaused = false
        )

        playBreakAnnouncement()
        startPendingVoiceLoop()
        scheduleAlarm(System.currentTimeMillis() + VOICE_REPEAT_INTERVAL_MS)
    }

    private fun beginBreakPhase(breakState: SessionState, durationSeconds: Int) {
        cancelTicker()
        cancelAlarm()

        _sessionState.value = TimerSession(
            state = breakState,
            remainingSeconds = durationSeconds,
            totalPhaseSeconds = durationSeconds,
            nextBreakType = _sessionState.value.nextBreakType,
            secondsUntilShortBreak = 0,
            secondsUntilLongBreak = 0,
            isPaused = false
        )

        startTicker()
        scheduleAlarm(System.currentTimeMillis() + durationSeconds * 1000L)
    }

    private fun beginBreakFinishedPhase() {
        cancelTicker()
        cancelAlarm()
        cancelPendingVoiceJob()

        if (currentSettings.isVoiceEnabled) {
            audioAnnouncementPlayer.playBreakFinished(currentSettings.voiceVolume)
        }

        _sessionState.value = TimerSession(
            state = SessionState.BREAK_FINISHED,
            remainingSeconds = 0,
            totalPhaseSeconds = 0,
            nextBreakType = _sessionState.value.nextBreakType,
            secondsUntilShortBreak = 0,
            secondsUntilLongBreak = 0,
            isPaused = false
        )
    }

    private fun handlePhaseCompleted() {
        when (_sessionState.value.state) {
            SessionState.WORKING -> handleWorkingCompleted()
            SessionState.SHORT_BREAK, SessionState.LONG_BREAK -> beginBreakFinishedPhase()
            else -> {}
        }
    }

    private fun handleWorkingCompleted() {
        val phaseDuration = _sessionState.value.totalPhaseSeconds
        workedSecondsInCycle += phaseDuration

        val longInterval = currentSettings.longBreakIntervalMinutes * 60
        if (workedSecondsInCycle >= longInterval) {
            workedSecondsInCycle = 0
            beginBreakPendingPhase(SessionState.LONG_BREAK, currentSettings.longBreakDurationSeconds)
        } else {
            beginBreakPendingPhase(SessionState.SHORT_BREAK, currentSettings.shortBreakDurationSeconds)
        }
    }

    private fun startPendingVoiceLoop() {
        cancelPendingVoiceJob()
        pendingVoiceJob = engineScope.launch {
            while (isActive) {
                delay(VOICE_REPEAT_INTERVAL_MS)
                playBreakAnnouncement()
                cancelAlarm()
                scheduleAlarm(System.currentTimeMillis() + VOICE_REPEAT_INTERVAL_MS)
            }
        }
    }

    private fun cancelPendingVoiceJob() {
        pendingVoiceJob?.cancel()
        pendingVoiceJob = null
    }

    private fun playBreakAnnouncement() {
        if (currentSettings.isVoiceEnabled) {
            audioAnnouncementPlayer.playBreakStart(currentSettings.voiceVolume)
        }
    }

    private fun startTicker() {
        cancelTicker()
        tickerJob = engineScope.launch {
            while (isActive) {
                delay(1000L)
                processTick()
            }
        }
    }

    private fun processTick() {
        val current = _sessionState.value
        if (current.isPaused || current.state == SessionState.READY || current.state == SessionState.BREAK_PENDING || current.state == SessionState.BREAK_FINISHED) {
            return
        }

        if (current.remainingSeconds > 1) {
            decrementTick(current)
        } else {
            _sessionState.value = current.copy(remainingSeconds = 0)
            handlePhaseCompleted()
        }
    }

    private fun decrementTick(current: TimerSession) {
        val newRemaining = current.remainingSeconds - 1
        if (current.state == SessionState.WORKING) {
            val currentWorkedInCycle = workedSecondsInCycle + (current.totalPhaseSeconds - newRemaining)
            val shortInterval = currentSettings.shortBreakIntervalMinutes * 60
            val longInterval = currentSettings.longBreakIntervalMinutes * 60
            val timeUntilShort = shortInterval - (currentWorkedInCycle % shortInterval)
            val timeUntilLong = (longInterval - currentWorkedInCycle).coerceAtLeast(0)

            _sessionState.value = current.copy(
                remainingSeconds = newRemaining,
                secondsUntilShortBreak = timeUntilShort,
                secondsUntilLongBreak = timeUntilLong
            )
        } else {
            _sessionState.value = current.copy(remainingSeconds = newRemaining)
        }
    }

    private fun cancelTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun scheduleAlarm(triggerAtMillis: Long) {
        val intent = Intent(context, BreakAlarmReceiver::class.java).apply {
            action = BreakAlarmReceiver.ACTION_TIMER_ALARM
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm() {
        val intent = Intent(context, BreakAlarmReceiver::class.java).apply {
            action = BreakAlarmReceiver.ACTION_TIMER_ALARM
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)
        alarmManager.cancel(pendingIntent)
    }

    private fun startTimerService() {
        val intent = Intent(context, BreakTimerService::class.java).apply {
            action = BreakTimerService.ACTION_START_SERVICE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopTimerService() {
        val intent = Intent(context, BreakTimerService::class.java).apply {
            action = BreakTimerService.ACTION_STOP_SERVICE
        }
        context.stopService(intent)
    }

    companion object {
        private const val ALARM_REQUEST_CODE = 4201
        private const val VOICE_REPEAT_INTERVAL_MS = 20_000L
    }
}
