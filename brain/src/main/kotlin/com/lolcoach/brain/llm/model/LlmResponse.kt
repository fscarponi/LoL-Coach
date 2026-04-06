package com.lolcoach.brain.llm.model

import kotlinx.serialization.Serializable

/**
 * Modello astratto della risposta dall'LLM, strutturata in sezioni.
 */
@Serializable
data class LlmResponse(
    val sections: List<AnalysisSection> = emptyList(),
    val rawText: String = ""
)

@Serializable
data class AnalysisSection(
    val tag: String,
    val label: String,
    val content: String
) {
    companion object {
        /** Tag supportati e le loro label leggibili */
        val KNOWN_TAGS = mapOf(
            "COMP" to "Analisi Comp",
            "WIN" to "Win Condition",
            "EVITA" to "Cosa Evitare",
            "PRIORITA" to "Priorità"
        )
    }
}
