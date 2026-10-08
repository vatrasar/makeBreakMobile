package com.example.makebreak.feature.breakTimer.presentation.breakTimer

import androidx.compose.runtime.Immutable
import com.example.makebreak.core.domain.model.TimerSession

sealed class BreakTimerEffect {
    data class ShowSnackbar(val message: String) : BreakTimerEffect()
}

sealed class BreakTimerNavEffect

sealed class BreakTimerEvent {
    data object OnStartSession : BreakTimerEvent()
    data object OnStartBreak : BreakTimerEvent()
    data object OnPauseSession : BreakTimerEvent()
    data object OnResumeSession : BreakTimerEvent()
    data object OnResetSession : BreakTimerEvent()
    data object OnSkipBreak : BreakTimerEvent()
    data object OnFinishBreak : BreakTimerEvent()
    data object OnSyncTime : BreakTimerEvent()
}

@Immutable
data class BreakTimerState(
    val session: TimerSession = TimerSession(),
    val isLoading: Boolean = false
)
