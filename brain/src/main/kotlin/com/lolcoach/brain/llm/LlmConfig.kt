package com.lolcoach.brain.llm

/**
 * Configurazione per il provider LLM.
 */
data class LlmConfig(
    val baseUrl: String = "http://localhost:11434/v1",
    val model: String = "llama3",
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024,
    val enabled: Boolean = true
) {
    fun createProvider(): OpenAiCompatibleProvider = OpenAiCompatibleProvider(
        baseUrl = baseUrl,
        model = model,
        apiKey = apiKey,
        temperature = temperature,
        maxTokens = maxTokens
    )
}
