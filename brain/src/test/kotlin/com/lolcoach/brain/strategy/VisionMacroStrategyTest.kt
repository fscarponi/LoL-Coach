package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.*
import kotlin.test.Test
import kotlin.test.assertTrue

class VisionMacroStrategyTest {

    private val strategy = VisionMacroStrategy()

    private fun makeSnapshot(gameTime: Double, items: List<PlayerItem> = emptyList()): GameSnapshot {
        return GameSnapshot(
            activePlayer = ActivePlayer(summonerName = "TestPlayer"),
            allPlayers = listOf(
                Player(summonerName = "TestPlayer", items = items)
            ),
            gameData = GameData(gameTime = gameTime)
        )
    }

    @Test
    fun `suggests warding when ward charges available after 10 min`() {
        val snapshot = makeSnapshot(
            gameTime = 700.0,
            items = listOf(PlayerItem(itemID = 3340, count = 2, displayName = "Stealth Ward"))
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.any { it is GameEvent.VisionNeeded })
    }

    @Test
    fun `no ward suggestion before 10 min`() {
        val snapshot = makeSnapshot(
            gameTime = 300.0,
            items = listOf(PlayerItem(itemID = 3340, count = 3, displayName = "Stealth Ward"))
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.none { it is GameEvent.VisionNeeded })
    }

    @Test
    fun `suggests control ward when missing after 10 min`() {
        val snapshot = makeSnapshot(gameTime = 700.0)
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.any { it is GameEvent.ItemSuggestion && it.item == "Control Ward" })
    }

    @Test
    fun `no control ward suggestion when already has one`() {
        val snapshot = makeSnapshot(
            gameTime = 700.0,
            items = listOf(PlayerItem(itemID = 2055, count = 1, displayName = "Control Ward"))
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.none { it is GameEvent.ItemSuggestion && it.item == "Control Ward" })
    }

    @Test
    fun `suggests oracle lens after 15 min if still has stealth ward`() {
        val snapshot = makeSnapshot(
            gameTime = 1000.0,
            items = listOf(PlayerItem(itemID = 3340, count = 2, displayName = "Stealth Ward"))
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.any { it is GameEvent.ItemSuggestion && it.item == "Oracle Lens" })
    }
}
