package com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.makebreak.R
import com.example.makebreak.core.domain.enums.SessionState

/**
 * Displays a styled status badge highlighting the active session phase.
 *
 * Usage:
 * sessionState: current phase of the timer session.
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * Surface chip container, dynamic phase icon, phase label text.
 *
 * Used In:
 * BreakTimerScreen
 */
@Composable
fun SessionStatusBadge(
    sessionState: SessionState,
    modifier: Modifier = Modifier
) {
    val (labelRes, icon) = getBadgeDetails(sessionState)
    val containerColor by animateColorAsState(
        targetValue = when (sessionState) {
            SessionState.READY -> MaterialTheme.colorScheme.surfaceVariant
            SessionState.WORKING -> MaterialTheme.colorScheme.primaryContainer
            SessionState.BREAK_PENDING -> MaterialTheme.colorScheme.tertiaryContainer
            SessionState.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
            SessionState.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
            SessionState.BREAK_FINISHED -> MaterialTheme.colorScheme.primaryContainer
            SessionState.PAUSED -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "badgeContainerColor"
    )

    Surface(
        modifier = modifier.testTag("sessionStatusBadge"),
        shape = RoundedCornerShape(24.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = labelRes),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun getBadgeDetails(state: SessionState): Pair<Int, ImageVector> {
    return when (state) {
        SessionState.READY -> Pair(R.string.status_ready, Icons.Default.PlayCircleOutline)
        SessionState.WORKING -> Pair(R.string.status_working, Icons.Default.Laptop)
        SessionState.BREAK_PENDING -> Pair(R.string.status_break_pending, Icons.Default.Coffee)
        SessionState.SHORT_BREAK -> Pair(R.string.status_short_break, Icons.Default.Coffee)
        SessionState.LONG_BREAK -> Pair(R.string.status_long_break, Icons.Default.NaturePeople)
        SessionState.BREAK_FINISHED -> Pair(R.string.status_break_finished, Icons.Default.CheckCircle)
        SessionState.PAUSED -> Pair(R.string.status_paused, Icons.Default.PauseCircle)
    }
}
