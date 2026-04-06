package com.lolcoach.brain.llm.prompt

import com.lolcoach.brain.state.GameMode

/**
 * Carica i system prompt dai file .md nelle resources.
 * Fallback a prompt inline se il file non è disponibile.
 */
object PromptLoader {

    private val cache = mutableMapOf<GameMode, String>()

    /**
     * Restituisce il system prompt per la modalità specificata.
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
        appendLine("Sei un coach esperto di League of Legends, specializzato nel ruolo Support.")
        appendLine("Rispondi SEMPRE in italiano. Sii conciso ma strategicamente preciso.")
        appendLine("Modalità: ${gameMode.displayName}.")
        appendLine()
        appendLine("Rispondi con queste 4 sezioni (una per riga):")
        appendLine("[COMP] Analisi composizione team")
        appendLine("[WIN] Win condition dal punto di vista Support")
        appendLine("[EVITA] Cosa evitare")
        appendLine("[PRIORITA] Priorità chiave come Support")
        appendLine("Ogni sezione max 150 caratteri, niente markdown.")
    }
}
