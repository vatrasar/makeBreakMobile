package com.example.makebreak.feature.settings.presentation.settings.ScreenComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.makebreak.R

/**
 * Settings configuration card for adjusting short and long break intervals and durations.
 *
 * Usage:
 * shortInterval: minutes between short breaks.
 * shortDuration: duration in seconds of short breaks.
 * longInterval: minutes between long breaks.
 * longDuration: duration in seconds of long breaks.
 * onShortIntervalChange: callback when short interval changes.
 * onShortDurationChange: callback when short duration changes.
 * onLongIntervalChange: callback when long interval changes.
 * onLongDurationChange: callback when long duration changes.
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * Section header, value stepper controls for intervals and durations.
 *
 * Used In:
 * SettingsScreen
 */
@Composable
fun IntervalSettingsCard(
    shortInterval: Int,
    shortDuration: Int,
    longInterval: Int,
    longDuration: Int,
    onShortIntervalChange: (Int) -> Unit,
    onShortDurationChange: (Int) -> Unit,
    onLongIntervalChange: (Int) -> Unit,
    onLongDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("intervalSettingsCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardHeader()
            Spacer(modifier = Modifier.height(16.dp))
            ValueStepperRow(
                label = stringResource(R.string.settings_short_interval_label),
                value = "$shortInterval min",
                onDecrease = { onShortIntervalChange(shortInterval - 5) },
                onIncrease = { onShortIntervalChange(shortInterval + 5) },
                testTagPrefix = "shortInterval"
            )
            Spacer(modifier = Modifier.height(12.dp))
            ValueStepperRow(
                label = stringResource(R.string.settings_short_duration_label),
                value = "$shortDuration sec",
                onDecrease = { onShortDurationChange(shortDuration - 5) },
                onIncrease = { onShortDurationChange(shortDuration + 5) },
                testTagPrefix = "shortDuration"
            )
            Spacer(modifier = Modifier.height(12.dp))
            ValueStepperRow(
                label = stringResource(R.string.settings_long_interval_label),
                value = "$longInterval min",
                onDecrease = { onLongIntervalChange(longInterval - 10) },
                onIncrease = { onLongIntervalChange(longInterval + 10) },
                testTagPrefix = "longInterval"
            )
            Spacer(modifier = Modifier.height(12.dp))
            ValueStepperRow(
                label = stringResource(R.string.settings_long_duration_label),
                value = "${longDuration / 60}m ${longDuration % 60}s",
                onDecrease = { onLongDurationChange(longDuration - 30) },
                onIncrease = { onLongDurationChange(longDuration + 30) },
                testTagPrefix = "longDuration"
            )
        }
    }
}

@Composable
private fun CardHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.settings_section_intervals),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ValueStepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    testTagPrefix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = onDecrease,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("${testTagPrefix}MinusButton"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = null)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("${testTagPrefix}ValueText")
            )
            FilledIconButton(
                onClick = onIncrease,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("${testTagPrefix}PlusButton"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
            }
        }
    }
}
