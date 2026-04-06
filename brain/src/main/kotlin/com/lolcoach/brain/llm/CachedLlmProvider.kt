package com.lolcoach.brain.llm

import java.util.concurrent.ConcurrentHashMap

/**
 * Decorator that caches LLM responses based on the hash of (systemPrompt + userPrompt).
 * Uses an LRU-like eviction strategy with a configurable max cache size.
 * Thread-safe via ConcurrentHashMap.
 */
class CachedLlmProvider(
    private val delegate: LlmProvider,
    private val maxCacheSize: Int = 50
) : LlmProvider {

    private data class CacheEntry(
        val response: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        val key = buildCacheKey(systemPrompt, userPrompt)

        cache[key]?.let { entry ->
            return entry.response
        }

        val response = delegate.chat(systemPrompt, userPrompt)

        // Evict oldest entries if cache is full
        if (cache.size >= maxCacheSize) {
            val oldest = cache.entries.minByOrNull { it.value.timestamp }
            oldest?.let { cache.remove(it.key) }
        }

        cache[key] = CacheEntry(response)
        return response
    }

    fun invalidate() {
        cache.clear()
    }

    fun cacheSize(): Int = cache.size

    private fun buildCacheKey(systemPrompt: String, userPrompt: String): String {
        // Use hash to avoid storing large prompt strings as keys
        val combined = "$systemPrompt\n---\n$userPrompt"
        return combined.hashCode().toString(36)
    }
}
