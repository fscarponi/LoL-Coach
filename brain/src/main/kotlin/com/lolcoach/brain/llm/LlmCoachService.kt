package com.lolcoach.brain.llm

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.llm.model.AnalysisSection
import com.lolcoach.brain.llm.model.LlmResponse
import com.lolcoach.brain.llm.model.RequestBuilder
import com.lolcoach.brain.llm.prompt.PromptLoader
import com.lolcoach.brain.state.GameMode
import com.lolcoach.model.lcu.ChampSelectSession
import com.lolcoach.model.liveclient.GameSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Service that uses an LLM to generate in-depth analysis during champ select and loading.
 * Produces GameEvent.LlmAnalysis with sections: Comp Analysis, Win Condition, What to Avoid, Priority.
 *
 * Uses PromptLoader to load system prompts from .md files for different game modes,
 * and RequestBuilder to build structured requests.
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
         * Builds the system prompt for the specified game mode.
         * Uses PromptLoader to load from .md files in resources.
         */
        fun buildSystemPrompt(gameMode: GameMode): String = PromptLoader.getSystemPrompt(gameMode)

        // Backward compatibility
        val SYSTEM_PROMPT = buildSystemPrompt(GameMode.SUMMONERS_RIFT)
    }

    /**
     * Analyzes the current champion select via LLM.
     * Avoids duplicate analysis for the same session.
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
                    GameEvent.LlmAnalysis("ERROR", "LLM not available: ${e.message?.take(80)}")
                )
            }
        }
    }

    /**
     * Analyzes the current in-game state via LLM.
     * Throttled: avoids re-analyzing if the game time hasn't changed significantly.
     */
    private var lastAnalyzedGameTime: Double = -1.0

    fun analyzeGameSnapshot(snapshot: GameSnapshot) {
        val gameTime = snapshot.gameData?.gameTime ?: return
        // Only analyze every 60 seconds of game time
        if (gameTime - lastAnalyzedGameTime < 60.0) return
        lastAnalyzedGameTime = gameTime

        scope.launch {
            try {
                val request = RequestBuilder.fromGameSnapshot(snapshot, currentGameMode)
                val userPrompt = RequestBuilder.toUserPrompt(request)
                val systemPrompt = buildSystemPrompt(currentGameMode)
                val rawResponse = llmProvider.chat(systemPrompt, userPrompt)
                val response = parseResponse(rawResponse)
                emitAnalysis(response)
            } catch (e: Exception) {
                _analysisEvents.emit(
                    GameEvent.LlmAnalysis("ERROR", "LLM analysis failed: ${e.message?.take(80)}")
                )
            }
        }
    }

    /**
     * Reset for a new game.
     */
    fun reset() {
        lastAnalyzedGameTime = -1.0
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
     * Handles a direct voice query from the user.
     */
    fun askCoach(query: String) {
        scope.launch {
            try {
                _analysisEvents.emit(GameEvent.UserVoiceQuery(query))
                
                val systemPrompt = buildSystemPrompt(currentGameMode) + 
                    "\n\nYou are answering a direct voice question from the player. " +
                    "Keep it extremely short (max 20 words) and helpful for the current game situation."
                
                val rawResponse = llmProvider.chat(systemPrompt, "User question: $query")
                
                // Emittiamo la risposta come un evento LLM speciale
                _analysisEvents.emit(GameEvent.LlmAnalysis("COACH", rawResponse))
            } catch (e: Exception) {
                _analysisEvents.emit(GameEvent.LlmAnalysis("ERROR", "Coach failed to answer: ${e.message?.take(80)}"))
            }
        }
    }

    /**
     * Parses the raw LLM response into a structured LlmResponse.
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
            // Fallback: the model did not respect the format
            _analysisEvents.emit(GameEvent.LlmAnalysis("LLM Analysis", response.rawText.take(500)))
        }
    }
}
