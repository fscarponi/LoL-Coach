package com.lolcoach.brain.event

import com.lolcoach.brain.state.GameState
import com.lolcoach.brain.state.GameStateMachine
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EventProcessor(
    private val scope: CoroutineScope,
    private val stateMachine: GameStateMachine,
    private val strategies: List<Strategy> = emptyList()
) {
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val emittedEvents = mutableSetOf<String>()

    fun processGameSnapshot(snapshot: GameSnapshot) {
        stateMachine.onGameSnapshotReceived(snapshot)
        val currentState = stateMachine.state.value

        if (currentState is GameState.InGame) {
            val newEvents = strategies.flatMap { it.evaluate(snapshot, currentState) }
            scope.launch {
                for (event in newEvents) {
                    val key = event.deduplicationKey()
                    if (emittedEvents.add(key)) {
                        _events.emit(event)
                    }
                }
            }
        }
    }

    fun processChampSelect(session: ChampSelectSession) {
        stateMachine.onChampSelectUpdate(session)
        val currentState = stateMachine.state.value

        if (currentState is GameState.ChampSelect) {
            val snapshot = GameSnapshot() // empty snapshot for champ select strategies
            val newEvents = strategies.flatMap { it.evaluateChampSelect(session, currentState) }
            scope.launch {
                for (event in newEvents) {
                    val key = event.deduplicationKey()
                    if (emittedEvents.add(key)) {
                        _events.emit(event)
                    }
                }
            }
        }
    }

    /**
     * Emette un evento esterno (es. da LLM) nel flusso principale.
     */
    suspend fun emitEvent(event: GameEvent) {
        val key = event.deduplicationKey()
        if (emittedEvents.add(key)) {
            _events.emit(event)
        }
    }

    fun resetDeduplication() {
        emittedEvents.clear()
    }

    private fun GameEvent.deduplicationKey(): String = when (this) {
        is GameEvent.EnemySupportSelected -> "enemy_support_$championName"
        is GameEvent.Level2Approaching -> "level2_approaching_$minionsNeeded"
        is GameEvent.Level2Reached -> "level2_reached"
        is GameEvent.VisionNeeded -> "vision_$reason"
        is GameEvent.DragonTimerWarning -> "dragon_$seconds"
        is GameEvent.ItemSuggestion -> "item_$item"
        is GameEvent.SynergyAdvice -> "synergy_$advice"
        is GameEvent.GenericTip -> "tip_$message"
        is GameEvent.LlmAnalysis -> "llm_${section}_$content"
    }
}
