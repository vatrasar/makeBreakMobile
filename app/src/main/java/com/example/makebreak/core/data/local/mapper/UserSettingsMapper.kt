package com.example.makebreak.core.data.local.mapper

import com.example.makebreak.core.data.local.entities.UserSettingsEntity
import com.example.makebreak.core.domain.model.UserSettings

fun UserSettingsEntity.toDomain(): UserSettings {
    return UserSettings(
        shortBreakIntervalMinutes = shortBreakIntervalMinutes,
        shortBreakDurationSeconds = shortBreakDurationSeconds,
        longBreakIntervalMinutes = longBreakIntervalMinutes,
        longBreakDurationSeconds = longBreakDurationSeconds,
        isVoiceEnabled = isVoiceEnabled,
        voiceVolume = voiceVolume
    )
}

fun UserSettings.toEntity(id: Int = 1): UserSettingsEntity {
    return UserSettingsEntity(
        id = id,
        shortBreakIntervalMinutes = shortBreakIntervalMinutes,
        shortBreakDurationSeconds = shortBreakDurationSeconds,
        longBreakIntervalMinutes = longBreakIntervalMinutes,
        longBreakDurationSeconds = longBreakDurationSeconds,
        isVoiceEnabled = isVoiceEnabled,
        voiceVolume = voiceVolume
    )
}
