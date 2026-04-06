package com.lolcoach.model.lcu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChampSelectSession(
    @SerialName("actions") val actions: List<List<ChampSelectAction>> = emptyList(),
    @SerialName("benchChampions") val benchChampions: List<BenchChampion> = emptyList(),
    @SerialName("benchEnabled") val benchEnabled: Boolean = false,
    @SerialName("boostableSkinCount") val boostableSkinCount: Int = 0,
    @SerialName("chatDetails") val chatDetails: ChatDetails? = null,
    @SerialName("counter") val counter: Int = 0,
    @SerialName("hasSimultaneousBans") val hasSimultaneousBans: Boolean = false,
    @SerialName("hasSimultaneousPicks") val hasSimultaneousPicks: Boolean = false,
    @SerialName("isCustomGame") val isCustomGame: Boolean = false,
    @SerialName("isSpectating") val isSpectating: Boolean = false,
    @SerialName("localPlayerCellId") val localPlayerCellId: Int = -1,
    @SerialName("lockedEventIndex") val lockedEventIndex: Int = -1,
    @SerialName("myTeam") val myTeam: List<ChampSelectPlayerSelection> = emptyList(),
    @SerialName("theirTeam") val theirTeam: List<ChampSelectPlayerSelection> = emptyList(),
    @SerialName("timer") val timer: ChampSelectTimer? = null,
    @SerialName("trades") val trades: List<ChampSelectTrade> = emptyList()
)

@Serializable
data class ChampSelectAction(
    @SerialName("actorCellId") val actorCellId: Int = 0,
    @SerialName("championId") val championId: Int = 0,
    @SerialName("completed") val completed: Boolean = false,
    @SerialName("id") val id: Int = 0,
    @SerialName("isAllyAction") val isAllyAction: Boolean = false,
    @SerialName("isInProgress") val isInProgress: Boolean = false,
    @SerialName("type") val type: String = ""
)

@Serializable
data class ChampSelectPlayerSelection(
    @SerialName("assignedPosition") val assignedPosition: String = "",
    @SerialName("cellId") val cellId: Int = 0,
    @SerialName("championId") val championId: Int = 0,
    @SerialName("championPickIntent") val championPickIntent: Int = 0,
    @SerialName("spell1Id") val spell1Id: Int = 0,
    @SerialName("spell2Id") val spell2Id: Int = 0,
    @SerialName("summonerId") val summonerId: Long = 0,
    @SerialName("team") val team: Int = 0,
    @SerialName("wardSkinId") val wardSkinId: Int = 0
)

@Serializable
data class ChampSelectTimer(
    @SerialName("adjustedTimeLeftInPhase") val adjustedTimeLeftInPhase: Long = 0,
    @SerialName("internalNowInEpochMs") val internalNowInEpochMs: Long = 0,
    @SerialName("isInfinite") val isInfinite: Boolean = false,
    @SerialName("phase") val phase: String = "",
    @SerialName("totalTimeInPhase") val totalTimeInPhase: Long = 0
)

@Serializable
data class ChampSelectTrade(
    @SerialName("cellId") val cellId: Int = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("state") val state: String = ""
)

@Serializable
data class BenchChampion(
    @SerialName("championId") val championId: Int = 0,
    @SerialName("isPriority") val isPriority: Boolean = false
)

@Serializable
data class ChatDetails(
    @SerialName("mucJwtDto") val mucJwtDto: MucJwtDto? = null,
    @SerialName("multiUserChatId") val multiUserChatId: String = "",
    @SerialName("multiUserChatPassword") val multiUserChatPassword: String = ""
)

@Serializable
data class MucJwtDto(
    @SerialName("channelClaim") val channelClaim: String = "",
    @SerialName("domain") val domain: String = "",
    @SerialName("jwt") val jwt: String = "",
    @SerialName("targetRegion") val targetRegion: String = ""
)
