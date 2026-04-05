package com.lolcoach.brain.state

sealed class GameState {
    data object Idle : GameState()
    data object ChampSelect : GameState()
    data object Loading : GameState()
    data object InGame : GameState()
    data object PostGame : GameState()
}
