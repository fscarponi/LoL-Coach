package com.lolcoach.app.viewmodel

import com.lolcoach.app.logging.LogLevel
import com.lolcoach.app.settings.SettingsRepository
import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.voice.VoskModelDownloader
import com.lolcoach.model.LockfileData
import com.lolcoach.model.liveclient.GameData
import com.lolcoach.model.liveclient.GameSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private fun createFlows() = object {
        val gameEvents = MutableSharedFlow<GameEvent>()
        val gameState = MutableStateFlow<GameState>(GameState.Idle)
        val gameModeFlow = MutableStateFlow(GameMode.UNKNOWN)
        val lockfileData = MutableStateFlow<LockfileData?>(null)
        val gameSnapshots = MutableSharedFlow<GameSnapshot>()
    }

    private fun createViewModel(
        scope: kotlinx.coroutines.CoroutineScope,
        gameEvents: MutableSharedFlow<GameEvent>,
        gameState: MutableStateFlow<GameState>,
        gameModeFlow: MutableStateFlow<GameMode>,
        lockfileData: MutableStateFlow<LockfileData?>,
        gameSnapshots: MutableSharedFlow<GameSnapshot>
    ): DashboardViewModel {
        val modelDownloader = VoskModelDownloader("/tmp/test-vosk-model-nonexistent")
        val tempFile = java.io.File.createTempFile("lolcoach-test-settings-", ".json").also { it.delete() }
        val settingsRepo = SettingsRepository(tempFile.absolutePath)
        return DashboardViewModel(
            scope = scope,
            gameEvents = gameEvents,
            gameState = gameState,
            gameModeFlow = gameModeFlow,
            lockfileData = lockfileData,
            gameSnapshots = gameSnapshots,
            modelDownloader = modelDownloader,
            settingsRepository = settingsRepo
        ).also { it.start() }
    }

    @Test
    fun `initial connection status is disconnected`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        val status = vm.connectionStatus.value
        assertFalse(status.lockfileFound)
        assertNull(status.lockfilePort)
        assertFalse(status.liveClientActive)
    }

    @Test
    fun `lockfile data updates connection status`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        f.lockfileData.value = LockfileData("LeagueClient", 1234, 8080, "token", "https")
        val status = vm.connectionStatus.value
        assertTrue(status.lockfileFound)
        assertEquals(8080, status.lockfilePort)
    }

    @Test
    fun `lockfile removal resets connection status`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        f.lockfileData.value = LockfileData("LeagueClient", 1234, 8080, "token", "https")
        assertTrue(vm.connectionStatus.value.lockfileFound)
        f.lockfileData.value = null
        assertFalse(vm.connectionStatus.value.lockfileFound)
    }

    @Test
    fun `game snapshot updates last snapshot and connection status`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        val snapshot = GameSnapshot(gameData = GameData(gameTime = 120.0))
        f.gameSnapshots.emit(snapshot)
        assertEquals(snapshot, vm.lastSnapshot.value)
        assertTrue(vm.connectionStatus.value.liveClientActive)
    }

    @Test
    fun `game events are collected and ordered newest first`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        f.gameEvents.emit(GameEvent.GenericTip("First"))
        f.gameEvents.emit(GameEvent.GenericTip("Second"))
        assertEquals(2, vm.allEvents.value.size)
        assertEquals("Second", vm.allEvents.value.first().event.message)
    }

    @Test
    fun `events list is capped at 100`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        repeat(105) { i ->
            f.gameEvents.emit(GameEvent.GenericTip("Tip $i"))
        }
        assertEquals(100, vm.allEvents.value.size)
    }

    @Test
    fun `LLM analysis events are tracked separately`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        f.gameEvents.emit(GameEvent.LlmAnalysis("Comp Analysis", "Good synergy"))
        assertEquals(1, vm.llmAnalysis.value.size)
        assertEquals("Comp Analysis", vm.llmAnalysis.value.first().section)
    }

    @Test
    fun `non-LLM events do not appear in llmAnalysis`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        f.gameEvents.emit(GameEvent.GenericTip("Not LLM"))
        assertTrue(vm.llmAnalysis.value.isEmpty())
    }

    @Test
    fun `log filter works correctly`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        vm.setLogFilter(LogLevel.ERROR)
        assertEquals(LogLevel.ERROR, vm.selectedLogLevel.value)
    }

    @Test
    fun `voice toggle updates state`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        assertFalse(vm.voiceEnabled.value)
        vm.toggleVoice(true)
        assertTrue(vm.voiceEnabled.value)
        vm.toggleVoice(false)
        assertFalse(vm.voiceEnabled.value)
    }

    @Test
    fun `wake word update works`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        assertEquals("hey coach", vm.wakeWord.value)
        vm.updateWakeWord("ok coach")
        assertEquals("ok coach", vm.wakeWord.value)
    }

    @Test
    fun `blank wake word is rejected`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        vm.updateWakeWord("  ")
        assertEquals("hey coach", vm.wakeWord.value)
    }

    @Test
    fun `currentState reflects game state`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        assertEquals(GameState.Idle, vm.currentState.value)
        f.gameState.value = GameState.ChampSelect
        assertEquals(GameState.ChampSelect, vm.currentState.value)
    }

    @Test
    fun `currentGameMode reflects game mode`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        assertEquals(GameMode.UNKNOWN, vm.currentGameMode.value)
        f.gameModeFlow.value = GameMode.ARAM
        assertEquals(GameMode.ARAM, vm.currentGameMode.value)
    }

    @Test
    fun `isListening state can be set`() = runTest(UnconfinedTestDispatcher()) {
        val f = createFlows()
        val vm = createViewModel(backgroundScope, f.gameEvents, f.gameState, f.gameModeFlow, f.lockfileData, f.gameSnapshots)
        assertFalse(vm.isListeningForQuery.value)
        vm.setIsListening(true)
        assertTrue(vm.isListeningForQuery.value)
    }
}
