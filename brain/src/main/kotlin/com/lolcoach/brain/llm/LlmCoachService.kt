package com.lolcoach.brain.llm

import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.strategy.ChampSelectStrategy.Companion.CHAMPION_NAMES
import com.lolcoach.bridge.model.lcu.ChampSelectSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Servizio che usa un LLM per generare analisi approfondite durante champ select e loading.
 * Produce GameEvent.LlmAnalysis con sezioni: Analisi Comp, Win Condition, Cosa Evitare, Priorità.
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
        fun buildSystemPrompt(gameMode: GameMode): String {
            val modeContext = when (gameMode) {
                GameMode.ARAM, GameMode.ARAM_MAYHEM -> """
                    La partita è in modalità ${gameMode.displayName} (Howling Abyss, corsia singola, 5v5 teamfight costanti).
                    Adatta i consigli: non ci sono lane, jungle, dragon o baron. Focus su teamfight, poke, engage/disengage,
                    gestione health pack, timing dei back (morte = unico modo per comprare), e sinergie di squadra in fight.
                """.trimIndent()
                GameMode.SUMMONERS_RIFT -> """
                    La partita è in modalità Summoner's Rift (mappa classica 5v5 con lane, jungle e obiettivi).
                    Adatta i consigli al ruolo Support in bot lane: visione, roaming, obiettivi, peeling.
                """.trimIndent()
                GameMode.UNKNOWN -> "Modalità di gioco non ancora determinata. Dai consigli generali."
            }
            return """
                Sei un coach esperto di League of Legends, specializzato nel ruolo Support.
                Rispondi SEMPRE in italiano. Sii conciso ma strategicamente preciso.
                
                $modeContext
                
                Quando ricevi la composizione dei team, analizza e rispondi con ESATTAMENTE queste 4 sezioni,
                usando questo formato (una sezione per riga, prefissata dal tag):
                
                [COMP] Breve analisi della composizione di entrambi i team (punti di forza e debolezza)
                [WIN] Win condition principale per il tuo team dal punto di vista del Support
                [EVITA] Cosa evitare assolutamente in questa partita (errori critici)
                [PRIORITA] Le 2-3 priorità chiave su cui concentrarsi come Support
                
                Ogni sezione deve essere una singola riga di massimo 150 caratteri.
                Non usare elenchi puntati, asterischi o formattazione markdown.
            """.trimIndent()
        }

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
                val prompt = buildChampSelectPrompt(session)
                val systemPrompt = buildSystemPrompt(currentGameMode)
                val response = llmProvider.chat(systemPrompt, prompt)
                parseAndEmitAnalysis(response)
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

    private fun buildChampSelectPrompt(session: ChampSelectSession): String {
        val myTeam = session.myTeam.mapNotNull { player ->
            if (player.championId > 0) {
                val name = CHAMPION_NAMES[player.championId] ?: "Champion#${player.championId}"
                val role = player.assignedPosition.ifBlank { "?" }
                "$name ($role)"
            } else null
        }

        val theirTeam = session.theirTeam.mapNotNull { player ->
            if (player.championId > 0) {
                val name = CHAMPION_NAMES[player.championId] ?: "Champion#${player.championId}"
                val role = player.assignedPosition.ifBlank { "?" }
                "$name ($role)"
            } else null
        }

        return buildString {
            appendLine("Il mio team: ${myTeam.joinToString(", ").ifEmpty { "non ancora selezionato" }}")
            appendLine("Team nemico: ${theirTeam.joinToString(", ").ifEmpty { "non ancora visibile" }}")
            appendLine("Io gioco Support.")
            appendLine("Analizza la composizione e dammi i consigli strategici.")
        }
    }

    private suspend fun parseAndEmitAnalysis(response: String) {
        val sectionMap = mapOf(
            "[COMP]" to "Analisi Comp",
            "[WIN]" to "Win Condition",
            "[EVITA]" to "Cosa Evitare",
            "[PRIORITA]" to "Priorità"
        )

        val lines = response.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            for ((tag, sectionName) in sectionMap) {
                if (line.startsWith(tag, ignoreCase = true)) {
                    val content = line.removePrefix(tag).trim()
                    if (content.isNotBlank()) {
                        _analysisEvents.emit(GameEvent.LlmAnalysis(sectionName, content))
                    }
                }
            }
        }

        // Se il modello non ha rispettato il formato, emetti la risposta grezza
        if (lines.none { line -> sectionMap.keys.any { line.startsWith(it, ignoreCase = true) } }) {
            _analysisEvents.emit(GameEvent.LlmAnalysis("Analisi LLM", response.take(500)))
        }
    }
}
