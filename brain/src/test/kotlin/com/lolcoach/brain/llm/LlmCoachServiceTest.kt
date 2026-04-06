package com.lolcoach.brain.llm

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.model.lcu.ChampSelectPlayerSelection
import com.lolcoach.model.lcu.ChampSelectSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LlmCoachServiceTest {

    /** Fake LLM provider che restituisce una risposta predefinita */
    class FakeLlmProvider(private val response: String) : LlmProvider {
        var lastSystemPrompt: String = ""
        var lastUserPrompt: String = ""
        var callCount = 0

        override suspend fun chat(systemPrompt: String, userPrompt: String): String {
            lastSystemPrompt = systemPrompt
            lastUserPrompt = userPrompt
            callCount++
            return response
        }
    }

    private fun createSession(
        myTeam: List<Pair<Int, String>> = emptyList(),
        theirTeam: List<Pair<Int, String>> = emptyList()
    ): ChampSelectSession {
        return ChampSelectSession(
            myTeam = myTeam.map { (id, pos) ->
                ChampSelectPlayerSelection(championId = id, assignedPosition = pos)
            },
            theirTeam = theirTeam.map { (id, pos) ->
                ChampSelectPlayerSelection(championId = id, assignedPosition = pos)
            }
        )
    }

    @Test
    fun `parses structured LLM response into 4 analysis events`() = runTest {
        val fakeResponse = """
            [COMP] Allied team has good engage with Leona, enemies have heavy poke with Xerath
            [WIN] Force fights in narrow spaces where enemy poke is less effective
            [AVOID] Do not take long trades in lane against Xerath, avoid being poked down
            [PRIORITY] Engage at level 2, control bot bushes, protect ADC from poke
        """.trimIndent()

        val provider = FakeLlmProvider(fakeResponse)
        val service = LlmCoachService(this, provider)

        val collected = mutableListOf<GameEvent>()
        val job = launch {
            service.analysisEvents.collect { collected.add(it) }
        }

        val session = createSession(
            myTeam = listOf(89 to "utility", 222 to "bottom"),
            theirTeam = listOf(101 to "utility", 51 to "bottom")
        )
        service.analyzeChampSelect(session)

        // Wait for events to be emitted
        kotlinx.coroutines.delay(100)
        job.cancel()

        assertEquals(4, collected.size)
        assertTrue(collected.all { it is GameEvent.LlmAnalysis })

        val sections = collected.map { (it as GameEvent.LlmAnalysis).section }
        assertTrue("Comp Analysis" in sections)
        assertTrue("Win Condition" in sections)
        assertTrue("What to Avoid" in sections)
        assertTrue("Priority" in sections)
    }

    @Test
    fun `deduplicates same session`() = runTest {
        val provider = FakeLlmProvider("[COMP] Test analysis")
        val service = LlmCoachService(this, provider)

        val session = createSession(
            myTeam = listOf(89 to "utility"),
            theirTeam = listOf(101 to "utility")
        )

        service.analyzeChampSelect(session)
        service.analyzeChampSelect(session) // same session, should be skipped

        kotlinx.coroutines.delay(100)
        assertEquals(1, provider.callCount)
    }

    @Test
    fun `analyzes new session after champions change`() = runTest {
        val provider = FakeLlmProvider("[COMP] Test")
        val service = LlmCoachService(this, provider)

        val session1 = createSession(myTeam = listOf(89 to "utility"))
        val session2 = createSession(myTeam = listOf(412 to "utility"))

        service.analyzeChampSelect(session1)
        kotlinx.coroutines.delay(50)
        service.analyzeChampSelect(session2)
        kotlinx.coroutines.delay(50)

        assertEquals(2, provider.callCount)
    }

    @Test
    fun `skips empty session`() = runTest {
        val provider = FakeLlmProvider("[COMP] Test")
        val service = LlmCoachService(this, provider)

        val emptySession = createSession()
        service.analyzeChampSelect(emptySession)

        kotlinx.coroutines.delay(50)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun `handles LLM error gracefully`() = runTest {
        val provider = object : LlmProvider {
            override suspend fun chat(systemPrompt: String, userPrompt: String): String {
                throw RuntimeException("Connection refused")
            }
        }
        val service = LlmCoachService(this, provider)

        val collected = mutableListOf<GameEvent>()
        val job = launch {
            service.analysisEvents.collect { collected.add(it) }
        }

        val session = createSession(myTeam = listOf(89 to "utility"))
        service.analyzeChampSelect(session)

        kotlinx.coroutines.delay(100)
        job.cancel()

        assertEquals(1, collected.size)
        val event = collected[0] as GameEvent.LlmAnalysis
        assertEquals("ERROR", event.section)
        assertTrue(event.content.contains("Connection refused"))
    }

    @Test
    fun `falls back to raw response when format not respected`() = runTest {
        val provider = FakeLlmProvider("This is an unstructured response from the model")
        val service = LlmCoachService(this, provider)

        val collected = mutableListOf<GameEvent>()
        val job = launch {
            service.analysisEvents.collect { collected.add(it) }
        }

        val session = createSession(myTeam = listOf(89 to "utility"))
        service.analyzeChampSelect(session)

        kotlinx.coroutines.delay(100)
        job.cancel()

        assertEquals(1, collected.size)
        val event = collected[0] as GameEvent.LlmAnalysis
        assertEquals("LLM Analysis", event.section)
        assertTrue(event.content.contains("unstructured response"))
    }

    @Test
    fun `reset allows re-analysis of same session`() = runTest {
        val provider = FakeLlmProvider("[COMP] Test")
        val service = LlmCoachService(this, provider)

        val session = createSession(myTeam = listOf(89 to "utility"))

        service.analyzeChampSelect(session)
        kotlinx.coroutines.delay(50)
        service.reset()
        service.analyzeChampSelect(session)
        kotlinx.coroutines.delay(50)

        assertEquals(2, provider.callCount)
    }

    @Test
    fun `prompt contains champion names and roles`() = runTest {
        val provider = FakeLlmProvider("[COMP] Ok")
        val service = LlmCoachService(this, provider)

        val session = createSession(
            myTeam = listOf(89 to "utility", 222 to "bottom"),
            theirTeam = listOf(412 to "utility")
        )
        service.analyzeChampSelect(session)

        kotlinx.coroutines.delay(100)
        assertTrue(provider.lastUserPrompt.contains("Leona"))
        assertTrue(provider.lastUserPrompt.contains("Jinx"))
        assertTrue(provider.lastUserPrompt.contains("Thresh"))
        assertTrue(provider.lastUserPrompt.contains("Support"))
    }
}
