package com.lolcoach.bridge.model.liveclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    @SerialName("championName") val championName: String = "",
    @SerialName("isBot") val isBot: Boolean = false,
    @SerialName("isDead") val isDead: Boolean = false,
    @SerialName("items") val items: List<PlayerItem> = emptyList(),
    @SerialName("level") val level: Int = 1,
    @SerialName("position") val position: String = "",
    @SerialName("rawChampionName") val rawChampionName: String = "",
    @SerialName("rawSkinName") val rawSkinName: String = "",
    @SerialName("respawnTimer") val respawnTimer: Double = 0.0,
    @SerialName("riotId") val riotId: String = "",
    @SerialName("riotIdGameName") val riotIdGameName: String = "",
    @SerialName("riotIdTagLine") val riotIdTagLine: String = "",
    @SerialName("runes") val runes: PlayerRunes? = null,
    @SerialName("scores") val scores: PlayerScores? = null,
    @SerialName("skinID") val skinID: Int = 0,
    @SerialName("skinName") val skinName: String = "",
    @SerialName("summonerName") val summonerName: String = "",
    @SerialName("summonerSpells") val summonerSpells: SummonerSpells? = null,
    @SerialName("team") val team: String = ""
)

@Serializable
data class PlayerItem(
    @SerialName("canUse") val canUse: Boolean = false,
    @SerialName("consumable") val consumable: Boolean = false,
    @SerialName("count") val count: Int = 0,
    @SerialName("displayName") val displayName: String = "",
    @SerialName("itemID") val itemID: Int = 0,
    @SerialName("price") val price: Int = 0,
    @SerialName("rawDescription") val rawDescription: String = "",
    @SerialName("rawDisplayName") val rawDisplayName: String = "",
    @SerialName("slot") val slot: Int = 0
)

@Serializable
data class PlayerScores(
    @SerialName("assists") val assists: Int = 0,
    @SerialName("creepScore") val creepScore: Int = 0,
    @SerialName("deaths") val deaths: Int = 0,
    @SerialName("kills") val kills: Int = 0,
    @SerialName("wardScore") val wardScore: Double = 0.0
)

@Serializable
data class PlayerRunes(
    @SerialName("keystone") val keystone: Rune? = null,
    @SerialName("primaryRuneTree") val primaryRuneTree: Rune? = null,
    @SerialName("secondaryRuneTree") val secondaryRuneTree: Rune? = null
)

@Serializable
data class SummonerSpells(
    @SerialName("summonerSpellOne") val summonerSpellOne: SummonerSpell? = null,
    @SerialName("summonerSpellTwo") val summonerSpellTwo: SummonerSpell? = null
)

@Serializable
data class SummonerSpell(
    @SerialName("displayName") val displayName: String = "",
    @SerialName("rawDescription") val rawDescription: String = "",
    @SerialName("rawDisplayName") val rawDisplayName: String = ""
)
