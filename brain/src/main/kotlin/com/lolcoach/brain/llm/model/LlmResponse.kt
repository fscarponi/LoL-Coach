package com.lolcoach.brain.llm.model

import kotlinx.serialization.Serializable

/**
 * Abstract model of the response from the LLM, structured in sections.
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
        /** Supported tags and their readable labels */
        val KNOWN_TAGS = mapOf(
            "COMP" to "Comp Analysis",
            "WIN" to "Win Condition",
            "AVOID" to "What to Avoid",
            "PRIORITY" to "Priority"
        )
    }
}
