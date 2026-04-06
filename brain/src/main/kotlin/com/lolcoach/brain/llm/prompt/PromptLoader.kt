package com.lolcoach.brain.llm.prompt

import com.lolcoach.brain.state.GameMode

/**
 * Loads system prompts from .md files in resources.
 * Fallback to inline prompts if the file is not available.
 */
object PromptLoader {

    private val cache = mutableMapOf<GameMode, String>()

    /**
     * Returns the system prompt for the specified game mode.
     */
    fun getSystemPrompt(gameMode: GameMode): String {
        return cache.getOrPut(gameMode) { loadFromResources(gameMode) }
    }

    private fun loadFromResources(gameMode: GameMode): String {
        val fileName = when (gameMode) {
            GameMode.SUMMONERS_RIFT -> "prompts/SystemPrompt_SummonersRift.md"
            GameMode.ARAM -> "prompts/SystemPrompt_ARAM.md"
            GameMode.ARAM_MAYHEM -> "prompts/SystemPrompt_AramMayhem.md"
            GameMode.UNKNOWN -> "prompts/SystemPrompt_SummonersRift.md"
        }
        return try {
            val stream = this::class.java.classLoader.getResourceAsStream(fileName)
            stream?.bufferedReader()?.readText() ?: fallbackPrompt(gameMode)
        } catch (_: Exception) {
            fallbackPrompt(gameMode)
        }
    }

    private fun fallbackPrompt(gameMode: GameMode): String = buildString {
        appendLine("You are an expert League of Legends coach, specializing in the Support role.")
        appendLine("Always answer in English. Be concise but strategically precise.")
        appendLine("Mode: ${gameMode.displayName}.")
        appendLine()
        appendLine("Respond with these 4 sections (one per line):")
        appendLine("[COMP] Team composition analysis")
        appendLine("[WIN] Win condition from a Support perspective")
        appendLine("[AVOID] What to avoid")
        appendLine("[PRIORITY] Key priorities as Support")
        appendLine("Each section max 150 characters, no markdown.")
    }
}
