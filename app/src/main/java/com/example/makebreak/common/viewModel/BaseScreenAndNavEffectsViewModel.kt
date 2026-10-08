package com.example.makebreak.common.viewModel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

open class BaseScreenAndNavEffectsViewModel<ScreenEffectType, NavEffectType> : BaseEffectViewModel<ScreenEffectType>() {

    private val _navEffect = Channel<NavEffectType>()
    val navEffect = _navEffect.receiveAsFlow()

    protected fun sendNavEffect(effect: NavEffectType) {
        viewModelScope.launch {
            _navEffect.send(effect)
        }
    }
}
