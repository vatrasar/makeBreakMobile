package com.example.makebreak.feature.settings.presentation.settings

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.viewModelScope
import com.example.makebreak.common.viewModel.BaseScreenAndNavEffectsViewModel
import com.example.makebreak.core.domain.model.UserSettings
import com.example.makebreak.core.domain.repository.UserSettingsRepository
import com.example.makebreak.core.domain.service.AudioAnnouncementPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val audioAnnouncementPlayer: AudioAnnouncementPlayer,
    @ApplicationContext private val context: Context
) : BaseScreenAndNavEffectsViewModel<SettingsEffect, SettingsNavEffect>() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
        checkBatteryOptimization()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnShortIntervalChange -> updateShortInterval(event.value)
            is SettingsEvent.OnShortDurationChange -> updateShortDuration(event.value)
            is SettingsEvent.OnLongIntervalChange -> updateLongInterval(event.value)
            is SettingsEvent.OnLongDurationChange -> updateLongDuration(event.value)
            is SettingsEvent.OnVoiceEnabledChange -> updateVoiceEnabled(event.enabled)
            is SettingsEvent.OnVoiceVolumeChange -> updateVoiceVolume(event.volume)
            SettingsEvent.OnTestStartVoice -> testStartVoice()
            SettingsEvent.OnTestFinishedVoice -> testFinishedVoice()
            SettingsEvent.OnSaveSettings -> saveSettings()
            SettingsEvent.OnResetDefaults -> resetDefaults()
            SettingsEvent.OnRequestBatteryOptimization -> requestBatteryOptimization()
            SettingsEvent.OnRefreshBatteryStatus -> checkBatteryOptimization()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = userSettingsRepository.getUserSettings()
            _state.value = _state.value.copy(
                shortBreakIntervalMinutes = settings.shortBreakIntervalMinutes,
                shortBreakDurationSeconds = settings.shortBreakDurationSeconds,
                longBreakIntervalMinutes = settings.longBreakIntervalMinutes,
                longBreakDurationSeconds = settings.longBreakDurationSeconds,
                isVoiceEnabled = settings.isVoiceEnabled,
                voiceVolume = settings.voiceVolume
            )
        }
    }

    private fun updateShortInterval(value: Int) {
        _state.value = _state.value.copy(shortBreakIntervalMinutes = value.coerceIn(1, 180))
    }

    private fun updateShortDuration(value: Int) {
        _state.value = _state.value.copy(shortBreakDurationSeconds = value.coerceIn(5, 600))
    }

    private fun updateLongInterval(value: Int) {
        _state.value = _state.value.copy(longBreakIntervalMinutes = value.coerceIn(5, 360))
    }

    private fun updateLongDuration(value: Int) {
        _state.value = _state.value.copy(longBreakDurationSeconds = value.coerceIn(30, 3600))
    }

    private fun updateVoiceEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isVoiceEnabled = enabled)
    }

    private fun updateVoiceVolume(volume: Float) {
        _state.value = _state.value.copy(voiceVolume = volume.coerceIn(0f, 1f))
    }

    private fun testStartVoice() {
        audioAnnouncementPlayer.playBreakStart(_state.value.voiceVolume)
    }

    private fun testFinishedVoice() {
        audioAnnouncementPlayer.playBreakFinished(_state.value.voiceVolume)
    }

    private fun saveSettings() {
        viewModelScope.launch {
            val current = _state.value
            val settings = UserSettings(
                shortBreakIntervalMinutes = current.shortBreakIntervalMinutes,
                shortBreakDurationSeconds = current.shortBreakDurationSeconds,
                longBreakIntervalMinutes = current.longBreakIntervalMinutes,
                longBreakDurationSeconds = current.longBreakDurationSeconds,
                isVoiceEnabled = current.isVoiceEnabled,
                voiceVolume = current.voiceVolume
            )
            userSettingsRepository.saveUserSettings(settings)
            sendEffect(SettingsEffect.ShowSnackbar("Settings saved"))
        }
    }

    private fun resetDefaults() {
        viewModelScope.launch {
            val defaults = UserSettings()
            userSettingsRepository.saveUserSettings(defaults)
            _state.value = _state.value.copy(
                shortBreakIntervalMinutes = defaults.shortBreakIntervalMinutes,
                shortBreakDurationSeconds = defaults.shortBreakDurationSeconds,
                longBreakIntervalMinutes = defaults.longBreakIntervalMinutes,
                longBreakDurationSeconds = defaults.longBreakDurationSeconds,
                isVoiceEnabled = defaults.isVoiceEnabled,
                voiceVolume = defaults.voiceVolume
            )
            sendEffect(SettingsEffect.ShowSnackbar("Defaults restored"))
        }
    }

    private fun checkBatteryOptimization() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        _state.value = _state.value.copy(isIgnoringBatteryOptimizations = isIgnoring)
    }

    private fun requestBatteryOptimization() {
        sendEffect(SettingsEffect.OpenBatteryOptimizationSettings)
    }
}
