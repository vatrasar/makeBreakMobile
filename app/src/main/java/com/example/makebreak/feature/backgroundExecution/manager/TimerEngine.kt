package com.example.makebreak.feature.backgroundExecution.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.makebreak.MainActivity
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
    private var phaseEndTimestampMillis: Long = 0L
    internal var currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() }

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
        if (_sessionState.value.state == SessionState.PAUSED || _sessionState.value.state == SessionState.READY) {
            return
        }
        cancelTicker()
        cancelPendingVoiceJob()
        cancelAlarm()
        audioAnnouncementPlayer.stop()
        syncTime()
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
            if (!_sessionState.value.isBreakAnnouncementsMuted) {
                startPendingVoiceLoop()
                cancelAlarm()
                scheduleAlarm(currentTimeMillisProvider() + VOICE_REPEAT_INTERVAL_MS)
            }
        } else {
            val remaining = _sessionState.value.remainingSeconds
            phaseEndTimestampMillis = currentTimeMillisProvider() + remaining * 1000L
            startTicker()
            scheduleAlarm(phaseEndTimestampMillis)
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
        phaseEndTimestampMillis = 0L
        workedSecondsInCycle = 0
        _sessionState.value = TimerSession(state = SessionState.READY)
        stopTimerService()
    }

    /**
     * Toggles muting of voice announcements during a pending break without starting the break.
     * Invoked by: BreakTimerViewModel.
     */
    fun toggleMuteBreakAnnouncements() {
        if (_sessionState.value.state != SessionState.BREAK_PENDING) {
            return
        }
        val currentlyMuted = _sessionState.value.isBreakAnnouncementsMuted
        if (currentlyMuted) {
            unmuteBreakAnnouncements()
        } else {
            muteBreakAnnouncements()
        }
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
            if (_sessionState.value.isBreakAnnouncementsMuted) {
                return
            }
            cancelPendingVoiceJob()
            playBreakAnnouncement()
            startPendingVoiceLoop()
            cancelAlarm()
            scheduleAlarm(currentTimeMillisProvider() + VOICE_REPEAT_INTERVAL_MS)
            return
        }
        handlePhaseCompleted()
    }

    private fun muteBreakAnnouncements() {
        cancelPendingVoiceJob()
        cancelAlarm()
        audioAnnouncementPlayer.stop()
        _sessionState.value = _sessionState.value.copy(isBreakAnnouncementsMuted = true)
    }

    private fun unmuteBreakAnnouncements() {
        _sessionState.value = _sessionState.value.copy(isBreakAnnouncementsMuted = false)
        playBreakAnnouncement()
        startPendingVoiceLoop()
        scheduleAlarm(currentTimeMillisProvider() + VOICE_REPEAT_INTERVAL_MS)
    }

    /**
     * Synchronizes the countdown timer with real wall-clock elapsed time.
     * Invoked by: MainActivity, BreakTimerService, BreakTimerViewModel.
     */
    fun syncTime() {
        val current = _sessionState.value
        if (current.isPaused) {
            return
        }
        when (current.state) {
            SessionState.WORKING, SessionState.SHORT_BREAK, SessionState.LONG_BREAK -> {
                updateActivePhaseTime(current)
            }
            else -> {}
        }
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
        phaseEndTimestampMillis = currentTimeMillisProvider() + phaseSeconds * 1000L

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
        scheduleAlarm(phaseEndTimestampMillis)
    }

    private fun beginBreakPendingPhase(breakState: SessionState, durationSeconds: Int) {
        cancelTicker()
        cancelAlarm()
        cancelPendingVoiceJob()

        phaseEndTimestampMillis = 0L
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
        scheduleAlarm(currentTimeMillisProvider() + VOICE_REPEAT_INTERVAL_MS)
    }

    private fun beginBreakPhase(breakState: SessionState, durationSeconds: Int) {
        cancelTicker()
        cancelAlarm()

        phaseEndTimestampMillis = currentTimeMillisProvider() + durationSeconds * 1000L

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
        scheduleAlarm(phaseEndTimestampMillis)
    }

    private fun beginBreakFinishedPhase() {
        cancelTicker()
        cancelAlarm()
        cancelPendingVoiceJob()

        phaseEndTimestampMillis = 0L

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
                scheduleAlarm(currentTimeMillisProvider() + VOICE_REPEAT_INTERVAL_MS)
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
                syncTime()
            }
        }
    }

    private fun updateActivePhaseTime(current: TimerSession) {
        val remainingMillis = phaseEndTimestampMillis - currentTimeMillisProvider()
        val newRemaining = if (remainingMillis <= 0L) 0 else ((remainingMillis + 999L) / 1000L).toInt()

        if (newRemaining <= 0) {
            _sessionState.value = current.copy(remainingSeconds = 0)
            handlePhaseCompleted()
            return
        }

        applyUpdatedRemainingSeconds(current, newRemaining)
    }

    private fun applyUpdatedRemainingSeconds(current: TimerSession, newRemaining: Int) {
        if (current.state == SessionState.WORKING) {
            val currentWorkedInCycle = workedSecondsInCycle + (current.totalPhaseSeconds - newRemaining)
            val shortInterval = currentSettings.shortBreakIntervalMinutes * 60
            val longInterval = currentSettings.longBreakIntervalMinutes * 60
            val timeUntilShort = (shortInterval - (currentWorkedInCycle % shortInterval)).coerceAtLeast(0)
            val timeUntilLong = (longInterval - currentWorkedInCycle).coerceAtLeast(0)

            if (current.remainingSeconds == newRemaining &&
                current.secondsUntilShortBreak == timeUntilShort &&
                current.secondsUntilLongBreak == timeUntilLong
            ) {
                return
            }

            _sessionState.value = current.copy(
                remainingSeconds = newRemaining,
                secondsUntilShortBreak = timeUntilShort,
                secondsUntilLongBreak = timeUntilLong
            )
        } else {
            if (current.remainingSeconds == newRemaining) {
                return
            }
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
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags) ?: return
        val showPendingIntent = createAlarmShowPendingIntent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                scheduleAlarmClock(triggerAtMillis, pendingIntent, showPendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            scheduleAlarmClock(triggerAtMillis, pendingIntent, showPendingIntent)
        }
    }

    private fun scheduleAlarmClock(
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        showPendingIntent: PendingIntent?
    ) {
        try {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (exception: Exception) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun createAlarmShowPendingIntent(): PendingIntent? {
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, ALARM_SHOW_REQUEST_CODE, showIntent, flags)
    }

    private fun cancelAlarm() {
        val intent = Intent(context, BreakAlarmReceiver::class.java).apply {
            action = BreakAlarmReceiver.ACTION_TIMER_ALARM
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags) ?: return
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
        private const val ALARM_SHOW_REQUEST_CODE = 4202
        private const val VOICE_REPEAT_INTERVAL_MS = 20_000L
    }
}
