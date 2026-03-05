package com.dynamicisland.overlay

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object IslandEventBus {
    private val _events = MutableSharedFlow<IslandState>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<IslandState> = _events

    fun post(state: IslandState) {
        _events.tryEmit(state)
    }

    fun dismiss() {
        _events.tryEmit(IslandState.Idle)
    }
}
