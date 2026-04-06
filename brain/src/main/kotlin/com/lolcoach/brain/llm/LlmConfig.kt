package com.lolcoach.brain.llm

/**
 * Configurazione per il provider LLM.
 */
data class LlmConfig(
    val provider: LlmProviderType = System.getenv("LLM_PROVIDER")?.let { 
        LlmProviderType.entries.find { type -> type.name.equals(it, ignoreCase = true) }
    } ?: LlmProviderType.OPENAI_COMPATIBLE,
    val baseUrl: String = System.getenv("LLM_BASE_URL") ?: "http://localhost:11434/v1",
    val model: String = System.getenv("LLM_MODEL") ?: "",
    val apiKey: String = "",
    val temperature: Double = System.getenv("LLM_TEMPERATURE")?.toDoubleOrNull() ?: 0.7,
    val maxTokens: Int = System.getenv("LLM_MAX_TOKENS")?.toIntOrNull() ?: 1024,
    val enabled: Boolean = System.getenv("LLM_ENABLED")?.toBoolean() ?: true
) {
    fun createProvider(cached: Boolean = true): LlmProvider {
        val base: LlmProvider = when (provider) {
            LlmProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleProvider(
                baseUrl = baseUrl,
                model = model.ifBlank { "llama3" },
                apiKey = apiKey.ifBlank { System.getenv("OPENAI_API_KEY") ?: "" },
                temperature = temperature,
                maxTokens = maxTokens
            )
            LlmProviderType.GEMINI -> GeminiProvider(
                model = model.ifBlank { "gemini-1.5-flash" },
                apiKey = apiKey.ifBlank { System.getenv("GEMINI_API_KEY") ?: "" },
                temperature = temperature,
                maxTokens = maxTokens
            )
        }
        return if (cached) CachedLlmProvider(base) else base
    }
}

enum class LlmProviderType {
    OPENAI_COMPATIBLE,
    GEMINI
}
