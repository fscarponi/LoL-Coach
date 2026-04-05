package com.lolcoach.brain.event

import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import com.lolcoach.bridge.model.liveclient.GameSnapshot

interface Strategy {
    fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent>
    fun evaluateChampSelect(session: ChampSelectSession, state: GameState): List<GameEvent> = emptyList()
}
