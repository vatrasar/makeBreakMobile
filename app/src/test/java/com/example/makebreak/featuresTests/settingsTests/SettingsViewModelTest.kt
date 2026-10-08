package com.example.makebreak.featuresTests.settingsTests

import android.content.Context
import android.os.PowerManager
import app.cash.turbine.test
import com.example.makebreak.core.domain.model.UserSettings
import com.example.makebreak.core.domain.repository.UserSettingsRepository
import com.example.makebreak.core.domain.service.AudioAnnouncementPlayer
import com.example.makebreak.feature.settings.presentation.settings.SettingsEffect
import com.example.makebreak.feature.settings.presentation.settings.SettingsEvent
import com.example.makebreak.feature.settings.presentation.settings.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: UserSettingsRepository
    private lateinit var audioPlayer: AudioAnnouncementPlayer
    private lateinit var context: Context
    private lateinit var powerManager: PowerManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        audioPlayer = mockk(relaxed = true)
        context = mockk(relaxed = true)
        powerManager = mockk(relaxed = true)

        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.packageName } returns "com.example.makebreak"
        every { powerManager.isIgnoringBatteryOptimizations(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadSettings_whenRepositoryReturnsSettings_updatesUiState() = runTest(testDispatcher) {
        val customSettings = UserSettings(
            shortBreakIntervalMinutes = 25,
            shortBreakDurationSeconds = 30,
            longBreakIntervalMinutes = 90,
            longBreakDurationSeconds = 600,
            isVoiceEnabled = true,
            voiceVolume = 0.7f
        )
        coEvery { repository.getUserSettings() } returns customSettings

        val viewModel = SettingsViewModel(repository, audioPlayer, context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(25, viewModel.state.value.shortBreakIntervalMinutes)
        assertEquals(30, viewModel.state.value.shortBreakDurationSeconds)
        assertEquals(90, viewModel.state.value.longBreakIntervalMinutes)
        assertEquals(600, viewModel.state.value.longBreakDurationSeconds)
        assertEquals(0.7f, viewModel.state.value.voiceVolume, 0.001f)
    }

    @Test
    fun onEvent_onShortIntervalChange_updatesShortIntervalMinutes() = runTest(testDispatcher) {
        coEvery { repository.getUserSettings() } returns UserSettings()
        val viewModel = SettingsViewModel(repository, audioPlayer, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnShortIntervalChange(35))

        assertEquals(35, viewModel.state.value.shortBreakIntervalMinutes)
    }

    @Test
    fun onEvent_testStartVoice_triggersAudioPlayerPlayBreakStart() = runTest(testDispatcher) {
        coEvery { repository.getUserSettings() } returns UserSettings(voiceVolume = 0.8f)
        val viewModel = SettingsViewModel(repository, audioPlayer, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.OnTestStartVoice)

        verify(exactly = 1) { audioPlayer.playBreakStart(0.8f) }
    }

    @Test
    fun onEvent_onSaveSettings_callsRepositorySaveAndEmitsSnackbarEffect() = runTest(testDispatcher) {
        coEvery { repository.getUserSettings() } returns UserSettings()
        val viewModel = SettingsViewModel(repository, audioPlayer, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(SettingsEvent.OnSaveSettings)
            testDispatcher.scheduler.advanceUntilIdle()

            val effect = awaitItem()
            assert(effect is SettingsEffect.ShowSnackbar)
            coVerify(exactly = 1) { repository.saveUserSettings(any()) }
        }
    }
}
