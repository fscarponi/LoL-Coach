package com.lolcoach.app.viewmodel

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OverlayViewModelTest {

    private val gameEvents = MutableSharedFlow<GameEvent>()
    private val gameState = MutableStateFlow<GameState>(GameState.Idle)

    @Test
    fun `initial state has no visible events`() = runTest(UnconfinedTestDispatcher()) {
        val vm = OverlayViewModel(backgroundScope, gameEvents, gameState).also { it.start() }
        assertTrue(vm.visibleEvents.value.isEmpty())
    }

    @Test
    fun `emitted event appears in visible events`() = runTest(UnconfinedTestDispatcher()) {
        val vm = OverlayViewModel(backgroundScope, gameEvents, gameState).also { it.start() }
        gameEvents.emit(GameEvent.GenericTip("Test tip"))
        assertEquals(1, vm.visibleEvents.value.size)
        assertEquals("Test tip", vm.visibleEvents.value.first().event.message)
    }

    @Test
    fun `events are capped at MAX_VISIBLE_EVENTS`() = runTest(UnconfinedTestDispatcher()) {
        val vm = OverlayViewModel(backgroundScope, gameEvents, gameState).also { it.start() }
        repeat(OverlayViewModel.MAX_VISIBLE_EVENTS + 3) { i ->
            gameEvents.emit(GameEvent.GenericTip("Tip $i"))
        }
        assertEquals(OverlayViewModel.MAX_VISIBLE_EVENTS, vm.visibleEvents.value.size)
    }

    @Test
    fun `newest event is first in list`() = runTest(UnconfinedTestDispatcher()) {
        val vm = OverlayViewModel(backgroundScope, gameEvents, gameState).also { it.start() }
        gameEvents.emit(GameEvent.GenericTip("First"))
        gameEvents.emit(GameEvent.GenericTip("Second"))
        assertEquals("Second", vm.visibleEvents.value.first().event.message)
    }

    @Test
    fun `currentState reflects game state changes`() = runTest(UnconfinedTestDispatcher()) {
        val vm = OverlayViewModel(backgroundScope, gameEvents, gameState).also { it.start() }
        assertEquals(GameState.Idle, vm.currentState.value)

        gameState.value = GameState.InGame
        assertEquals(GameState.InGame, vm.currentState.value)
    }
}
