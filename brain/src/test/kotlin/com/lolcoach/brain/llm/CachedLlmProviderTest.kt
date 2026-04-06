package com.lolcoach.brain.llm

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CachedLlmProviderTest {

    private class CountingProvider : LlmProvider {
        var callCount = 0
        override suspend fun chat(systemPrompt: String, userPrompt: String): String {
            callCount++
            return "response-$callCount"
        }
    }

    @Test
    fun `cache returns same response for identical prompts`() = runTest {
        val delegate = CountingProvider()
        val cached = CachedLlmProvider(delegate)

        val r1 = cached.chat("sys", "user")
        val r2 = cached.chat("sys", "user")

        assertEquals(r1, r2)
        assertEquals(1, delegate.callCount)
    }

    @Test
    fun `different prompts produce different cache entries`() = runTest {
        val delegate = CountingProvider()
        val cached = CachedLlmProvider(delegate)

        val r1 = cached.chat("sys", "prompt1")
        val r2 = cached.chat("sys", "prompt2")

        assertEquals("response-1", r1)
        assertEquals("response-2", r2)
        assertEquals(2, delegate.callCount)
        assertEquals(2, cached.cacheSize())
    }

    @Test
    fun `invalidate clears cache`() = runTest {
        val delegate = CountingProvider()
        val cached = CachedLlmProvider(delegate)

        cached.chat("sys", "user")
        assertEquals(1, cached.cacheSize())

        cached.invalidate()
        assertEquals(0, cached.cacheSize())

        cached.chat("sys", "user")
        assertEquals(2, delegate.callCount)
    }

    @Test
    fun `eviction occurs when max size is reached`() = runTest {
        val delegate = CountingProvider()
        val cached = CachedLlmProvider(delegate, maxCacheSize = 3)

        cached.chat("sys", "a")
        cached.chat("sys", "b")
        cached.chat("sys", "c")
        assertEquals(3, cached.cacheSize())

        cached.chat("sys", "d")
        assertEquals(3, cached.cacheSize())
        assertEquals(4, delegate.callCount)
    }
}
