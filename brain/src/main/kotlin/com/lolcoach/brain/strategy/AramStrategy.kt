package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.event.Strategy
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot

/**
 * Strategia specifica per ARAM e ARAM Mayhem.
 * Consigli su teamfight, poke, health pack e snowball.
 */
class AramStrategy : Strategy {

    override val applicableGameModes = setOf(GameMode.ARAM, GameMode.ARAM_MAYHEM)

    companion object {
        // Champion noti per poke pesante in ARAM
        val POKE_CHAMPIONS = setOf(
            "Lux", "Xerath", "Ziggs", "Vel'Koz", "Jayce", "Nidalee",
            "Ezreal", "Varus", "Kog'Maw", "Zoe", "Seraphine", "Senna",
            "Caitlyn", "Jhin", "Ashe", "Morgana", "Brand", "Zyra"
        )

        // Champion con engage forte in ARAM
        val ENGAGE_CHAMPIONS = setOf(
            "Malphite", "Amumu", "Leona", "Nautilus", "Alistar", "Rakan",
            "Ornn", "Sejuani", "Zac", "Rell", "Thresh", "Blitzcrank"
        )

        // Intervalli per i reminder (in secondi di game time)
        const val HEALTH_PACK_FIRST_REMINDER = 120.0  // 2 min
        const val HEALTH_PACK_INTERVAL = 300.0         // ogni 5 min
        const val EARLY_GAME_END = 180.0               // 3 min
        const val MID_GAME_START = 480.0               // 8 min
    }

    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        val activePlayer = snapshot.activePlayer ?: return events
        val gameTime = snapshot.gameData?.gameTime ?: return events

        // Poke warning: segnala champion nemici con poke pesante
        detectPokeThreats(snapshot, events)

        // Health pack reminder periodico
        checkHealthPackReminder(gameTime, events)

        // Consigli teamfight basati sulla composizione
        evaluateTeamfightAdvice(snapshot, gameTime, events)

        // Snowball advice per support
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
            // Emetti reminder a intervalli regolari (la dedup nel EventProcessor evita duplicati)
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

        // Consiglio early game
        if (gameTime < EARLY_GAME_END) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "Fase iniziale: farma in sicurezza, non forzare fight prima del livello 3"
                )
            )
        }

        // Se il nemico ha molto engage, consiglia di stare indietro
        if (enemyEngagers >= 2) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "Il nemico ha $enemyEngagers engager! Stai dietro e peela per i carry"
                )
            )
        }

        // Se noi abbiamo engage, consiglia di seguire
        if (allyEngagers >= 1 && gameTime > EARLY_GAME_END) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "Segui l'engage dei tuoi tank, prepara CC e shield"
                )
            )
        }

        // Mid-game: consiglio su obiettivi
        if (gameTime > MID_GAME_START) {
            events.add(
                GameEvent.AramTeamfightTip(
                    "Mid-game: forza fight dopo aver pokato, non andare all-in a vita piena del nemico"
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
                    "Usa snowball per engage o per raggiungere alleati in difficoltà"
                )
            )
        }
    }
}
