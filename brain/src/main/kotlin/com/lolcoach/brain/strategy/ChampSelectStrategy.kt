package com.lolcoach.brain.strategy

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.event.Strategy
import com.lolcoach.brain.state.GameState
import com.lolcoach.model.lcu.ChampSelectSession
import com.lolcoach.model.liveclient.GameSnapshot

class ChampSelectStrategy : Strategy {

    companion object {
        // Champion ID -> Name mapping (subset for common supports/ADCs)
        val CHAMPION_NAMES = mapOf(
            222 to "Jinx", 51 to "Caitlyn", 81 to "Ezreal", 145 to "Kai'Sa",
            236 to "Lucian", 21 to "Miss Fortune", 29 to "Twitch", 18 to "Tristana",
            67 to "Vayne", 119 to "Draven", 498 to "Xayah", 235 to "Senna",
            110 to "Varus", 15 to "Sivir", 202 to "Jhin", 96 to "Kog'Maw",
            42 to "Corki", 22 to "Ashe",
            // Supports
            89 to "Leona", 412 to "Thresh", 111 to "Nautilus", 53 to "Blitzcrank",
            12 to "Alistar", 201 to "Braum", 497 to "Rakan", 44 to "Taric",
            37 to "Sona", 16 to "Soraka", 117 to "Lulu", 40 to "Janna",
            267 to "Nami", 43 to "Karma", 25 to "Morgana", 143 to "Zyra",
            63 to "Brand", 161 to "Vel'Koz", 101 to "Xerath", 350 to "Yuumi",
            526 to "Rell", 887 to "Renata Glasc", 902 to "Milio"
        )

        // Synergy table: ADC champion ID -> list of good Support champion IDs
        val SYNERGIES = mapOf(
            222 to listOf(89, 111, 412, 267), // Jinx + engage/Nami
            51 to listOf(412, 40, 117, 267),   // Caitlyn + peel/poke
            81 to listOf(43, 37, 117, 350),    // Ezreal + enchanter
            145 to listOf(111, 89, 412, 267),  // Kai'Sa + engage
            236 to listOf(267, 12, 53, 201),   // Lucian + engage/Nami
            21 to listOf(89, 111, 12, 267),    // MF + engage
            67 to listOf(117, 267, 40, 37),    // Vayne + enchanter
            119 to listOf(412, 89, 111, 267),  // Draven + engage
            498 to listOf(497, 89, 412, 267),  // Xayah + Rakan/engage
            202 to listOf(412, 143, 25, 161),  // Jhin + CC/poke
            22 to listOf(143, 161, 101, 25)    // Ashe + poke mage
        )
    }

    override fun evaluate(snapshot: GameSnapshot, state: GameState): List<GameEvent> = emptyList()

    override fun evaluateChampSelect(session: ChampSelectSession, state: GameState): List<GameEvent> {
        val events = mutableListOf<GameEvent>()

        // Find the ADC in our team
        val adc = session.myTeam.find { it.assignedPosition == "bottom" }
        val support = session.myTeam.find { it.assignedPosition == "utility" }

        // Enemy support detection
        val enemySupport = session.theirTeam.find { it.assignedPosition == "utility" }
        if (enemySupport != null && enemySupport.championId > 0) {
            val name = CHAMPION_NAMES[enemySupport.championId] ?: "Champion #${enemySupport.championId}"
            events.add(GameEvent.EnemySupportSelected(name))
        }

        // Synergy advice
        if (adc != null && adc.championId > 0 && support != null && support.championId > 0) {
            val adcName = CHAMPION_NAMES[adc.championId] ?: "ADC"
            val supportName = CHAMPION_NAMES[support.championId] ?: "Support"
            val goodSupports = SYNERGIES[adc.championId]

            if (goodSupports != null) {
                if (support.championId in goodSupports) {
                    events.add(
                        GameEvent.SynergyAdvice(
                            "Great synergy! $supportName is perfect with $adcName"
                        )
                    )
                } else {
                    val suggestions = goodSupports
                        .mapNotNull { CHAMPION_NAMES[it] }
                        .take(3)
                        .joinToString(", ")
                    events.add(
                        GameEvent.SynergyAdvice(
                            "With $adcName, the best synergies are: $suggestions"
                        )
                    )
                }
            }
        }

        return events
    }
}
