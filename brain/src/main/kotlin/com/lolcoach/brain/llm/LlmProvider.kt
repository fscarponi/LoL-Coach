package com.lolcoach.brain.llm

/**
 * Interfaccia astratta per provider LLM.
 * Supporta qualsiasi backend OpenAI-compatible (OpenAI, Ollama, LM Studio, ecc.)
 */
interface LlmProvider {
    /**
     * Invia un prompt al modello e restituisce la risposta testuale.
     * @param systemPrompt istruzioni di sistema per il modello
     * @param userPrompt il messaggio utente
     * @return la risposta generata dal modello
     */
    suspend fun chat(systemPrompt: String, userPrompt: String): String
}
