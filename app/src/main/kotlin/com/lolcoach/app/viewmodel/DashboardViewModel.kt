package com.lolcoach.app.viewmodel

import com.lolcoach.app.logging.AppLogger
import com.lolcoach.app.logging.LogEntry
import com.lolcoach.app.logging.LogLevel
import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.LockfileData
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ConnectionStatus(
    val lockfileFound: Boolean = false,
    val lockfilePort: Int? = null,
    val liveClientActive: Boolean = false,
    val lastSnapshotTime: Long? = null
)

class DashboardViewModel(
    private val scope: CoroutineScope,
    private val gameEvents: SharedFlow<GameEvent>,
    private val gameState: StateFlow<GameState>,
    private val gameModeFlow: StateFlow<GameMode>,
    private val lockfileData: StateFlow<LockfileData?>,
    private val gameSnapshots: SharedFlow<GameSnapshot>
) {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus())
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    val currentState: StateFlow<GameState> = gameState
    val currentGameMode: StateFlow<GameMode> = gameModeFlow

    private val _lastSnapshot = MutableStateFlow<GameSnapshot?>(null)
    val lastSnapshot: StateFlow<GameSnapshot?> = _lastSnapshot.asStateFlow()

    private val _allEvents = MutableStateFlow<List<TimestampedGameEvent>>(emptyList())
    val allEvents: StateFlow<List<TimestampedGameEvent>> = _allEvents.asStateFlow()

    private val _llmAnalysis = MutableStateFlow<List<GameEvent.LlmAnalysis>>(emptyList())
    val llmAnalysis: StateFlow<List<GameEvent.LlmAnalysis>> = _llmAnalysis.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = AppLogger.logs

    private val _selectedLogLevel = MutableStateFlow<LogLevel?>(null)
    val selectedLogLevel: StateFlow<LogLevel?> = _selectedLogLevel.asStateFlow()

    val filteredLogs: StateFlow<List<LogEntry>> = combine(logs, _selectedLogLevel) { allLogs, level ->
        if (level == null) allLogs else allLogs.filter { it.level == level }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun setLogFilter(level: LogLevel?) {
        _selectedLogLevel.value = level
    }

    fun start() {
        // Track lockfile
        scope.launch {
            lockfileData.collect { data ->
                _connectionStatus.value = _connectionStatus.value.copy(
                    lockfileFound = data != null,
                    lockfilePort = data?.port
                )
                if (data != null) {
                    AppLogger.info("Bridge", "Lockfile found — port ${data.port}, PID ${data.pid}")
                } else {
                    AppLogger.warn("Bridge", "Lockfile not found — LoL client not active")
                }
            }
        }

        // Track game snapshots
        scope.launch {
            gameSnapshots.collect { snapshot ->
                _lastSnapshot.value = snapshot
                _connectionStatus.value = _connectionStatus.value.copy(
                    liveClientActive = true,
                    lastSnapshotTime = System.currentTimeMillis()
                )
                val gt = snapshot.gameData?.gameTime?.let { "%.1f".format(it) } ?: "?"
                val playerCount = snapshot.allPlayers.size
                AppLogger.debug("LiveClient", "Snapshot received — gameTime=$gt, players=$playerCount")
            }
        }

        // Track game events
        scope.launch {
            gameEvents.collect { event ->
                val entry = TimestampedGameEvent(event, System.currentTimeMillis())
                val current = _allEvents.value.toMutableList()
                current.add(0, entry)
                if (current.size > 100) current.removeAt(current.lastIndex)
                _allEvents.value = current
                AppLogger.event("Strategy", event.message)

                // Track LLM analysis separately
                if (event is GameEvent.LlmAnalysis) {
                    val currentLlm = _llmAnalysis.value.toMutableList()
                    currentLlm.add(event)
                    if (currentLlm.size > 20) currentLlm.removeAt(0)
                    _llmAnalysis.value = currentLlm
                }
            }
        }

        // Track game state changes
        scope.launch {
            gameState.collect { state ->
                val stateName = when (state) {
                    is GameState.Idle -> "Idle"
                    is GameState.ChampSelect -> "ChampSelect"
                    is GameState.Loading -> "Loading"
                    is GameState.InGame -> "InGame"
                    is GameState.PostGame -> "PostGame"
                }
                AppLogger.info("StateMachine", "State changed → $stateName")
            }
        }

        AppLogger.info("App", "LoL Support Strategist started")
    }

    data class TimestampedGameEvent(
        val event: GameEvent,
        val timestamp: Long
    )
}
