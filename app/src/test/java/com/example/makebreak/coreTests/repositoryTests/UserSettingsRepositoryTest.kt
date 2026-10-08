package com.example.makebreak.coreTests.repositoryTests

import com.example.makebreak.core.data.local.dao.UserSettingsDao
import com.example.makebreak.core.data.local.entities.UserSettingsEntity
import com.example.makebreak.core.data.repository.UserSettingsRepositoryImpl
import com.example.makebreak.core.domain.model.UserSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserSettingsRepositoryTest {

    private lateinit var dao: UserSettingsDao
    private lateinit var repository: UserSettingsRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = UserSettingsRepositoryImpl(dao)
    }

    @Test
    fun getUserSettings_whenEntityExistsInDb_returnsMappedDomain() = runTest {
        val existingEntity = UserSettingsEntity(
            id = 1,
            shortBreakIntervalMinutes = 30,
            shortBreakDurationSeconds = 25,
            longBreakIntervalMinutes = 75,
            longBreakDurationSeconds = 450,
            isVoiceEnabled = true,
            voiceVolume = 0.9f
        )
        coEvery { dao.getUserSettings() } returns existingEntity

        val result = repository.getUserSettings()

        assertEquals(30, result.shortBreakIntervalMinutes)
        assertEquals(25, result.shortBreakDurationSeconds)
        assertEquals(75, result.longBreakIntervalMinutes)
        assertEquals(450, result.longBreakDurationSeconds)
        assertEquals(true, result.isVoiceEnabled)
        assertEquals(0.9f, result.voiceVolume, 0.001f)
    }

    @Test
    fun getUserSettings_whenEntityDoesNotExist_insertsDefaultsAndReturnsDefaultSettings() = runTest {
        coEvery { dao.getUserSettings() } returns null

        val result = repository.getUserSettings()

        assertEquals(20, result.shortBreakIntervalMinutes)
        assertEquals(20, result.shortBreakDurationSeconds)
        assertEquals(60, result.longBreakIntervalMinutes)
        assertEquals(300, result.longBreakDurationSeconds)
        coVerify(exactly = 1) { dao.insertOrUpdateUserSettings(any()) }
    }

    @Test
    fun saveUserSettings_withUpdatedSettings_invokesDaoInsertOrUpdate() = runTest {
        val newSettings = UserSettings(
            shortBreakIntervalMinutes = 15,
            shortBreakDurationSeconds = 15,
            longBreakIntervalMinutes = 45,
            longBreakDurationSeconds = 180,
            isVoiceEnabled = false,
            voiceVolume = 0.5f
        )

        repository.saveUserSettings(newSettings)

        coVerify(exactly = 1) {
            dao.insertOrUpdateUserSettings(
                match {
                    it.shortBreakIntervalMinutes == 15 &&
                            it.shortBreakDurationSeconds == 15 &&
                            it.longBreakIntervalMinutes == 45 &&
                            it.longBreakDurationSeconds == 180 &&
                            !it.isVoiceEnabled
                }
            )
        }
    }
}
