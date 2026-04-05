package com.lolcoach.bridge.model.liveclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameSnapshot(
    @SerialName("activePlayer") val activePlayer: ActivePlayer? = null,
    @SerialName("allPlayers") val allPlayers: List<Player> = emptyList(),
    @SerialName("events") val events: GameEvents? = null,
    @SerialName("gameData") val gameData: GameData? = null
)

@Serializable
data class GameEvents(
    @SerialName("Events") val events: List<GameEvent> = emptyList()
)

@Serializable
data class GameEvent(
    @SerialName("EventID") val eventID: Int = 0,
    @SerialName("EventName") val eventName: String = "",
    @SerialName("EventTime") val eventTime: Double = 0.0,
    @SerialName("KillerName") val killerName: String = "",
    @SerialName("VictimName") val victimName: String = "",
    @SerialName("Assisters") val assisters: List<String> = emptyList()
)
