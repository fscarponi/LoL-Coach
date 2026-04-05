package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EarlyGameStrategyTest {

    private val strategy = EarlyGameStrategy()

    @Test
    fun `level 2 reached event when player is level 2 early`() {
        val snapshot = GameSnapshot(
            activePlayer = ActivePlayer(level = 2, summonerName = "TestPlayer"),
            gameData = GameData(gameTime = 120.0)
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.any { it is GameEvent.Level2Reached })
    }

    @Test
    fun `no level 2 event after 5 minutes`() {
        val snapshot = GameSnapshot(
            activePlayer = ActivePlayer(level = 2, summonerName = "TestPlayer"),
            gameData = GameData(gameTime = 350.0)
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `no events when activePlayer is null`() {
        val snapshot = GameSnapshot(gameData = GameData(gameTime = 60.0))
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `no events when gameData is null`() {
        val snapshot = GameSnapshot(activePlayer = ActivePlayer(level = 1))
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `level 2 approaching event at correct timing`() {
        // At ~95 seconds: 1 wave arrived (95-65)/30 = 1 wave = 6 minions
        // 9 - 6 = 3 minions needed -> should trigger
        val snapshot = GameSnapshot(
            activePlayer = ActivePlayer(level = 1, summonerName = "TestPlayer"),
            allPlayers = listOf(
                Player(summonerName = "TestPlayer", scores = PlayerScores(creepScore = 5))
            ),
            gameData = GameData(gameTime = 95.0)
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.any { it is GameEvent.Level2Approaching })
    }
}
