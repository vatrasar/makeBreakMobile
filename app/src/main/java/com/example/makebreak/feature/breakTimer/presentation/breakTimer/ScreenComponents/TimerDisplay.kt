package com.example.makebreak.feature.breakTimer.presentation.breakTimer.ScreenComponents

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.makebreak.R
import com.example.makebreak.core.domain.enums.NextBreakType
import com.example.makebreak.core.domain.enums.SessionState
import com.example.makebreak.core.domain.model.TimerSession

/**
 * Visual countdown display with animated circular progress indicator, session details, and in-circle action triggers.
 *
 * Usage:
 * session: contains timer progress, countdown seconds, and active state.
 * onStartBreak: triggers pending break start when in-circle action is tapped.
 * onFinishBreak: confirms break completion and resumes work when in-circle action is tapped.
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * Circular progress ring, countdown time text, in-circle animated action button, supportive subtitle description.
 *
 * Used In:
 * BreakTimerScreen
 */
@Composable
fun TimerDisplay(
    session: TimerSession,
    onStartBreak: () -> Unit = {},
    onFinishBreak: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = session.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "timerProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timerDisplayContainer"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(240.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.size(240.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp
            )
            TimerCenterContent(
                session = session,
                onStartBreak = onStartBreak,
                onFinishBreak = onFinishBreak
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        PhaseDescriptionText(session = session)
    }
}

@Composable
private fun TimerCenterContent(
    session: TimerSession,
    onStartBreak: () -> Unit,
    onFinishBreak: () -> Unit
) {
    AnimatedContent(
        targetState = session.state,
        transitionSpec = {
            (scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialScale = 0.7f
            ) + fadeIn(animationSpec = tween(300))).togetherWith(
                scaleOut(
                    animationSpec = tween(200),
                    targetScale = 0.7f
                ) + fadeOut(animationSpec = tween(200))
            )
        },
        label = "timerCenterContent"
    ) { targetState ->
        when (targetState) {
            SessionState.BREAK_PENDING -> InCircleActionButton(
                icon = Icons.Default.PlayArrow,
                label = stringResource(R.string.btn_start_break),
                testTag = "startBreakButton",
                onClick = onStartBreak
            )
            SessionState.BREAK_FINISHED -> InCircleActionButton(
                icon = Icons.Default.Check,
                label = stringResource(R.string.btn_finish_break),
                testTag = "finishBreakButton",
                onClick = onFinishBreak
            )
            else -> TimeText(
                remainingSeconds = session.remainingSeconds,
                state = targetState
            )
        }
    }
}

@Composable
private fun InCircleActionButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(150.dp)
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha),
                    shape = CircleShape
                )
        )
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .testTag(testTag),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun TimeText(
    remainingSeconds: Int,
    state: SessionState
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formatted = String.format("%02d:%02d", minutes, seconds)

    Text(
        text = formatted,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("timerCountdownText")
    )
}

@Composable
private fun PhaseDescriptionText(session: TimerSession) {
    val description = when (session.state) {
        SessionState.READY -> stringResource(R.string.desc_ready)
        SessionState.WORKING -> formatWorkingSubtitle(session)
        SessionState.BREAK_PENDING -> if (session.nextBreakType == NextBreakType.LONG) {
            stringResource(R.string.desc_break_pending_long)
        } else {
            stringResource(R.string.desc_break_pending_short)
        }
        SessionState.SHORT_BREAK -> stringResource(R.string.desc_short_break)
        SessionState.LONG_BREAK -> stringResource(R.string.desc_long_break)
        SessionState.BREAK_FINISHED -> stringResource(R.string.desc_break_finished)
        SessionState.PAUSED -> stringResource(R.string.status_paused)
    }

    Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .testTag("timerDescriptionText")
    )
}

@Composable
private fun formatWorkingSubtitle(session: TimerSession): String {
    val longMinutes = session.secondsUntilLongBreak / 60
    val longSeconds = session.secondsUntilLongBreak % 60
    val longFormatted = String.format("%02d:%02d", longMinutes, longSeconds)
    val nextLongPrefix = stringResource(R.string.desc_working_next_long)
    return "$nextLongPrefix $longFormatted"
}
