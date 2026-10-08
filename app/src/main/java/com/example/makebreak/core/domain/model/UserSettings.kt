package com.example.makebreak.core.domain.model

data class UserSettings(
    val shortBreakIntervalMinutes: Int = 20,
    val shortBreakDurationSeconds: Int = 20,
    val longBreakIntervalMinutes: Int = 60,
    val longBreakDurationSeconds: Int = 300,
    val isVoiceEnabled: Boolean = true,
    val voiceVolume: Float = 1.0f
)
