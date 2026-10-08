package com.example.makebreak.feature.settings.presentation.settings.ScreenComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.makebreak.R

/**
 * Settings configuration card for audio announcements and alarm stream playback.
 *
 * Usage:
 * isVoiceEnabled: boolean indicating whether announcements are active.
 * volume: current playback volume fraction between 0.0 and 1.0.
 * onVoiceEnabledChange: callback when switch is toggled.
 * onVolumeChange: callback when slider moves.
 * onTestStartVoice: callback to test break start audio.
 * onTestFinishedVoice: callback to test break finish audio.
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * Voice announcement toggle switch, volume slider, audio preview buttons, alarm stream notice.
 *
 * Used In:
 * SettingsScreen
 */
@Composable
fun SoundSettingsCard(
    isVoiceEnabled: Boolean,
    volume: Float,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onTestStartVoice: () -> Unit,
    onTestFinishedVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("soundSettingsCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardHeader()
            Spacer(modifier = Modifier.height(16.dp))
            VoiceToggleRow(
                isVoiceEnabled = isVoiceEnabled,
                onVoiceEnabledChange = onVoiceEnabledChange
            )
            Spacer(modifier = Modifier.height(12.dp))
            VolumeSliderSection(
                volume = volume,
                isEnabled = isVoiceEnabled,
                onVolumeChange = onVolumeChange
            )
            Spacer(modifier = Modifier.height(16.dp))
            TestButtonsRow(
                isEnabled = isVoiceEnabled,
                onTestStart = onTestStartVoice,
                onTestFinished = onTestFinishedVoice
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_channel_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CardHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.settings_section_sound),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun VoiceToggleRow(
    isVoiceEnabled: Boolean,
    onVoiceEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.settings_voice_enabled),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = isVoiceEnabled,
            onCheckedChange = onVoiceEnabledChange,
            modifier = Modifier.testTag("voiceEnabledSwitch")
        )
    }
}

@Composable
private fun VolumeSliderSection(
    volume: Float,
    isEnabled: Boolean,
    onVolumeChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_voice_volume),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("volumePercentText")
            )
        }
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            enabled = isEnabled,
            valueRange = 0f..1f,
            modifier = Modifier.testTag("voiceVolumeSlider")
        )
    }
}

@Composable
private fun TestButtonsRow(
    isEnabled: Boolean,
    onTestStart: () -> Unit,
    onTestFinished: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onTestStart,
            enabled = isEnabled,
            modifier = Modifier
                .weight(1f)
                .testTag("testStartVoiceButton")
        ) {
            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null)
            Text(
                text = stringResource(R.string.settings_test_start_voice),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        OutlinedButton(
            onClick = onTestFinished,
            enabled = isEnabled,
            modifier = Modifier
                .weight(1f)
                .testTag("testFinishVoiceButton")
        ) {
            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null)
            Text(
                text = stringResource(R.string.settings_test_finish_voice),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
