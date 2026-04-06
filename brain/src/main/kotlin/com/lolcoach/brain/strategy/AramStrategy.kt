package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.event.Strategy
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot

/**
 * Strategy specific to ARAM and ARAM Mayhem.
 * Advice on teamfights, poke, health packs and snowballs.
 */
class AramStrategy : Strategy {

    override val applicableGameModes = setOf(GameMode.ARAM, GameMode.ARAM_MAYHEM)

    companion object {
        // Champions known for heavy poke in ARAM
        val POKE_CHAMPIONS = setOf(
            "Lux", "Xerath", "Ziggs", "Vel'Koz", "Jayce", "Nidalee",
            "Ezreal", "Varus", "Kog'Maw", "Zoe", "Seraphine", "Senna",
            "Caitlyn", "Jhin", "Ashe", "Morgana", "Brand", "Zyra"
        )

        // Champions with strong engage in ARAM
        val ENGAGE_CHAMPIONS = setOf(
            "Malphite", "Amumu", "Leona", "Nautilus", "Alistar", "Rakan",
            "Ornn", "Sejuani", "Zac", "Rell", "Thresh", "Blitzcrank"
        )

        // Intervals for reminders (in seconds of game time)
        const val HEALTH_PACK_FIRST_REMINDER = 120.0  // 2 min
        const val HEALTH_PACK_INTERVAL = 300.0         // every 5 min
        const val EARLY_GAME_END = 180.0               // 3 min
        const val MID_GAME_START = 480.0               // 8 min
    }

    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val activePlayer = snapshot.activePlayer ?: return events
        val gameTime = snapshot.gameData?.gameTime ?: return events

        // Poke warning: signal enemy champions with heavy poke
        detectPokeThreats(snapshot, events)

        // Periodic health pack reminder
        checkHealthPackReminder(gameTime, events)

        // Teamfight advice based on composition
        evaluateTeamfightAdvice(snapshot, gameTime, events)

        // Snowball advice for support
        evaluateSnowballUsage(snapshot, gameTime, events)

        return events
    }

    private fun detectPokeThreats(snapshot: GameSnapshot, events: MutableList<GameEvent>) {
        val activePlayer = snapshot.activePlayer ?: return
        val ourPlayer = snapshot.allPlayers.find {
            it.summonerName == activePlayer.summonerName || it.riotId == activePlayer.riotId
        } ?: return
        val ourTeam = ourPlayer.team

        snapshot.allPlayers
            .filter { it.team != ourTeam }
            .filter { it.championName in POKE_CHAMPIONS }
            .forEach { enemy ->
                events.add(GameEvent.AramPokeWarning(enemy.championName))
            }
    }

    private fun checkHealthPackReminder(gameTime: Double, events: MutableList<GameEvent>) {
        if (gameTime >= HEALTH_PACK_FIRST_REMINDER) {
            // Emit reminder at regular intervals (deduplication in EventProcessor avoids duplicates)
            val intervalIndex = ((gameTime - HEALTH_PACK_FIRST_REMINDER) / HEALTH_PACK_INTERVAL).toInt()
            val reminderTime = HEALTH_PACK_FIRST_REMINDER + (intervalIndex * HEALTH_PACK_INTERVAL)
            if (gameTime >= reminderTime && gameTime < reminderTime + 10) {
                events.add(GameEvent.AramHealthPackReminder(reminderTime))
            }
        }
    }

    private fun evaluateTeamfightAdvice(
        snapshot: GameSnapshot,
        gameTime: Double,
        events: MutableList<GameEvent>
    ) {
        val activePlayer = snapshot.activePlayer ?: return
        val ourPlayer = snapshot.allPlayers.find {
            it.summonerName == activePlayer.summonerName || it.riotId == activePlayer.riotId
        } ?: return
        val ourTeam = ourPlayer.team

        val enemies = snapshot.allPlayers.filter { it.team != ourTeam }
        val allies = snapshot.allPlayers.filter { it.team == ourTeam }

        val enemyEngagers = enemies.count { it.championName in ENGAGE_CHAMPIONS }
        val allyEngagers = allies.count { it.championName in ENGAGE_CHAMPIONS }

        // Early game advice
        if (gameTime < EARLY_GAME_END) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "Early game: farm safely, do not force fights before level 3"
                )
            )
        }

        // If the enemy has heavy engage, advise to stay back
        if (enemyEngagers >= 2) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "The enemy has $enemyEngagers engagers! Stay back and peel for the carries"
                )
            )
        }

        // If we have engage, advise to follow up
        if (allyEngagers >= 1 && gameTime > EARLY_GAME_END) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "Follow up on your tank's engage, prepare CC and shields"
                )
            )
        }

        // Mid-game: objective advice
        if (gameTime > MID_GAME_START) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "Mid-game: force fights after poking, do not all-in when the enemy is full HP"
                )
            )
        }
    }

    private fun evaluateSnowballUsage(
        snapshot: GameSnapshot,
        gameTime: Double,
        events: MutableList<GameEvent>
    ) {
        if (gameTime < EARLY_GAME_END) return

        val activePlayer = snapshot.activePlayer ?: return
        val ourPlayer = snapshot.allPlayers.find {
            it.summonerName == activePlayer.summonerName || it.riotId == activePlayer.riotId
        } ?: return

        // Check if player has Mark/Dash (snowball) - summoner spell
        val hasSnowball = ourPlayer.summonerSpells?.let { spells ->
            spells.summonerSpellOne?.displayName?.contains("Mark", ignoreCase = true) == true ||
                spells.summonerSpellTwo?.displayName?.contains("Mark", ignoreCase = true) == true
        } ?: false

        if (hasSnowball) {
            events.add(
                GameEvent.AramSnowballAdvice(
                    "Use snowball to engage or to reach allies in trouble"
                )
            )
        }
    }
}
