package com.lolcoach.brain.state

import com.lolcoach.model.LockfileData
import com.lolcoach.model.lcu.ChampSelectSession
import com.lolcoach.model.liveclient.GameData
import com.lolcoach.model.liveclient.GameSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GameStateMachineTest {

    private val machine = GameStateMachine()

    @Test
    fun `initial state is Idle`() {
        assertIs<GameState.Idle>(machine.state.value)
    }

    @Test
    fun `champ select update transitions to ChampSelect`() {
        machine.onChampSelectUpdate(ChampSelectSession())
        assertIs<GameState.ChampSelect>(machine.state.value)
    }

    @Test
    fun `champ select ended transitions to Loading`() {
        machine.onChampSelectUpdate(ChampSelectSession())
        machine.onChampSelectEnded()
        assertIs<GameState.Loading>(machine.state.value)
    }

    @Test
    fun `game snapshot transitions to InGame`() {
        val snapshot = GameSnapshot(gameData = GameData(gameMode = "CLASSIC", gameTime = 100.0))
        machine.onGameSnapshotReceived(snapshot)
        assertIs<GameState.InGame>(machine.state.value)
    }

    @Test
    fun `null snapshot does not change state`() {
        machine.onGameSnapshotReceived(null)
        assertIs<GameState.Idle>(machine.state.value)
    }

    @Test
    fun `lockfile null resets to Idle`() {
        machine.onChampSelectUpdate(ChampSelectSession())
        assertIs<GameState.ChampSelect>(machine.state.value)

        machine.onLockfileChanged(null)
        assertIs<GameState.Idle>(machine.state.value)
    }

    @Test
    fun `lockfile present does not change state by itself`() {
        val lockfile = LockfileData("LeagueClient", 1234, 8394, "token", "https")
        machine.onLockfileChanged(lockfile)
        assertIs<GameState.Idle>(machine.state.value)
    }

    @Test
    fun `game ended transitions to PostGame`() {
        val snapshot = GameSnapshot(gameData = GameData(gameMode = "CLASSIC", gameTime = 100.0))
        machine.onGameSnapshotReceived(snapshot)
        machine.onGameEnded()
        assertIs<GameState.PostGame>(machine.state.value)
    }

    @Test
    fun `InGame state ignores champ select updates`() {
        val snapshot = GameSnapshot(gameData = GameData(gameMode = "CLASSIC", gameTime = 100.0))
        machine.onGameSnapshotReceived(snapshot)
        assertIs<GameState.InGame>(machine.state.value)

        machine.onChampSelectUpdate(ChampSelectSession())
        assertIs<GameState.InGame>(machine.state.value)
    }

    @Test
    fun `reset returns to Idle`() {
        machine.onGameSnapshotReceived(GameSnapshot(gameData = GameData()))
        machine.reset()
        assertIs<GameState.Idle>(machine.state.value)
    }
}
