package com.lolcoach.brain.event

sealed class GameEvent {
    abstract val message: String

    data class EnemySupportSelected(val championName: String) : GameEvent() {
        override val message = "Enemy support is $championName"
    }

    data class Level2Approaching(val minionsNeeded: Int) : GameEvent() {
        override val message = "$minionsNeeded minions left for level 2! Prepare for trade"
    }

    data object Level2Reached : GameEvent() {
        override val message = "Level 2 reached! Go for the attack!"
    }

    data class VisionNeeded(val reason: String) : GameEvent() {
        override val message = "Place wards: $reason"
    }

    data class DragonTimerWarning(val seconds: Int) : GameEvent() {
        override val message = "Dragon in $seconds seconds, prepare vision"
    }

    data class ItemSuggestion(val item: String, val reason: String) : GameEvent() {
        override val message = "Item suggestion: $item - $reason"
    }

    data class SynergyAdvice(val advice: String) : GameEvent() {
        override val message = advice
    }

    data class GenericTip(override val message: String) : GameEvent()

    // ARAM-specific events
    data class AramHealthPackReminder(val gameTime: Double) : GameEvent() {
        override val message = "Remember health packs! Check the side areas"
    }

    data class AramTeamfightTip(val tip: String) : GameEvent() {
        override val message = "ARAM Teamfight: $tip"
    }

    data class AramPokeWarning(val enemyChampion: String) : GameEvent() {
        override val message = "Beware of $enemyChampion's poke! Stay behind minions"
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

    data class UserVoiceQuery(val text: String) : GameEvent() {
        override val message = "You asked: \"$text\""
    }
}
