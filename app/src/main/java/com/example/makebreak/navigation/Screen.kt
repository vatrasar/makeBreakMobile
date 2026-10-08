package com.example.makebreak.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.makebreak.R
import com.example.makebreak.feature.breakTimer.navigation.BreakTimerScreen
import com.example.makebreak.feature.settings.navigation.SettingsScreen

sealed class Screen(
    val route: Any,
    val titleRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    data object Timer : Screen(
        route = BreakTimerScreen.TimerRoute,
        titleRes = R.string.nav_timer,
        icon = Icons.Default.HourglassBottom,
        testTag = "bottomNavTimer"
    )

    data object Settings : Screen(
        route = SettingsScreen.SettingsRoute,
        titleRes = R.string.nav_settings,
        icon = Icons.Default.Settings,
        testTag = "bottomNavSettings"
    )
}
