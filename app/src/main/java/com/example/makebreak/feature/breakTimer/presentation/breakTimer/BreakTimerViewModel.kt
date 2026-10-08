package com.example.makebreak.feature.breakTimer.presentation.breakTimer

import androidx.lifecycle.viewModelScope
import com.example.makebreak.common.viewModel.BaseScreenAndNavEffectsViewModel
import com.example.makebreak.feature.backgroundExecution.manager.TimerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BreakTimerViewModel @Inject constructor(
    private val timerEngine: TimerEngine
) : BaseScreenAndNavEffectsViewModel<BreakTimerEffect, BreakTimerNavEffect>() {

    private val _state = MutableStateFlow(BreakTimerState())
    val state: StateFlow<BreakTimerState> = _state.asStateFlow()

    init {
        observeTimerEngine()
    }

    fun onEvent(event: BreakTimerEvent) {
        when (event) {
            BreakTimerEvent.OnStartSession -> timerEngine.startSession()
            BreakTimerEvent.OnStartBreak -> timerEngine.startBreak()
            BreakTimerEvent.OnPauseSession -> timerEngine.pauseSession()
            BreakTimerEvent.OnResumeSession -> timerEngine.resumeSession()
            BreakTimerEvent.OnResetSession -> timerEngine.resetSession()
            BreakTimerEvent.OnSkipBreak -> timerEngine.skipBreak()
            BreakTimerEvent.OnFinishBreak -> timerEngine.finishBreak()
            BreakTimerEvent.OnSyncTime -> timerEngine.syncTime()
        }
    }

    private fun observeTimerEngine() {
        viewModelScope.launch {
            timerEngine.sessionState.collect { session ->
                _state.value = _state.value.copy(session = session)
            }
        }
    }
}
