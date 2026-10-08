package com.example.makebreak.coreTests.mapperTests

import com.example.makebreak.core.data.local.entities.UserSettingsEntity
import com.example.makebreak.core.data.local.mapper.toDomain
import com.example.makebreak.core.data.local.mapper.toEntity
import com.example.makebreak.core.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class UserSettingsMapperTest {

    @Test
    fun toDomain_withValidEntity_mapsAllFieldsCorrectly() {
        val entity = UserSettingsEntity(
            id = 1,
            shortBreakIntervalMinutes = 25,
            shortBreakDurationSeconds = 30,
            longBreakIntervalMinutes = 90,
            longBreakDurationSeconds = 600,
            isVoiceEnabled = true,
            voiceVolume = 0.8f
        )

        val domain = entity.toDomain()

        assertEquals(25, domain.shortBreakIntervalMinutes)
        assertEquals(30, domain.shortBreakDurationSeconds)
        assertEquals(90, domain.longBreakIntervalMinutes)
        assertEquals(600, domain.longBreakDurationSeconds)
        assertEquals(true, domain.isVoiceEnabled)
        assertEquals(0.8f, domain.voiceVolume, 0.001f)
    }

    @Test
    fun toEntity_withValidDomainModel_mapsAllFieldsCorrectly() {
        val domain = UserSettings(
            shortBreakIntervalMinutes = 15,
            shortBreakDurationSeconds = 15,
            longBreakIntervalMinutes = 45,
            longBreakDurationSeconds = 300,
            isVoiceEnabled = false,
            voiceVolume = 0.5f
        )

        val entity = domain.toEntity(id = 1)

        assertEquals(1, entity.id)
        assertEquals(15, entity.shortBreakIntervalMinutes)
        assertEquals(15, entity.shortBreakDurationSeconds)
        assertEquals(45, entity.longBreakIntervalMinutes)
        assertEquals(300, entity.longBreakDurationSeconds)
        assertEquals(false, entity.isVoiceEnabled)
        assertEquals(0.5f, entity.voiceVolume, 0.001f)
    }
}
