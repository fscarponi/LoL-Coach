package com.lolcoach.brain.state

/**
 * Modalità di gioco supportate.
 */
enum class GameMode(val displayName: String) {
    SUMMONERS_RIFT("Summoner's Rift"),
    ARAM("ARAM"),
    ARAM_MAYHEM("ARAM Mayhem"),
    UNKNOWN("Sconosciuta");

    companion object {
        /**
         * Determina la modalità dal campo gameMode e mapName della Live Client Data API.
         * gameMode: "CLASSIC", "ARAM", "CHERRY" (Arena), etc.
         * mapName: "Map11" (SR), "Map12" (ARAM/Howling Abyss), etc.
         * gameModeName dal LCU può contenere "GAMEMODEX" per varianti come Mayhem.
         */
        fun fromApiData(gameMode: String, mapName: String = "", mapNumber: Int = 0): GameMode {
            val gm = gameMode.uppercase()
            return when {
                gm == "ARAM" && mapName.uppercase().contains("MAYHEM") -> ARAM_MAYHEM
                gm == "ARAM" -> ARAM
                // Alcune varianti ARAM usano nomi diversi
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
