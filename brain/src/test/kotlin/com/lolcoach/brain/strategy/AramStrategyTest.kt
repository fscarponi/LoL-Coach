package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AramStrategyTest {

    private val strategy = AramStrategy()

    private fun makeSnapshot(
        gameTime: Double = 300.0,
        enemyChampions: List<String> = emptyList(),
        allyChampions: List<String> = emptyList(),
        summonerName: String = "TestPlayer"
    ): GameSnapshot {
        val activePlayer = ActivePlayer(summonerName = summonerName, level = 5)
        val allies = allyChampions.map { Player(championName = it, team = "ORDER", summonerName = it) }
        val enemies = enemyChampions.map { Player(championName = it, team = "CHAOS", summonerName = it) }
        val ourPlayer = Player(championName = "Lulu", team = "ORDER", summonerName = summonerName)
        return GameSnapshot(
            activePlayer = activePlayer,
            allPlayers = listOf(ourPlayer) + allies + enemies,
            gameData = GameData(gameMode = "ARAM", gameTime = gameTime, mapNumber = 12)
        )
    }

    @Test
    fun `applicableGameModes includes ARAM and ARAM_MAYHEM`() {
        assertTrue(strategy.isApplicable(GameMode.ARAM))
        assertTrue(strategy.isApplicable(GameMode.ARAM_MAYHEM))
        assertTrue(!strategy.isApplicable(GameMode.SUMMONERS_RIFT))
    }

    @Test
    fun `detects poke threats from enemy team`() {
        val snapshot = makeSnapshot(
            gameTime = 300.0,
            enemyChampions = listOf("Xerath", "Lux")
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val pokeWarnings = events.filterIsInstance<GameEvent.AramPokeWarning>()
        assertEquals(2, pokeWarnings.size)
        assertTrue(pokeWarnings.any { it.enemyChampion == "Xerath" })
        assertTrue(pokeWarnings.any { it.enemyChampion == "Lux" })
    }

    @Test
    fun `no poke warning for non-poke champions`() {
        val snapshot = makeSnapshot(
            gameTime = 300.0,
            enemyChampions = listOf("Garen", "Darius")
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val pokeWarnings = events.filterIsInstance<GameEvent.AramPokeWarning>()
        assertTrue(pokeWarnings.isEmpty())
    }

    @Test
    fun `early game teamfight tip before 3 minutes`() {
        val snapshot = makeSnapshot(gameTime = 100.0)
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val tips = events.filterIsInstance<GameEvent.AramTeamfightTip>()
        assertTrue(tips.any { it.tip.contains("Early game") })
    }

    @Test
    fun `enemy engagers warning when 2 or more`() {
        val snapshot = makeSnapshot(
            gameTime = 300.0,
            enemyChampions = listOf("Malphite", "Leona")
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val tips = events.filterIsInstance<GameEvent.AramTeamfightTip>()
        assertTrue(tips.any { it.tip.contains("engager") })
    }

    @Test
    fun `health pack reminder at correct time`() {
        val snapshot = makeSnapshot(gameTime = 125.0) // within 120+10 window
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val reminders = events.filterIsInstance<GameEvent.AramHealthPackReminder>()
        assertTrue(reminders.isNotEmpty())
    }

    @Test
    fun `no health pack reminder before threshold`() {
        val snapshot = makeSnapshot(gameTime = 60.0)
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val reminders = events.filterIsInstance<GameEvent.AramHealthPackReminder>()
        assertTrue(reminders.isEmpty())
    }

    @Test
    fun `mid-game advice after 8 minutes`() {
        val snapshot = makeSnapshot(gameTime = 500.0)
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val tips = events.filterIsInstance<GameEvent.AramTeamfightTip>()
        assertTrue(tips.any { it.tip.contains("Mid-game") })
    }

    @Test
    fun `snowball advice when player has Mark spell`() {
        val activePlayer = ActivePlayer(summonerName = "TestPlayer", level = 5)
        val ourPlayer = Player(
            championName = "Lulu",
            team = "ORDER",
            summonerName = "TestPlayer",
            summonerSpells = SummonerSpells(
                summonerSpellOne = SummonerSpell(displayName = "Mark"),
                summonerSpellTwo = SummonerSpell(displayName = "Flash")
            )
        )
        val snapshot = GameSnapshot(
            activePlayer = activePlayer,
            allPlayers = listOf(ourPlayer),
            gameData = GameData(gameMode = "ARAM", gameTime = 300.0, mapNumber = 12)
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        val snowball = events.filterIsInstance<GameEvent.AramSnowballAdvice>()
        assertTrue(snowball.isNotEmpty())
    }

    @Test
    fun `returns empty when no active player`() {
        val snapshot = GameSnapshot(
            gameData = GameData(gameMode = "ARAM", gameTime = 300.0, mapNumber = 12)
        )
        val events = strategy.evaluate(snapshot, GameState.InGame)
        assertTrue(events.isEmpty())
    }
}
