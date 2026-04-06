package com.lolcoach.brain.state

/**
 * Supported game modes.
 */
enum class GameMode(val displayName: String) {
    SUMMONERS_RIFT("Summoner's Rift"),
    ARAM("ARAM"),
    ARAM_MAYHEM("ARAM Mayhem"),
    UNKNOWN("Unknown");

    companion object {
        /**
         * Determines the mode from the gameMode and mapName fields of the Live Client Data API.
         * gameMode: "CLASSIC", "ARAM", "CHERRY" (Arena), etc.
         * mapName: "Map11" (SR), "Map12" (ARAM/Howling Abyss), etc.
         * gameModeName from LCU may contain "GAMEMODEX" for variants like Mayhem.
         */
        fun fromApiData(gameMode: String, mapName: String = "", mapNumber: Int = 0): GameMode {
            val gm = gameMode.uppercase()
            return when {
                gm == "ARAM" && mapName.uppercase().contains("MAYHEM") -> ARAM_MAYHEM
                gm == "ARAM" -> ARAM
                // Some ARAM variants use different names
                gm.contains("ARAM") -> ARAM
                gm == "CLASSIC" && (mapNumber == 11 || mapName.contains("11")) -> SUMMONERS_RIFT
                gm == "CLASSIC" -> SUMMONERS_RIFT
                // Howling Abyss map
                mapNumber == 12 -> ARAM
                else -> UNKNOWN
            }
        }
    }
}
