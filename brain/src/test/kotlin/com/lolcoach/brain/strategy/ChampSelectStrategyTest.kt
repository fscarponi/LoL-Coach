package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.lcu.ChampSelectPlayerSelection
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChampSelectStrategyTest {

    private val strategy = ChampSelectStrategy()

    @Test
    fun `detects enemy support selection`() {
        val session = ChampSelectSession(
            theirTeam = listOf(
                ChampSelectPlayerSelection(assignedPosition = "utility", championId = 412) // Thresh
            )
        )
        val events = strategy.evaluateChampSelect(session, GameState.ChampSelect)
        val enemyEvent = events.filterIsInstance<GameEvent.EnemySupportSelected>()
        assertEquals(1, enemyEvent.size)
        assertEquals("Thresh", enemyEvent[0].championName)
    }

    @Test
    fun `no enemy support event when championId is 0`() {
        val session = ChampSelectSession(
            theirTeam = listOf(
                ChampSelectPlayerSelection(assignedPosition = "utility", championId = 0)
            )
        )
        val events = strategy.evaluateChampSelect(session, GameState.ChampSelect)
        assertTrue(events.none { it is GameEvent.EnemySupportSelected })
    }

    @Test
    fun `good synergy detected`() {
        val session = ChampSelectSession(
            myTeam = listOf(
                ChampSelectPlayerSelection(assignedPosition = "bottom", championId = 222),  // Jinx
                ChampSelectPlayerSelection(assignedPosition = "utility", championId = 89)   // Leona
            )
        )
        val events = strategy.evaluateChampSelect(session, GameState.ChampSelect)
        val synergyEvent = events.filterIsInstance<GameEvent.SynergyAdvice>()
        assertTrue(synergyEvent.any { it.advice.contains("Great synergy") })
    }

    @Test
    fun `suggests better synergies when not optimal`() {
        val session = ChampSelectSession(
            myTeam = listOf(
                ChampSelectPlayerSelection(assignedPosition = "bottom", championId = 222),  // Jinx
                ChampSelectPlayerSelection(assignedPosition = "utility", championId = 63)   // Brand
            )
        )
        val events = strategy.evaluateChampSelect(session, GameState.ChampSelect)
        val synergyEvent = events.filterIsInstance<GameEvent.SynergyAdvice>()
        assertTrue(synergyEvent.any { it.advice.contains("best synergies") })
    }

    @Test
    fun `empty session produces no events`() {
        val session = ChampSelectSession()
        val events = strategy.evaluateChampSelect(session, GameState.ChampSelect)
        assertTrue(events.isEmpty())
    }
}
