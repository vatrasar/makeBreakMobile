package com.example.makebreak.feature.settings.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.makebreak.R
import com.example.makebreak.feature.settings.presentation.settings.ScreenComponents.BatteryOptimizationCard
import com.example.makebreak.feature.settings.presentation.settings.ScreenComponents.IntervalSettingsCard
import com.example.makebreak.feature.settings.presentation.settings.ScreenComponents.SoundSettingsCard
import kotlinx.coroutines.flow.Flow

/**
 * Settings configuration screen for customizing intervals, sound preferences, and battery optimization.
 *
 * Available Functionalities:
 * Modify short and long break intervals and durations, configure audio announcement volume and previews,
 * request battery optimization exemptions, save configurations, and restore default settings.
 *
 * Key UI elements:
 * TopAppBar, IntervalSettingsCard, SoundSettingsCard, BatteryOptimizationCard, Save Settings button, Reset Defaults button.
 *
 * Navigation:
 * - Navigate From: AppBottomBar (Settings tab).
 * - Navigate To: System Battery Optimization settings (external system Intent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    effects: Flow<SettingsEffect>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onEvent(SettingsEvent.OnRefreshBatteryStatus)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                SettingsEffect.OpenBatteryOptimizationSettings -> openBatterySettings(context)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settingsScaffold"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                modifier = Modifier.testTag("settingsTopAppBar")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        SettingsContent(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settingsLazyColumn"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            IntervalSettingsCard(
                shortInterval = state.shortBreakIntervalMinutes,
                shortDuration = state.shortBreakDurationSeconds,
                longInterval = state.longBreakIntervalMinutes,
                longDuration = state.longBreakDurationSeconds,
                onShortIntervalChange = { onEvent(SettingsEvent.OnShortIntervalChange(it)) },
                onShortDurationChange = { onEvent(SettingsEvent.OnShortDurationChange(it)) },
                onLongIntervalChange = { onEvent(SettingsEvent.OnLongIntervalChange(it)) },
                onLongDurationChange = { onEvent(SettingsEvent.OnLongDurationChange(it)) }
            )
        }
        item {
            SoundSettingsCard(
                isVoiceEnabled = state.isVoiceEnabled,
                volume = state.voiceVolume,
                onVoiceEnabledChange = { onEvent(SettingsEvent.OnVoiceEnabledChange(it)) },
                onVolumeChange = { onEvent(SettingsEvent.OnVoiceVolumeChange(it)) },
                onTestStartVoice = { onEvent(SettingsEvent.OnTestStartVoice) },
                onTestFinishedVoice = { onEvent(SettingsEvent.OnTestFinishedVoice) }
            )
        }
        item {
            BatteryOptimizationCard(
                isIgnoringBatteryOptimizations = state.isIgnoringBatteryOptimizations,
                onRequestBatteryOptimization = { onEvent(SettingsEvent.OnRequestBatteryOptimization) }
            )
        }
        item {
            SettingsActionButtons(
                onSave = { onEvent(SettingsEvent.OnSaveSettings) },
                onReset = { onEvent(SettingsEvent.OnResetDefaults) }
            )
        }
    }
}

@Composable
private fun SettingsActionButtons(
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("resetDefaultsButton")
        ) {
            Text(text = stringResource(R.string.settings_reset_defaults))
        }
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("saveSettingsButton")
        ) {
            Text(text = stringResource(R.string.settings_save))
        }
    }
}

private fun openBatterySettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    try {
        context.startActivity(intent)
    } catch (exception: Exception) {
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(fallbackIntent)
    }
}
