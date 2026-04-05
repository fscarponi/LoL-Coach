package com.lolcoach.brain.state

import com.lolcoach.bridge.model.LockfileData
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameStateMachine {

    private val _state = MutableStateFlow<GameState>(GameState.Idle)
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun onLockfileChanged(lockfileData: LockfileData?) {
        if (lockfileData == null) {
            _state.value = GameState.Idle
        }
    }

    fun onChampSelectUpdate(session: ChampSelectSession?) {
        if (session != null && _state.value !is GameState.InGame) {
            _state.value = GameState.ChampSelect
        }
    }

    fun onChampSelectEnded() {
        if (_state.value is GameState.ChampSelect) {
            _state.value = GameState.Loading
        }
    }

    fun onGameSnapshotReceived(snapshot: GameSnapshot?) {
        if (snapshot != null && snapshot.gameData != null) {
            _state.value = GameState.InGame
        }
    }

    fun onGameEnded() {
        _state.value = GameState.PostGame
    }

    fun reset() {
        _state.value = GameState.Idle
    }
}
