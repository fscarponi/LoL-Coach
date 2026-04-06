package com.lolcoach.brain.llm.model

import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.strategy.ChampSelectStrategy.Companion.CHAMPION_NAMES
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import com.lolcoach.bridge.model.liveclient.GameSnapshot

/**
 * Costruisce LlmRequest a partire dai dati grezzi del bridge.
 * Centralizza la conversione dati bridge → modello LLM.
 */
object RequestBuilder {

    /**
     * Costruisce una richiesta LLM dalla champion select.
     */
    fun fromChampSelect(session: ChampSelectSession, gameMode: GameMode): LlmRequest {
        val myTeam = session.myTeam.mapNotNull { player ->
            if (player.championId > 0) {
                ChampSelectPlayer(
                    championName = CHAMPION_NAMES[player.championId] ?: "Champion#${player.championId}",
                    assignedRole = player.assignedPosition.ifBlank { "?" }
                )
            } else null
        }

        val enemyTeam = session.theirTeam.mapNotNull { player ->
            if (player.championId > 0) {
                ChampSelectPlayer(
                    championName = CHAMPION_NAMES[player.championId] ?: "Champion#${player.championId}",
                    assignedRole = player.assignedPosition.ifBlank { "?" }
                )
            } else null
        }

        val bans = session.actions.flatten()
            .filter { it.type == "ban" && it.completed && it.championId > 0 }
            .mapNotNull { CHAMPION_NAMES[it.championId] }

        val bench = session.benchChampions
            .filter { it.championId > 0 }
            .mapNotNull { CHAMPION_NAMES[it.championId] }

        val phase = session.timer?.phase ?: ""

        return LlmRequest(
            gameMode = gameMode,
            phase = GamePhase.CHAMP_SELECT,
            champSelect = ChampSelectInfo(
                myTeam = myTeam,
                enemyTeam = enemyTeam,
                bans = bans,
                currentPhase = phase,
                benchChampions = bench
            )
        )
    }

    /**
     * Costruisce una richiesta LLM dallo snapshot in-game.
     */
    fun fromGameSnapshot(snapshot: GameSnapshot, gameMode: GameMode): LlmRequest {
        val activePlayer = snapshot.activePlayer?.let { ap ->
            ActivePlayerInfo(
                championName = ap.summonerName,
                level = ap.level,
                currentGold = ap.currentGold,
                keystoneRune = ap.fullRunes?.keystone?.displayName ?: "",
                primaryTree = ap.fullRunes?.primaryRuneTree?.displayName ?: "",
                secondaryTree = ap.fullRunes?.secondaryRuneTree?.displayName ?: "",
                stats = ap.championStats?.let { s ->
                    PlayerStatsInfo(
                        currentHealth = s.currentHealth,
                        maxHealth = s.maxHealth,
                        armor = s.armor,
                        magicResist = s.magicResist,
                        attackDamage = s.attackDamage,
                        abilityPower = s.abilityPower,
                        abilityHaste = s.abilityHaste,
                        moveSpeed = s.moveSpeed
                    )
                },
                abilities = ap.abilities?.let { a ->
                    AbilitiesInfo(
                        qLevel = a.q?.abilityLevel ?: 0,
                        wLevel = a.w?.abilityLevel ?: 0,
                        eLevel = a.e?.abilityLevel ?: 0,
                        rLevel = a.r?.abilityLevel ?: 0
                    )
                }
            )
        }

        val myTeamName = snapshot.allPlayers
            .firstOrNull { it.summonerName == snapshot.activePlayer?.summonerName }
            ?.team ?: "ORDER"

        val allies = snapshot.allPlayers
            .filter { it.team == myTeamName }
            .map { it.toPlayerInfo() }

        val enemies = snapshot.allPlayers
            .filter { it.team != myTeamName }
            .map { it.toPlayerInfo() }

        val recentEvents = snapshot.events?.events
            ?.takeLast(10)
            ?.map { GameEventInfo(it.eventName, it.eventTime, it.killerName, it.victimName) }
            ?: emptyList()

        return LlmRequest(
            gameMode = gameMode,
            phase = GamePhase.IN_GAME,
            inGame = InGameInfo(
                gameTime = snapshot.gameData?.gameTime ?: 0.0,
                mapTerrain = snapshot.gameData?.mapTerrain ?: "",
                activePlayer = activePlayer,
                allies = allies,
                enemies = enemies,
                recentEvents = recentEvents
            )
        )
    }

    /**
     * Converte un LlmRequest in un prompt testuale leggibile dall'LLM.
     */
    fun toUserPrompt(request: LlmRequest): String = buildString {
        when (request.phase) {
            GamePhase.CHAMP_SELECT -> {
                val cs = request.champSelect ?: return@buildString
                appendLine("=== CHAMPION SELECT ===")
                appendLine("Modalità: ${request.gameMode.displayName}")
                if (cs.currentPhase.isNotBlank()) appendLine("Fase: ${cs.currentPhase}")

                val myTeamStr = cs.myTeam.joinToString(", ") { "${it.championName} (${it.assignedRole})" }
                appendLine("Il mio team: ${myTeamStr.ifEmpty { "non ancora selezionato" }}")

                val enemyStr = cs.enemyTeam.joinToString(", ") { "${it.championName} (${it.assignedRole})" }
                appendLine("Team nemico: ${enemyStr.ifEmpty { "non ancora visibile" }}")

                if (cs.bans.isNotEmpty()) appendLine("Ban: ${cs.bans.joinToString(", ")}")
                if (cs.benchChampions.isNotEmpty()) appendLine("Bench disponibili: ${cs.benchChampions.joinToString(", ")}")

                appendLine("Io gioco Support.")
                appendLine("Analizza la composizione e dammi i consigli strategici.")
            }

            GamePhase.LOADING, GamePhase.IN_GAME -> {
                val ig = request.inGame ?: return@buildString
                appendLine("=== IN GAME (${formatTime(ig.gameTime)}) ===")
                appendLine("Modalità: ${request.gameMode.displayName}")
                if (ig.mapTerrain.isNotBlank()) appendLine("Terreno mappa: ${ig.mapTerrain}")

                ig.activePlayer?.let { ap ->
                    appendLine("--- Il mio champion ---")
                    appendLine("${ap.championName} Lv.${ap.level} | Gold: ${ap.currentGold.toInt()}")
                    if (ap.keystoneRune.isNotBlank()) appendLine("Rune: ${ap.keystoneRune} (${ap.primaryTree}/${ap.secondaryTree})")
                    ap.abilities?.let { appendLine("Abilità: Q${it.qLevel} W${it.wLevel} E${it.eLevel} R${it.rLevel}") }
                    ap.stats?.let { appendLine("HP: ${it.currentHealth.toInt()}/${it.maxHealth.toInt()} | AR:${it.armor.toInt()} MR:${it.magicResist.toInt()} | AD:${it.attackDamage.toInt()} AP:${it.abilityPower.toInt()} AH:${it.abilityHaste.toInt()}") }
                }

                if (ig.allies.isNotEmpty()) {
                    appendLine("--- Alleati ---")
                    ig.allies.forEach { p ->
                        appendLine("${p.championName} Lv.${p.level} ${p.kills}/${p.deaths}/${p.assists} CS:${p.creepScore} WS:${"%.1f".format(p.wardScore)} | ${p.items.joinToString(", ")}${if (p.isDead) " [MORTO ${p.respawnTimer.toInt()}s]" else ""}")
                    }
                }

                if (ig.enemies.isNotEmpty()) {
                    appendLine("--- Nemici ---")
                    ig.enemies.forEach { p ->
                        appendLine("${p.championName} Lv.${p.level} ${p.kills}/${p.deaths}/${p.assists} CS:${p.creepScore} | ${p.items.joinToString(", ")}${if (p.isDead) " [MORTO ${p.respawnTimer.toInt()}s]" else ""}")
                    }
                }

                if (ig.recentEvents.isNotEmpty()) {
                    appendLine("--- Eventi recenti ---")
                    ig.recentEvents.forEach { e ->
                        appendLine("[${formatTime(e.eventTime)}] ${e.eventName}: ${e.killerName} → ${e.victimName}")
                    }
                }

                appendLine("Io gioco Support. Dammi consigli strategici per la situazione attuale.")
            }
        }
    }

    private fun formatTime(seconds: Double): String {
        val min = (seconds / 60).toInt()
        val sec = (seconds % 60).toInt()
        return "%d:%02d".format(min, sec)
    }

    private fun com.lolcoach.bridge.model.liveclient.Player.toPlayerInfo() = PlayerInfo(
        championName = championName,
        level = level,
        kills = scores?.kills ?: 0,
        deaths = scores?.deaths ?: 0,
        assists = scores?.assists ?: 0,
        creepScore = scores?.creepScore ?: 0,
        wardScore = scores?.wardScore ?: 0.0,
        items = items.map { it.displayName },
        keystoneRune = runes?.keystone?.displayName ?: "",
        summonerSpell1 = summonerSpells?.summonerSpellOne?.displayName ?: "",
        summonerSpell2 = summonerSpells?.summonerSpellTwo?.displayName ?: "",
        isDead = isDead,
        respawnTimer = respawnTimer,
        position = position
    )
}
