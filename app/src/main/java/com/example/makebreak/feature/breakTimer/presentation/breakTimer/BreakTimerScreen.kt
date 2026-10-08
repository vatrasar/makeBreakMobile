package com.example.makebreak.feature.breakTimer.presentation.breakTimer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents.AppHeader
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents.SessionStatusBadge
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents.TimerControls
import com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents.TimerDisplay
import kotlinx.coroutines.flow.Flow

/**
 * Main BreakTimer screen displaying the active session countdown, status badge, and controls.
 *
 * Available Functionalities:
 * Start focus session, start break upon confirmation, pause, resume, reset, toggle voice muting for pending breaks, and confirm break completion.
 *
 * Key UI elements:
 * AppHeader branding, SessionStatusBadge, TimerDisplay with circular progress, TimerControls action buttons.
 *
 * Navigation:
 * - Navigate From: AppBottomBar (Timer tab).
 * - Navigate To: None (main tab screen).
 */
@Composable
fun BreakTimerScreen(
    state: BreakTimerState,
    onEvent: (BreakTimerEvent) -> Unit,
    effects: Flow<BreakTimerEffect>,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is BreakTimerEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("breakTimerScaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        BreakTimerContent(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun BreakTimerContent(
    state: BreakTimerState,
    onEvent: (BreakTimerEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
            .testTag("breakTimerContentColumn"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppHeader()
        Spacer(modifier = Modifier.height(8.dp))
        SessionStatusBadge(sessionState = state.session.state)
        Spacer(modifier = Modifier.weight(1f))
        TimerDisplay(
            session = state.session,
            onStartBreak = { onEvent(BreakTimerEvent.OnStartBreak) },
            onFinishBreak = { onEvent(BreakTimerEvent.OnFinishBreak) }
        )
        Spacer(modifier = Modifier.weight(1f))
        TimerControls(
            sessionState = state.session.state,
            isMuted = state.session.isBreakAnnouncementsMuted,
            onStart = { onEvent(BreakTimerEvent.OnStartSession) },
            onPause = { onEvent(BreakTimerEvent.OnPauseSession) },
            onResume = { onEvent(BreakTimerEvent.OnResumeSession) },
            onReset = { onEvent(BreakTimerEvent.OnResetSession) },
            onToggleMute = { onEvent(BreakTimerEvent.OnToggleMuteBreakAnnouncements) }
        )
    }
}
