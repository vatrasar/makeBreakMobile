package com.example.makebreak.core.domain.repository

import com.example.makebreak.core.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {

    /**
     * Observes real-time updates to user settings.
     * Invoked by: SettingsViewModel, BreakTimerViewModel, BreakTimerService.
     */
    fun getUserSettingsFlow(): Flow<UserSettings>

    /**
     * Retrieves current user settings once.
     * Invoked by: BreakTimerService, SettingsViewModel.
     */
    suspend fun getUserSettings(): UserSettings

    /**
     * Persists updated user settings.
     * Invoked by: SettingsViewModel.
     */
    suspend fun saveUserSettings(settings: UserSettings)
}
