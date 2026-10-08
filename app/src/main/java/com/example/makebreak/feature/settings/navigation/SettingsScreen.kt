package com.example.makebreak.feature.settings.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface SettingsScreen {

    @Serializable
    data object SettingsRoute : SettingsScreen
}
