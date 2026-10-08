package com.example.makebreak.coreTests.engineTests

import android.app.AlarmManager
import android.content.Context
import com.example.makebreak.core.domain.enums.SessionState
import com.example.makebreak.core.domain.model.UserSettings
import com.example.makebreak.core.domain.repository.UserSettingsRepository
import com.example.makebreak.core.domain.service.AudioAnnouncementPlayer
import com.example.makebreak.feature.backgroundExecution.manager.TimerEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var audioPlayer: AudioAnnouncementPlayer

    private val testSettings = UserSettings(
        shortBreakIntervalMinutes = 20,
        shortBreakDurationSeconds = 20,
        longBreakIntervalMinutes = 60,
        longBreakDurationSeconds = 300,
        isVoiceEnabled = false
    )

    private var currentEngine: TimerEngine? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)
        userSettingsRepository = mockk(relaxed = true)
        audioPlayer = mockk(relaxed = true)

        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { userSettingsRepository.getUserSettingsFlow() } returns flowOf(testSettings)
    }

    @After
    fun tearDown() {
        currentEngine?.resetSession()
        currentEngine = null
        Dispatchers.resetMain()
    }

    @Test
    fun startSession_initializesWorkingStateWithFullShortInterval() = runTest(testDispatcher) {
        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        engine.startSession()

        val state = engine.sessionState.value
        assertEquals(SessionState.WORKING, state.state)
        assertEquals(1200, state.remainingSeconds)
        assertEquals(1200, state.totalPhaseSeconds)
        assertEquals(1200, state.secondsUntilShortBreak)
        assertEquals(3600, state.secondsUntilLongBreak)

        engine.resetSession()
    }

    @Test
    fun finishBreak_afterFirstShortBreak_startsNextWorkSessionWithFullShortInterval() = runTest(testDispatcher) {
        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        engine.startSession()

        engine.onAlarmTriggered()
        assertEquals(SessionState.BREAK_PENDING, engine.sessionState.value.state)
        assertEquals(20, engine.sessionState.value.remainingSeconds)

        engine.startBreak()
        assertEquals(SessionState.SHORT_BREAK, engine.sessionState.value.state)
        assertEquals(20, engine.sessionState.value.remainingSeconds)

        engine.onAlarmTriggered()
        assertEquals(SessionState.BREAK_FINISHED, engine.sessionState.value.state)

        engine.finishBreak()

        val nextWorkState = engine.sessionState.value
        assertEquals(SessionState.WORKING, nextWorkState.state)
        assertEquals(1200, nextWorkState.remainingSeconds)
        assertEquals(1200, nextWorkState.totalPhaseSeconds)
        assertEquals(1200, nextWorkState.secondsUntilShortBreak)
        assertEquals(2400, nextWorkState.secondsUntilLongBreak)

        engine.resetSession()
    }

    @Test
    fun onAlarmTriggered_whenLongBreakIntervalReached_transitionsToLongBreakPending() = runTest(testDispatcher) {
        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        engine.startSession()

        engine.onAlarmTriggered()
        engine.startBreak()
        engine.onAlarmTriggered()
        engine.finishBreak()

        engine.onAlarmTriggered()
        engine.startBreak()
        engine.onAlarmTriggered()
        engine.finishBreak()

        engine.onAlarmTriggered()
        assertEquals(SessionState.BREAK_PENDING, engine.sessionState.value.state)
        assertEquals(300, engine.sessionState.value.remainingSeconds)
        assertEquals(300, engine.sessionState.value.totalPhaseSeconds)

        engine.startBreak()
        assertEquals(SessionState.LONG_BREAK, engine.sessionState.value.state)
        assertEquals(300, engine.sessionState.value.remainingSeconds)

        engine.onAlarmTriggered()
        assertEquals(SessionState.BREAK_FINISHED, engine.sessionState.value.state)

        engine.finishBreak()
        val restartedState = engine.sessionState.value
        assertEquals(SessionState.WORKING, restartedState.state)
        assertEquals(1200, restartedState.remainingSeconds)
        assertEquals(3600, restartedState.secondsUntilLongBreak)

        engine.resetSession()
    }

    @Test
    fun breakPending_repeatsVoiceAnnouncementEvery20SecondsUntilStarted() = runTest(testDispatcher) {
        val voiceSettings = testSettings.copy(isVoiceEnabled = true)
        every { userSettingsRepository.getUserSettingsFlow() } returns flowOf(voiceSettings)

        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            engine.onAlarmTriggered()

            assertEquals(SessionState.BREAK_PENDING, engine.sessionState.value.state)
            io.mockk.verify(exactly = 1) { audioPlayer.playBreakStart(any()) }

            testDispatcher.scheduler.runCurrent()
            testDispatcher.scheduler.advanceTimeBy(20_000L)
            testDispatcher.scheduler.runCurrent()
            io.mockk.verify(exactly = 2) { audioPlayer.playBreakStart(any()) }

            testDispatcher.scheduler.advanceTimeBy(20_000L)
            testDispatcher.scheduler.runCurrent()
            io.mockk.verify(exactly = 3) { audioPlayer.playBreakStart(any()) }

            engine.startBreak()
            assertEquals(SessionState.SHORT_BREAK, engine.sessionState.value.state)
            io.mockk.verify(atLeast = 1) { audioPlayer.stop() }

            testDispatcher.scheduler.advanceTimeBy(20_000L)
            testDispatcher.scheduler.runCurrent()
            io.mockk.verify(exactly = 3) { audioPlayer.playBreakStart(any()) }
        } finally {
            engine.resetSession()
        }
    }

    @Test
    fun toggleMuteBreakAnnouncements_duringBreakPending_mutesAudioWithoutStartingBreak() = runTest(testDispatcher) {
        val voiceSettings = testSettings.copy(isVoiceEnabled = true)
        every { userSettingsRepository.getUserSettingsFlow() } returns flowOf(voiceSettings)

        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            engine.onAlarmTriggered()

            assertEquals(SessionState.BREAK_PENDING, engine.sessionState.value.state)
            org.junit.Assert.assertFalse(engine.sessionState.value.isBreakAnnouncementsMuted)

            engine.toggleMuteBreakAnnouncements()

            assertEquals(SessionState.BREAK_PENDING, engine.sessionState.value.state)
            org.junit.Assert.assertTrue(engine.sessionState.value.isBreakAnnouncementsMuted)
            io.mockk.verify(atLeast = 1) { audioPlayer.stop() }

            testDispatcher.scheduler.advanceTimeBy(40_000L)
            io.mockk.verify(exactly = 1) { audioPlayer.playBreakStart(any()) }
        } finally {
            engine.resetSession()
        }
    }

    @Test
    fun toggleMuteBreakAnnouncements_whenAlreadyMuted_unmutesAndResumesAnnouncements() = runTest(testDispatcher) {
        val voiceSettings = testSettings.copy(isVoiceEnabled = true)
        every { userSettingsRepository.getUserSettingsFlow() } returns flowOf(voiceSettings)

        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            engine.onAlarmTriggered()
            engine.toggleMuteBreakAnnouncements()
            org.junit.Assert.assertTrue(engine.sessionState.value.isBreakAnnouncementsMuted)

            engine.toggleMuteBreakAnnouncements()
            org.junit.Assert.assertFalse(engine.sessionState.value.isBreakAnnouncementsMuted)
            io.mockk.verify(exactly = 2) { audioPlayer.playBreakStart(any()) }

            testDispatcher.scheduler.advanceTimeBy(20_000L)
            testDispatcher.scheduler.runCurrent()
            io.mockk.verify(exactly = 3) { audioPlayer.playBreakStart(any()) }
        } finally {
            engine.resetSession()
        }
    }

    @Test
    fun onAlarmTriggered_whenBreakPendingAndMuted_doesNotPlayAudio() = runTest(testDispatcher) {
        val voiceSettings = testSettings.copy(isVoiceEnabled = true)
        every { userSettingsRepository.getUserSettingsFlow() } returns flowOf(voiceSettings)

        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            engine.onAlarmTriggered()
            engine.toggleMuteBreakAnnouncements()

            engine.onAlarmTriggered()

            io.mockk.verify(exactly = 1) { audioPlayer.playBreakStart(any()) }
        } finally {
            engine.resetSession()
        }
    }

    @Test
    fun syncTime_whenTimeElapsedWithScreenOff_updatesRemainingSecondsAccurately() = runTest(testDispatcher) {
        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        var simulatedTime = 1_000_000L
        engine.currentTimeMillisProvider = { simulatedTime }
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            assertEquals(1200, engine.sessionState.value.remainingSeconds)

            simulatedTime += 600_000L
            engine.syncTime()

            val state = engine.sessionState.value
            assertEquals(SessionState.WORKING, state.state)
            assertEquals(600, state.remainingSeconds)
            assertEquals(600, state.secondsUntilShortBreak)
            assertEquals(3000, state.secondsUntilLongBreak)
        } finally {
            engine.resetSession()
        }
    }

    @Test
    fun syncTime_whenFullWorkPhaseElapsed_transitionsToBreakPending() = runTest(testDispatcher) {
        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        var simulatedTime = 1_000_000L
        engine.currentTimeMillisProvider = { simulatedTime }
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            assertEquals(1200, engine.sessionState.value.remainingSeconds)

            simulatedTime += 1_200_000L
            engine.syncTime()

            val state = engine.sessionState.value
            assertEquals(SessionState.BREAK_PENDING, state.state)
            assertEquals(20, state.remainingSeconds)
        } finally {
            engine.resetSession()
        }
    }

    @Test
    fun syncTime_duringBreakPhase_updatesRemainingBreakSeconds() = runTest(testDispatcher) {
        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        var simulatedTime = 1_000_000L
        engine.currentTimeMillisProvider = { simulatedTime }
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            simulatedTime += 1_200_000L
            engine.syncTime()

            engine.startBreak()
            assertEquals(SessionState.SHORT_BREAK, engine.sessionState.value.state)
            assertEquals(20, engine.sessionState.value.remainingSeconds)

            simulatedTime += 12_000L
            engine.syncTime()

            assertEquals(SessionState.SHORT_BREAK, engine.sessionState.value.state)
            assertEquals(8, engine.sessionState.value.remainingSeconds)
        } finally {
            engine.resetSession()
        }
    }

    @Test
    fun pauseAndResume_recalculatesEndTimestampFromRemainingSeconds() = runTest(testDispatcher) {
        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        var simulatedTime = 1_000_000L
        engine.currentTimeMillisProvider = { simulatedTime }
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            simulatedTime += 300_000L
            engine.pauseSession()

            assertEquals(SessionState.PAUSED, engine.sessionState.value.state)
            assertEquals(900, engine.sessionState.value.remainingSeconds)

            simulatedTime += 10_000_000L
            engine.syncTime()
            assertEquals(900, engine.sessionState.value.remainingSeconds)

            engine.resumeSession()
            assertEquals(SessionState.WORKING, engine.sessionState.value.state)

            simulatedTime += 100_000L
            engine.syncTime()
            assertEquals(800, engine.sessionState.value.remainingSeconds)
        } finally {
            engine.resetSession()
        }
    }
}
