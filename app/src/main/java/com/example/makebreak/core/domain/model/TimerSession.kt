package com.example.makebreak.core.domain.model

import com.example.makebreak.core.domain.enums.NextBreakType
import com.example.makebreak.core.domain.enums.SessionState

data class TimerSession(
    val state: SessionState = SessionState.READY,
    val remainingSeconds: Int = 0,
    val totalPhaseSeconds: Int = 0,
    val nextBreakType: NextBreakType = NextBreakType.SHORT,
    val secondsUntilShortBreak: Int = 0,
    val secondsUntilLongBreak: Int = 0,
    val isPaused: Boolean = false,
    val isBreakAnnouncementsMuted: Boolean = false
) {
    val progress: Float
        get() = if (totalPhaseSeconds > 0) {
            1f - (remainingSeconds.toFloat() / totalPhaseSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
