package com.example.makebreak.feature.settings.presentation.settings

import androidx.compose.runtime.Immutable

sealed class SettingsEffect {
    data class ShowSnackbar(val message: String) : SettingsEffect()
    data object OpenBatteryOptimizationSettings : SettingsEffect()
}

sealed class SettingsNavEffect

sealed class SettingsEvent {
    data class OnShortIntervalChange(val value: Int) : SettingsEvent()
    data class OnShortDurationChange(val value: Int) : SettingsEvent()
    data class OnLongIntervalChange(val value: Int) : SettingsEvent()
    data class OnLongDurationChange(val value: Int) : SettingsEvent()
    data class OnVoiceEnabledChange(val enabled: Boolean) : SettingsEvent()
    data class OnVoiceVolumeChange(val volume: Float) : SettingsEvent()
    data object OnTestStartVoice : SettingsEvent()
    data object OnTestFinishedVoice : SettingsEvent()
    data object OnSaveSettings : SettingsEvent()
    data object OnResetDefaults : SettingsEvent()
    data object OnRequestBatteryOptimization : SettingsEvent()
    data object OnRefreshBatteryStatus : SettingsEvent()
}

@Immutable
data class SettingsState(
    val shortBreakIntervalMinutes: Int = 20,
    val shortBreakDurationSeconds: Int = 20,
    val longBreakIntervalMinutes: Int = 60,
    val longBreakDurationSeconds: Int = 300,
    val isVoiceEnabled: Boolean = true,
    val voiceVolume: Float = 1.0f,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val isLoading: Boolean = false
)
