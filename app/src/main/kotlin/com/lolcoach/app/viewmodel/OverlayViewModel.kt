package com.lolcoach.app.viewmodel

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OverlayViewModel(
    private val scope: CoroutineScope,
    private val gameEvents: SharedFlow<GameEvent>,
    private val gameState: StateFlow<GameState>
) {
    companion object {
        const val MAX_VISIBLE_EVENTS = 5
        const val EVENT_DISPLAY_DURATION_MS = 8000L
    }

    private val _visibleEvents = MutableStateFlow<List<TimedEvent>>(emptyList())
    val visibleEvents: StateFlow<List<TimedEvent>> = _visibleEvents.asStateFlow()

    val currentState: StateFlow<GameState> = gameState

    fun start() {
        // Collect game events
        scope.launch {
            gameEvents.collect { event ->
                addEvent(event)
            }
        }

        // Cleanup expired events
        scope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                _visibleEvents.value = _visibleEvents.value.filter {
                    now - it.timestamp < EVENT_DISPLAY_DURATION_MS
                }
            }
        }
    }

    private fun addEvent(event: GameEvent) {
        val timed = TimedEvent(event, System.currentTimeMillis())
        val current = _visibleEvents.value.toMutableList()
        current.add(0, timed)
        if (current.size > MAX_VISIBLE_EVENTS) {
            current.removeAt(current.lastIndex)
        }
        _visibleEvents.value = current
    }

    data class TimedEvent(
        val event: GameEvent,
        val timestamp: Long
    )
}
