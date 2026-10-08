package com.example.makebreak.featuresTests.breakTimerTests

import com.example.makebreak.core.domain.enums.SessionState
import com.example.makebreak.core.domain.model.TimerSession
import com.example.makebreak.feature.backgroundExecution.manager.TimerEngine
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.BreakTimerEvent
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.BreakTimerViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BreakTimerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var timerEngine: TimerEngine
    private val sessionStateFlow = MutableStateFlow(TimerSession())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        timerEngine = mockk(relaxed = true)
        every { timerEngine.sessionState } returns sessionStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onEvent_startSession_triggersTimerEngineStartSession() = runTest(testDispatcher) {
        val viewModel = BreakTimerViewModel(timerEngine)

        viewModel.onEvent(BreakTimerEvent.OnStartSession)

        verify(exactly = 1) { timerEngine.startSession() }
    }

    @Test
    fun onEvent_pauseSession_triggersTimerEnginePauseSession() = runTest(testDispatcher) {
        val viewModel = BreakTimerViewModel(timerEngine)

        viewModel.onEvent(BreakTimerEvent.OnPauseSession)

        verify(exactly = 1) { timerEngine.pauseSession() }
    }

    @Test
    fun onEvent_startBreak_triggersTimerEngineStartBreak() = runTest(testDispatcher) {
        val viewModel = BreakTimerViewModel(timerEngine)

        viewModel.onEvent(BreakTimerEvent.OnStartBreak)

        verify(exactly = 1) { timerEngine.startBreak() }
    }

    @Test
    fun onEvent_finishBreak_triggersTimerEngineFinishBreak() = runTest(testDispatcher) {
        val viewModel = BreakTimerViewModel(timerEngine)

        viewModel.onEvent(BreakTimerEvent.OnFinishBreak)

        verify(exactly = 1) { timerEngine.finishBreak() }
    }

    @Test
    fun onEvent_syncTime_triggersTimerEngineSyncTime() = runTest(testDispatcher) {
        val viewModel = BreakTimerViewModel(timerEngine)

        viewModel.onEvent(BreakTimerEvent.OnSyncTime)

        verify(exactly = 1) { timerEngine.syncTime() }
    }

    @Test
    fun stateObserver_whenTimerEngineEmitsNewSession_updatesUiState() = runTest(testDispatcher) {
        val viewModel = BreakTimerViewModel(timerEngine)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedSession = TimerSession(
            state = SessionState.WORKING,
            remainingSeconds = 1200
        )
        sessionStateFlow.value = updatedSession
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SessionState.WORKING, viewModel.state.value.session.state)
        assertEquals(1200, viewModel.state.value.session.remainingSeconds)
    }
}
