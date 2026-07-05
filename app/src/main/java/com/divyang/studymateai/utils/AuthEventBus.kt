package com.divyang.studymateai.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow


object AuthEventBus {
    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    suspend fun notifySessionExpired() {
        _events.emit(AuthEvent.SessionExpired)
    }
}

sealed class AuthEvent {
    object SessionExpired : AuthEvent()
}