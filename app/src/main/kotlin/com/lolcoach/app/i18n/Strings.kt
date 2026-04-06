package com.lolcoach.app.i18n

object Strings {
    const val Connection = "Connection"
    const val Lockfile = "Lockfile"
    const val LiveClient = "Live Client"
    const val NotFound = "Not found"
    const val NotActive = "Not active"
    const val Connected = "Connected"
    
    fun lastSnapshot(seconds: Long) = "Last snapshot ${seconds}s ago"
    
    const val Idle = "IDLE — Disconnected"
    const val ChampSelect = "CHAMPION SELECT"
    const val Loading = "LOADING"
    const val InGame = "IN GAME"
    const val PostGame = "POST GAME"
    
    const val DisconnectedStatus = "Disconnected"
    const val LoadingStatus = "Loading..."
    const val InGameStatus = "In Game"
    const val PostGameStatus = "Post Game"
}
