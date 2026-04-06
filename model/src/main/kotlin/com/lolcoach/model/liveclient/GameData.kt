package com.lolcoach.model.liveclient

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameData(
    @SerialName("gameMode") val gameMode: String = "",
    @SerialName("gameTime") val gameTime: Double = 0.0,
    @SerialName("mapName") val mapName: String = "",
    @SerialName("mapNumber") val mapNumber: Int = 0,
    @SerialName("mapTerrain") val mapTerrain: String = ""
)
