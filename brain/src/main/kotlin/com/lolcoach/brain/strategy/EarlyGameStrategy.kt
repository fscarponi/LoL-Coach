package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.event.Strategy
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot

class EarlyGameStrategy : Strategy {

    override val applicableGameModes = setOf(GameMode.SUMMONERS_RIFT)

    companion object {
        // Level 2 requires ~280 XP = first wave (6 melee + 3 caster) + 1 melee from second wave
        // That's approximately 7 minions total for bot lane (shared XP with ADC)
        const val LEVEL_2_MINION_THRESHOLD = 9
        const val LEVEL_2_WARNING_THRESHOLD = 7
    }

    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val activePlayer = snapshot.activePlayer ?: return events
        val gameTime = snapshot.gameData?.gameTime ?: return events

        // Only relevant in early game (first 5 minutes)
        if (gameTime > 300.0) return events

        val level = activePlayer.level

        if (level < 2) {
            // Find our player in allPlayers to get creep score
            val ourPlayer = snapshot.allPlayers.find {
                it.summonerName == activePlayer.summonerName ||
                    it.riotId == activePlayer.riotId
            }
            val creepScore = ourPlayer?.scores?.creepScore ?: 0

            // Use total lane minions (approximation based on game time)
            // First wave spawns at 1:05, waves every 30s, 6 minions per wave (+ 1 cannon every 3rd)
            val wavesArrived = ((gameTime - 65.0) / 30.0).toInt().coerceAtLeast(0)
            val totalLaneMinions = (wavesArrived * 6) + (wavesArrived / 3)

            val minionsNeeded = (LEVEL_2_MINION_THRESHOLD - totalLaneMinions).coerceAtLeast(0)

            if (minionsNeeded in 1..3) {
                events.add(GameEvent.Level2Approaching(minionsNeeded))
            }
        }

        if (level == 2 && gameTime < 180.0) {
            events.add(GameEvent.Level2Reached)
        }

        return events
    }
}
