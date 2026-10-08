package com.example.makebreak.core.data.repository

import com.example.makebreak.core.data.local.dao.UserSettingsDao
import com.example.makebreak.core.data.local.mapper.toDomain
import com.example.makebreak.core.data.local.mapper.toEntity
import com.example.makebreak.core.domain.model.UserSettings
import com.example.makebreak.core.domain.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsRepositoryImpl @Inject constructor(
    private val userSettingsDao: UserSettingsDao
) : UserSettingsRepository {

    /**
     * Observes real-time updates to user settings.
     * Invoked by: SettingsViewModel, BreakTimerViewModel, BreakTimerService.
     */
    override fun getUserSettingsFlow(): Flow<UserSettings> {
        return userSettingsDao.getUserSettingsFlow().map { entity ->
            entity?.toDomain() ?: UserSettings()
        }
    }

    /**
     * Retrieves current user settings once.
     * Invoked by: BreakTimerService, SettingsViewModel.
     */
    override suspend fun getUserSettings(): UserSettings {
        val entity = userSettingsDao.getUserSettings()
        if (entity != null) {
            return entity.toDomain()
        }
        val defaultSettings = UserSettings()
        userSettingsDao.insertOrUpdateUserSettings(defaultSettings.toEntity())
        return defaultSettings
    }

    /**
     * Persists updated user settings.
     * Invoked by: SettingsViewModel.
     */
    override suspend fun saveUserSettings(settings: UserSettings) {
        userSettingsDao.insertOrUpdateUserSettings(settings.toEntity())
    }
}
