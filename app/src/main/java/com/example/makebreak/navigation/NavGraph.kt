package com.example.makebreak.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.makebreak.feature.breakTimer.navigation.BreakTimerScreen
import com.example.makebreak.feature.breakTimer.navigation.setupBreakTimerNavigation
import com.example.makebreak.feature.settings.navigation.setupSettingsNavigation

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BreakTimerScreen.TimerRoute,
        modifier = modifier
    ) {
        setupBreakTimerNavigation(navController)
        setupSettingsNavigation(navController)
    }
}
