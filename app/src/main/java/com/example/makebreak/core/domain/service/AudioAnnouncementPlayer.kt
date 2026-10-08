package com.example.makebreak.core.domain.service

interface AudioAnnouncementPlayer {

    /**
     * Plays a random break start voice announcement without immediate repetition.
     * Invoked by: TimerEngine, SettingsViewModel.
     */
    fun playBreakStart(volume: Float = 1.0f)

    /**
     * Plays a random break finished voice announcement without immediate repetition.
     * Invoked by: TimerEngine, SettingsViewModel.
     */
    fun playBreakFinished(volume: Float = 1.0f)

    /**
     * Stops currently playing voice announcement.
     * Invoked by: TimerEngine, SettingsViewModel.
     */
    fun stop()
}
