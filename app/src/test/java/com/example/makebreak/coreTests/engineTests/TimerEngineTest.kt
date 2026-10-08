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
    fun skipBreak_duringBreakPending_stopsRepeatingVoiceAndResumesWork() = runTest(testDispatcher) {
        val voiceSettings = testSettings.copy(isVoiceEnabled = true)
        every { userSettingsRepository.getUserSettingsFlow() } returns flowOf(voiceSettings)

        val engine = TimerEngine(context, userSettingsRepository, audioPlayer)
        currentEngine = engine
        testDispatcher.scheduler.advanceUntilIdle()

        try {
            engine.startSession()
            engine.onAlarmTriggered()

            assertEquals(SessionState.BREAK_PENDING, engine.sessionState.value.state)

            engine.skipBreak()
            assertEquals(SessionState.WORKING, engine.sessionState.value.state)
            io.mockk.verify(atLeast = 1) { audioPlayer.stop() }

            testDispatcher.scheduler.advanceTimeBy(20_000L)
            io.mockk.verify(exactly = 1) { audioPlayer.playBreakStart(any()) }
        } finally {
            engine.resetSession()
        }
    }
}
