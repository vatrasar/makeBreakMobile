package com.example.makebreak.feature.breakTimer.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.BreakTimerEvent
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.BreakTimerScreen
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.BreakTimerViewModel

fun NavGraphBuilder.setupBreakTimerNavigation(navController: NavController) {
    composable<BreakTimerScreen.TimerRoute> {
        val viewModel: BreakTimerViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            viewModel.onEvent(BreakTimerEvent.OnSyncTime)
        }

        BreakTimerScreen(
            state = state,
            onEvent = viewModel::onEvent,
            effects = viewModel.effect
        )
    }
}
