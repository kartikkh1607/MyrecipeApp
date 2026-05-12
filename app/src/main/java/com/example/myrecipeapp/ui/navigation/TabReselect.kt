package com.example.myrecipeapp.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Channel for "tab reselected" events — the user tapped a bottom-nav tab that
 * is already the active destination. Each tab screen subscribes to its own
 * route singleton via the [events] flow and reacts (typically: animate scroll
 * to top, Instagram/YouTube-style).
 *
 * Owned by MainScreen, consumed by tab screens via [LocalTabReselectEvents].
 *
 * `extraBufferCapacity = 1` + `tryEmit` keeps emission non-blocking. Events
 * are dropped if no screen is collecting, which is intentional: a reselect
 * that fired while the screen was off-screen shouldn't replay later.
 */
class TabReselectEvents {
    private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 1)
    val events: SharedFlow<Any> = _events.asSharedFlow()

    fun emit(route: Any) {
        _events.tryEmit(route)
    }
}

val LocalTabReselectEvents = staticCompositionLocalOf<TabReselectEvents> {
    error("LocalTabReselectEvents not provided. Wrap your tree in MainScreen's CompositionLocalProvider.")
}
