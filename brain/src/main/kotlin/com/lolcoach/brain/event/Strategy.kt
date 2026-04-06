package com.lolcoach.brain.event

import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import com.lolcoach.bridge.model.liveclient.GameSnapshot

interface Strategy {
    /** Game modes in which this strategy is active. */
    val applicableGameModes: Set<GameMode>
        get() = setOf(GameMode.SUMMONERS_RIFT, GameMode.ARAM, GameMode.ARAM_MAYHEM, GameMode.UNKNOWN)

    fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent>
    fun evaluateChampSelect(session: ChampSelectSession, state: GameState): List<GameEvent> = emptyList()

    fun isApplicable(gameMode: GameMode): Boolean = gameMode in applicableGameModes
}
