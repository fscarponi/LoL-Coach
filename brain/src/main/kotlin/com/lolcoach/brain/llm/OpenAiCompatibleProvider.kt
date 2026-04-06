package com.lolcoach.brain.llm

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Provider LLM compatibile con l'API OpenAI (chat/completions).
 * Funziona con OpenAI, Ollama, LM Studio, LocalAI, vLLM, ecc.
 *
 * @param baseUrl URL base dell'API (es. "http://localhost:11434/v1" per Ollama, "https://api.openai.com/v1" per OpenAI)
 * @param model nome del modello (es. "llama3", "gpt-4o-mini")
 * @param apiKey chiave API (opzionale per provider locali)
 * @param temperature temperatura di generazione (0.0 = deterministico, 1.0 = creativo)
 * @param maxTokens numero massimo di token nella risposta
 */
class OpenAiCompatibleProvider(
    private val baseUrl: String = "http://localhost:11434/v1",
    private val model: String = "llama3",
    private val apiKey: String = "",
    private val temperature: Double = 0.7,
    private val maxTokens: Int = 1024
) : LlmProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@OpenAiCompatibleProvider.json)
        }
    }

    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        val request = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            temperature = temperature,
            maxTokens = maxTokens
        )

        val response = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            if (apiKey.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            setBody(request)
        }

        val body = response.bodyAsText()
        val parsed = json.decodeFromString<ChatCompletionResponse>(body)
        return parsed.choices.firstOrNull()?.message?.content ?: ""
    }

    fun close() {
        client.close()
    }
}

// ── OpenAI Chat Completions API models ──

@Serializable
data class ChatCompletionRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessage>,
    @SerialName("temperature") val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 1024
)

@Serializable
data class ChatMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class ChatCompletionResponse(
    @SerialName("id") val id: String = "",
    @SerialName("choices") val choices: List<ChatChoice> = emptyList(),
    @SerialName("model") val model: String = "",
    @SerialName("usage") val usage: TokenUsage? = null
)

@Serializable
data class ChatChoice(
    @SerialName("index") val index: Int = 0,
    @SerialName("message") val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String = ""
)

@Serializable
data class TokenUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)
