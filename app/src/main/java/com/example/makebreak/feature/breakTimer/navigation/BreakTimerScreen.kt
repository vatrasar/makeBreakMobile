package com.example.makebreak.feature.breakTimer.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface BreakTimerScreen {

    @Serializable
    data object TimerRoute : BreakTimerScreen
}
