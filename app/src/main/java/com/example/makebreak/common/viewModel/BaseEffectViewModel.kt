package com.example.makebreak.common.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

open class BaseEffectViewModel<EffectType> : ViewModel() {

    private val _effect = Channel<EffectType>()
    val effect = _effect.receiveAsFlow()

    protected fun sendEffect(effect: EffectType) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}
