package com.lolcoach.bridge.model.liveclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivePlayer(
    @SerialName("abilities") val abilities: Abilities? = null,
    @SerialName("championStats") val championStats: ChampionStats? = null,
    @SerialName("currentGold") val currentGold: Double = 0.0,
    @SerialName("fullRunes") val fullRunes: FullRunes? = null,
    @SerialName("level") val level: Int = 1,
    @SerialName("summonerName") val summonerName: String = "",
    @SerialName("riotId") val riotId: String = "",
    @SerialName("teamRelativeColors") val teamRelativeColors: Boolean = false
)

@Serializable
data class Abilities(
    @SerialName("E") val e: Ability? = null,
    @SerialName("Passive") val passive: Ability? = null,
    @SerialName("Q") val q: Ability? = null,
    @SerialName("R") val r: Ability? = null,
    @SerialName("W") val w: Ability? = null
)

@Serializable
data class Ability(
    @SerialName("abilityLevel") val abilityLevel: Int = 0,
    @SerialName("displayName") val displayName: String = "",
    @SerialName("id") val id: String = "",
    @SerialName("rawDescription") val rawDescription: String = "",
    @SerialName("rawDisplayName") val rawDisplayName: String = ""
)

@Serializable
data class ChampionStats(
    @SerialName("abilityHaste") val abilityHaste: Double = 0.0,
    @SerialName("abilityPower") val abilityPower: Double = 0.0,
    @SerialName("armor") val armor: Double = 0.0,
    @SerialName("armorPenetrationFlat") val armorPenetrationFlat: Double = 0.0,
    @SerialName("armorPenetrationPercent") val armorPenetrationPercent: Double = 0.0,
    @SerialName("attackDamage") val attackDamage: Double = 0.0,
    @SerialName("attackRange") val attackRange: Double = 0.0,
    @SerialName("attackSpeed") val attackSpeed: Double = 0.0,
    @SerialName("bonusArmorPenetrationPercent") val bonusArmorPenetrationPercent: Double = 0.0,
    @SerialName("bonusMagicPenetrationPercent") val bonusMagicPenetrationPercent: Double = 0.0,
    @SerialName("critChance") val critChance: Double = 0.0,
    @SerialName("critDamage") val critDamage: Double = 0.0,
    @SerialName("currentHealth") val currentHealth: Double = 0.0,
    @SerialName("healShieldPower") val healShieldPower: Double = 0.0,
    @SerialName("healthRegenRate") val healthRegenRate: Double = 0.0,
    @SerialName("lifeSteal") val lifeSteal: Double = 0.0,
    @SerialName("magicLethality") val magicLethality: Double = 0.0,
    @SerialName("magicPenetrationFlat") val magicPenetrationFlat: Double = 0.0,
    @SerialName("magicPenetrationPercent") val magicPenetrationPercent: Double = 0.0,
    @SerialName("magicResist") val magicResist: Double = 0.0,
    @SerialName("maxHealth") val maxHealth: Double = 0.0,
    @SerialName("moveSpeed") val moveSpeed: Double = 0.0,
    @SerialName("omnivamp") val omnivamp: Double = 0.0,
    @SerialName("physicalLethality") val physicalLethality: Double = 0.0,
    @SerialName("physicalVamp") val physicalVamp: Double = 0.0,
    @SerialName("resourceMax") val resourceMax: Double = 0.0,
    @SerialName("resourceRegenRate") val resourceRegenRate: Double = 0.0,
    @SerialName("resourceType") val resourceType: String = "",
    @SerialName("resourceValue") val resourceValue: Double = 0.0,
    @SerialName("spellVamp") val spellVamp: Double = 0.0,
    @SerialName("tenacity") val tenacity: Double = 0.0
)

@Serializable
data class FullRunes(
    @SerialName("generalRunes") val generalRunes: List<Rune> = emptyList(),
    @SerialName("keystone") val keystone: Rune? = null,
    @SerialName("primaryRuneTree") val primaryRuneTree: Rune? = null,
    @SerialName("secondaryRuneTree") val secondaryRuneTree: Rune? = null,
    @SerialName("statRunes") val statRunes: List<StatRune> = emptyList()
)

@Serializable
data class Rune(
    @SerialName("displayName") val displayName: String = "",
    @SerialName("id") val id: Int = 0,
    @SerialName("rawDescription") val rawDescription: String = "",
    @SerialName("rawDisplayName") val rawDisplayName: String = ""
)

@Serializable
data class StatRune(
    @SerialName("id") val id: Int = 0,
    @SerialName("rawDescription") val rawDescription: String = ""
)
