package com.example.makebreak.feature.settings.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.makebreak.feature.settings.presentation.settings.SettingsScreen
import com.example.makebreak.feature.settings.presentation.settings.SettingsViewModel

fun NavGraphBuilder.setupSettingsNavigation(navController: NavController) {
    composable<SettingsScreen.SettingsRoute> {
        val viewModel: SettingsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        SettingsScreen(
            state = state,
            onEvent = viewModel::onEvent,
            effects = viewModel.effect
        )
    }
}
