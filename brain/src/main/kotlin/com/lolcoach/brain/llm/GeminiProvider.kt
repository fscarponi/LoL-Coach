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
 * Provider LLM per Google Gemini API.
 * Documentazione: https://ai.google.dev/api/rest/v1beta/models/generateContent
 *
 * @param model nome del modello (es. "gemini-1.5-flash", "gemini-1.5-pro")
 * @param apiKey chiave API di Google AI Studio (necessaria)
 * @param temperature temperatura di generazione (0.0 - 2.0)
 * @param maxTokens numero massimo di token in output
 */
class GeminiProvider(
    private val model: String,
    private val apiKey: String = "",
    private val temperature: Double = 0.7,
    private val maxTokens: Int = 1024,
    private val httpClient: HttpClient? = null
) : LlmProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val internalClient = httpClient ?: HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@GeminiProvider.json)
        }
    }

    override suspend fun chat(systemPrompt: String, userPrompt: String): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API Key is missing. Set it in LlmConfig or GEMINI_API_KEY environment variable.")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val request = GeminiRequest(
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userPrompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens
            )
        )

        val response = internalClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("Gemini API error (${response.status}): $errorBody")
        }

        val body = response.bodyAsText()
        val parsed = json.decodeFromString<GeminiResponse>(body)
        
        return parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
    }

    fun close() {
        if (httpClient == null) {
            internalClient.close()
        }
    }
}

// ── Google Gemini API models ──

@Serializable
data class GeminiRequest(
    @SerialName("contents") val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
data class GeminiSystemInstruction(
    @SerialName("parts") val parts: List<GeminiPart>
)

@Serializable
data class GeminiContent(
    @SerialName("role") val role: String = "model",
    @SerialName("parts") val parts: List<GeminiPart> = emptyList()
)

@Serializable
data class GeminiPart(
    @SerialName("text") val text: String
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("temperature") val temperature: Double? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null,
    @SerialName("topP") val topP: Double? = null,
    @SerialName("topK") val topK: Int? = null
)

@Serializable
data class GeminiResponse(
    @SerialName("candidates") val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(
    @SerialName("content") val content: GeminiContent? = null,
    @SerialName("finishReason") val finishReason: String? = null
)
