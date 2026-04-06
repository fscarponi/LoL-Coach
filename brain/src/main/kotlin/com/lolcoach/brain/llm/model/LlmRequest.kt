package com.lolcoach.brain.llm.model

import com.lolcoach.brain.state.GameMode
import kotlinx.serialization.Serializable

/**
 * Abstract model of a request to the LLM.
 * Contains all structured information that the coach can send to the model.
 */
@Serializable
data class LlmRequest(
    val gameMode: GameMode = GameMode.UNKNOWN,
    val phase: GamePhase = GamePhase.CHAMP_SELECT,
    val champSelect: ChampSelectInfo? = null,
    val inGame: InGameInfo? = null
)

/**
 * Fase di gioco in cui viene fatta la richiesta.
 */
@Serializable
enum class GamePhase {
    CHAMP_SELECT,
    LOADING,
    IN_GAME
}

/**
 * Informazioni disponibili durante la Champion Select.
 */
@Serializable
data class ChampSelectInfo(
    val myTeam: List<ChampSelectPlayer> = emptyList(),
    val enemyTeam: List<ChampSelectPlayer> = emptyList(),
    val bans: List<String> = emptyList(),
    val currentPhase: String = "",
    val benchChampions: List<String> = emptyList()
)

@Serializable
data class ChampSelectPlayer(
    val championName: String,
    val assignedRole: String = "",
    val summonerSpell1: String = "",
    val summonerSpell2: String = ""
)

/**
 * Informazioni disponibili durante la partita (da Live Client Data API).
 */
@Serializable
data class InGameInfo(
    val gameTime: Double = 0.0,
    val mapTerrain: String = "",
    val activePlayer: ActivePlayerInfo? = null,
    val allies: List<PlayerInfo> = emptyList(),
    val enemies: List<PlayerInfo> = emptyList(),
    val recentEvents: List<GameEventInfo> = emptyList()
)

@Serializable
data class ActivePlayerInfo(
    val championName: String = "",
    val level: Int = 1,
    val currentGold: Double = 0.0,
    val keystoneRune: String = "",
    val primaryTree: String = "",
    val secondaryTree: String = "",
    val stats: PlayerStatsInfo? = null,
    val abilities: AbilitiesInfo? = null
)

@Serializable
data class PlayerStatsInfo(
    val currentHealth: Double = 0.0,
    val maxHealth: Double = 0.0,
    val armor: Double = 0.0,
    val magicResist: Double = 0.0,
    val attackDamage: Double = 0.0,
    val abilityPower: Double = 0.0,
    val abilityHaste: Double = 0.0,
    val moveSpeed: Double = 0.0
)

@Serializable
data class AbilitiesInfo(
    val qLevel: Int = 0,
    val wLevel: Int = 0,
    val eLevel: Int = 0,
    val rLevel: Int = 0
)

@Serializable
data class PlayerInfo(
    val championName: String = "",
    val level: Int = 1,
    val kills: Int = 0,
    val deaths: Int = 0,
    val assists: Int = 0,
    val creepScore: Int = 0,
    val wardScore: Double = 0.0,
    val items: List<String> = emptyList(),
    val keystoneRune: String = "",
    val summonerSpell1: String = "",
    val summonerSpell2: String = "",
    val isDead: Boolean = false,
    val respawnTimer: Double = 0.0,
    val position: String = ""
)

@Serializable
data class GameEventInfo(
    val eventName: String = "",
    val eventTime: Double = 0.0,
    val killerName: String = "",
    val victimName: String = ""
)
