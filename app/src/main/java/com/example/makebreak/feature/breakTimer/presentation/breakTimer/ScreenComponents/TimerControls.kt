package com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.makebreak.R
import com.example.makebreak.core.domain.enums.SessionState

/**
 * Control action buttons for managing active timer sessions.
 *
 * Usage:
 * sessionState: current state of the timer session.
 * onStart: triggers session start.
 * onPause: triggers session pause.
 * onResume: triggers session resume.
 * onReset: resets session to ready.
 * onSkipBreak: skips current break phase.
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * Start Session button, Pause/Resume button, Reset button, Skip button.
 *
 * Used In:
 * BreakTimerScreen
 */
@Composable
fun TimerControls(
    sessionState: SessionState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSkipBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("timerControlsContainer"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (sessionState) {
            SessionState.READY -> ReadyControls(onStart = onStart)
            SessionState.WORKING -> WorkingControls(onPause = onPause, onReset = onReset)
            SessionState.BREAK_PENDING -> BreakPendingControls(
                onSkip = onSkipBreak,
                onReset = onReset
            )
            SessionState.PAUSED -> PausedControls(onResume = onResume, onReset = onReset)
            SessionState.SHORT_BREAK, SessionState.LONG_BREAK -> InBreakControls(onSkip = onSkipBreak, onReset = onReset)
            SessionState.BREAK_FINISHED -> BreakFinishedControls(onReset = onReset)
        }
    }
}

@Composable
private fun ReadyControls(onStart: () -> Unit) {
    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("startSessionButton"),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(
            text = stringResource(R.string.btn_start),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun WorkingControls(onPause: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("resetSessionButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_reset),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
        Button(
            onClick = onPause,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("pauseSessionButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Pause, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_pause),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PausedControls(onResume: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("resetSessionButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_reset),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
        Button(
            onClick = onResume,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("resumeSessionButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_resume),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun InBreakControls(onSkip: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("resetSessionButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_reset),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
        Button(
            onClick = onSkip,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("skipBreakButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.FastForward, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_skip_break),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BreakFinishedControls(onReset: () -> Unit) {
    OutlinedButton(
        onClick = onReset,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("resetSessionButton"),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
        Text(
            text = stringResource(R.string.btn_reset),
            modifier = Modifier.padding(start = 6.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun BreakPendingControls(
    onSkip: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("resetSessionButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_reset),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("skipBreakButton"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.FastForward, contentDescription = null)
            Text(
                text = stringResource(R.string.btn_skip_break),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1
            )
        }
    }
}
