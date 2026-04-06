package com.lolcoach.brain.state

import kotlin.test.Test
import kotlin.test.assertEquals

class GameModeTest {

    @Test
    fun `ARAM gameMode returns ARAM`() {
        assertEquals(GameMode.ARAM, GameMode.fromApiData("ARAM", "Map12", 12))
    }

    @Test
    fun `ARAM gameMode without map info returns ARAM`() {
        assertEquals(GameMode.ARAM, GameMode.fromApiData("ARAM"))
    }

    @Test
    fun `ARAM with MAYHEM in mapName returns ARAM_MAYHEM`() {
        assertEquals(GameMode.ARAM_MAYHEM, GameMode.fromApiData("ARAM", "Mayhem", 12))
    }

    @Test
    fun `CLASSIC with map 11 returns SUMMONERS_RIFT`() {
        assertEquals(GameMode.SUMMONERS_RIFT, GameMode.fromApiData("CLASSIC", "Map11", 11))
    }

    @Test
    fun `CLASSIC without map info returns SUMMONERS_RIFT`() {
        assertEquals(GameMode.SUMMONERS_RIFT, GameMode.fromApiData("CLASSIC"))
    }

    @Test
    fun `map 12 without ARAM gameMode returns ARAM`() {
        assertEquals(GameMode.ARAM, GameMode.fromApiData("UNKNOWN_MODE", "", 12))
    }

    @Test
    fun `unknown gameMode and map returns UNKNOWN`() {
        assertEquals(GameMode.UNKNOWN, GameMode.fromApiData("CHERRY", "", 0))
    }

    @Test
    fun `case insensitive gameMode`() {
        assertEquals(GameMode.ARAM, GameMode.fromApiData("aram"))
    }

    @Test
    fun `displayName is correct`() {
        assertEquals("Summoner's Rift", GameMode.SUMMONERS_RIFT.displayName)
        assertEquals("ARAM", GameMode.ARAM.displayName)
        assertEquals("ARAM Mayhem", GameMode.ARAM_MAYHEM.displayName)
        assertEquals("Sconosciuta", GameMode.UNKNOWN.displayName)
    }
}
