package com.example.makebreak.feature.settings.presentation.settings.ScreenComponents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
 * Settings card displaying system battery optimization status and instructions for reliable background execution.
 *
 * Usage:
 * isIgnoringBatteryOptimizations: whether the app is whitelisted from battery restrictions.
 * onRequestBatteryOptimization: callback requesting battery optimization exemption.
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * Section header, current status icon with label, explanatory text, exemption request button.
 *
 * Used In:
 * SettingsScreen
 */
@Composable
fun BatteryOptimizationCard(
    isIgnoringBatteryOptimizations: Boolean,
    onRequestBatteryOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("batteryOptimizationCard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CardHeader()
            Spacer(modifier = Modifier.height(12.dp))
            StatusRow(isIgnoring = isIgnoringBatteryOptimizations)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_battery_optimization_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isIgnoringBatteryOptimizations) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestBatteryOptimization,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("requestBatteryOptimizationButton")
                ) {
                    Text(text = stringResource(R.string.settings_battery_request_button))
                }
            }
        }
    }
}

@Composable
private fun CardHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.BatteryChargingFull,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.settings_section_battery),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun StatusRow(isIgnoring: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (icon, textRes, tint) = if (isIgnoring) {
            Triple(
                Icons.Default.CheckCircle,
                R.string.settings_battery_disabled_status,
                MaterialTheme.colorScheme.secondary
            )
        } else {
            Triple(
                Icons.Default.Warning,
                R.string.settings_battery_enabled_status,
                MaterialTheme.colorScheme.error
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = tint
        )
    }
}
