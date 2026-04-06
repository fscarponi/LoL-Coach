package com.lolcoach.brain.llm

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.llm.model.AnalysisSection
import com.lolcoach.brain.llm.model.LlmResponse
import com.lolcoach.brain.llm.model.RequestBuilder
import com.lolcoach.brain.llm.prompt.PromptLoader
import com.lolcoach.brain.state.GameMode
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Servizio che usa un LLM per generare analisi approfondite durante champ select e loading.
 * Produce GameEvent.LlmAnalysis con sezioni: Analisi Comp, Win Condition, Cosa Evitare, Priorità.
 *
 * Usa PromptLoader per caricare i system prompt dai file .md per modalità,
 * e RequestBuilder per costruire richieste strutturate.
 */
class LlmCoachService(
    private val scope: CoroutineScope,
    private val llmProvider: LlmProvider
) {
    private val _analysisEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val analysisEvents: SharedFlow<GameEvent> = _analysisEvents.asSharedFlow()

    private var lastAnalyzedSession: String = ""

    private var currentGameMode: GameMode = GameMode.UNKNOWN

    fun setGameMode(mode: GameMode) {
        currentGameMode = mode
    }

    companion object {
        /**
         * Costruisce il system prompt per la modalità specificata.
         * Usa PromptLoader per caricare dai file .md nelle resources.
         */
        fun buildSystemPrompt(gameMode: GameMode): String = PromptLoader.getSystemPrompt(gameMode)

        // Backward compatibility
        val SYSTEM_PROMPT = buildSystemPrompt(GameMode.SUMMONERS_RIFT)
    }

    /**
     * Analizza la champion select corrente tramite LLM.
     * Evita analisi duplicate per la stessa sessione.
     */
    fun analyzeChampSelect(session: ChampSelectSession) {
        val sessionKey = buildSessionKey(session)
        if (sessionKey == lastAnalyzedSession || sessionKey.isBlank()) return
        lastAnalyzedSession = sessionKey

        scope.launch {
            try {
                val request = RequestBuilder.fromChampSelect(session, currentGameMode)
                val userPrompt = RequestBuilder.toUserPrompt(request)
                val systemPrompt = buildSystemPrompt(currentGameMode)
                val rawResponse = llmProvider.chat(systemPrompt, userPrompt)
                val response = parseResponse(rawResponse)
                emitAnalysis(response)
            } catch (e: Exception) {
                _analysisEvents.emit(
                    GameEvent.LlmAnalysis("ERRORE", "LLM non disponibile: ${e.message?.take(80)}")
                )
            }
        }
    }

    /**
     * Reset per nuova partita.
     */
    fun reset() {
        lastAnalyzedSession = ""
    }

    private fun buildSessionKey(session: ChampSelectSession): String {
        val myChamps = session.myTeam
            .filter { it.championId > 0 }
            .sortedBy { it.championId }
            .joinToString(",") { it.championId.toString() }
        val theirChamps = session.theirTeam
            .filter { it.championId > 0 }
            .sortedBy { it.championId }
            .joinToString(",") { it.championId.toString() }
        return if (myChamps.isNotEmpty() || theirChamps.isNotEmpty()) "$myChamps|$theirChamps" else ""
    }

    /**
     * Parsa la risposta grezza dell'LLM in un LlmResponse strutturato.
     */
    internal fun parseResponse(rawResponse: String): LlmResponse {
        val sections = mutableListOf<AnalysisSection>()
        val lines = rawResponse.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            for ((tag, label) in AnalysisSection.KNOWN_TAGS) {
                if (line.startsWith("[$tag]", ignoreCase = true)) {
                    val content = line.removePrefix("[$tag]").trim()
                    if (content.isNotBlank()) {
                        sections.add(AnalysisSection(tag, label, content))
                    }
                }
            }
        }

        return LlmResponse(
            sections = sections,
            rawText = rawResponse
        )
    }

    private suspend fun emitAnalysis(response: LlmResponse) {
        if (response.sections.isNotEmpty()) {
            for (section in response.sections) {
                _analysisEvents.emit(GameEvent.LlmAnalysis(section.label, section.content))
            }
        } else {
            // Fallback: il modello non ha rispettato il formato
            _analysisEvents.emit(GameEvent.LlmAnalysis("Analisi LLM", response.rawText.take(500)))
        }
    }
}
