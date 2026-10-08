package com.example.makebreak.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val shortBreakIntervalMinutes: Int,
    val shortBreakDurationSeconds: Int,
    val longBreakIntervalMinutes: Int,
    val longBreakDurationSeconds: Int,
    val isVoiceEnabled: Boolean,
    val voiceVolume: Float
)
