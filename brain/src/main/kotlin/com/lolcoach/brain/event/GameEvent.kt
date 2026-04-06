package com.lolcoach.brain.event

sealed class GameEvent {
    abstract val message: String

    data class EnemySupportSelected(val championName: String) : GameEvent() {
        override val message = "Il support nemico è $championName"
    }

    data class Level2Approaching(val minionsNeeded: Int) : GameEvent() {
        override val message = "Mancano $minionsNeeded minion al livello 2! Preparati al trade"
    }

    data object Level2Reached : GameEvent() {
        override val message = "Livello 2 raggiunto! Vai all'attacco!"
    }

    data class VisionNeeded(val reason: String) : GameEvent() {
        override val message = "Piazza le ward: $reason"
    }

    data class DragonTimerWarning(val seconds: Int) : GameEvent() {
        override val message = "Dragon tra $seconds secondi, prepara la visione"
    }

    data class ItemSuggestion(val item: String, val reason: String) : GameEvent() {
        override val message = "Suggerimento item: $item - $reason"
    }

    data class SynergyAdvice(val advice: String) : GameEvent() {
        override val message = advice
    }

    data class GenericTip(override val message: String) : GameEvent()

    // ARAM-specific events
    data class AramHealthPackReminder(val gameTime: Double) : GameEvent() {
        override val message = "Ricorda i pack salute! Controlla le zone laterali"
    }

    data class AramTeamfightTip(val tip: String) : GameEvent() {
        override val message = "Teamfight ARAM: $tip"
    }

    data class AramPokeWarning(val enemyChampion: String) : GameEvent() {
        override val message = "Attenzione al poke di $enemyChampion! Stai dietro i minion"
    }

    data class AramSnowballAdvice(val advice: String) : GameEvent() {
        override val message = "Snowball: $advice"
    }

    data class LlmAnalysis(
        val section: String,
        val content: String
    ) : GameEvent() {
        override val message = "[$section] $content"
    }
}
