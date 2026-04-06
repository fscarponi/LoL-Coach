package com.lolcoach.app

import androidx.compose.foundation.layout.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.lolcoach.app.logging.AppLogger
import com.lolcoach.app.ui.OverlayPanel
import com.lolcoach.app.ui.StatusBar
import com.lolcoach.app.ui.dashboard.DashboardContent
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.app.viewmodel.OverlayViewModel
import com.lolcoach.brain.event.EventProcessor
import com.lolcoach.brain.llm.LlmCoachService
import com.lolcoach.brain.llm.LlmConfig
import com.lolcoach.brain.state.GameStateMachine
import com.lolcoach.brain.strategy.StrategyEngine
import com.lolcoach.app.tts.SystemTtsManager
import com.lolcoach.app.tts.TtsEventListener
import com.lolcoach.bridge.BridgeFacade
import com.lolcoach.bridge.voice.VoskModelDownloader
import com.lolcoach.bridge.voice.WakeWordDetector
import java.io.File

fun main() = application {
    val scope = rememberCoroutineScope()

    val stateMachine = remember { GameStateMachine() }
    val strategyEngine = remember { StrategyEngine() }
    val eventProcessor = remember {
        EventProcessor(scope, stateMachine, strategyEngine.allStrategies())
    }
    val bridge = remember { BridgeFacade(scope) }

    val modelPath = remember {
        System.getenv("VOSK_MODEL_PATH") ?: "models/vosk-model"
    }
    val modelDownloader = remember { VoskModelDownloader(modelPath) }

    // LLM Coach Service
    val llmConfig = remember { LlmConfig() }
    val llmCoachService = remember {
        if (llmConfig.enabled) {
            LlmCoachService(scope, llmConfig.createProvider())
        } else null
    }

    val viewModel = remember {
        OverlayViewModel(scope, eventProcessor.events, stateMachine.state).also { it.start() }
    }

    val dashboardViewModel = remember {
        DashboardViewModel(
            scope = scope,
            gameEvents = eventProcessor.events,
            gameState = stateMachine.state,
            gameModeFlow = stateMachine.gameMode,
            lockfileData = bridge.lockfileData,
            gameSnapshots = bridge.gameSnapshots,
            modelDownloader = modelDownloader
        ).also { 
            it.start()
            it.refreshDevices()
        }
    }

    val wakeWordDetector = remember {
        // Points to the root dir, Vosk will find the model inside
        WakeWordDetector(scope, modelPath)
    }

    val ttsListener = remember {
        TtsEventListener(scope, SystemTtsManager(), eventProcessor.events).also { it.start() }
    }

    // Connect bridge to event processor
    remember {
        AppLogger.info("App", "Starting LoL Support Strategist...")
        bridge.start()
        AppLogger.info("Bridge", "Bridge started — lockfile monitoring and polling active")

        scope.launch {
            bridge.gameSnapshots.collect { snapshot ->
                eventProcessor.processGameSnapshot(snapshot)
            }
        }
        scope.launch {
            bridge.champSelectEvents.collect { session ->
                eventProcessor.processChampSelect(session)
                llmCoachService?.analyzeChampSelect(session)
            }
        }
        // Forward LLM analysis events to the main event processor flow
        if (llmCoachService != null) {
            scope.launch {
                llmCoachService.analysisEvents.collect { event ->
                    eventProcessor.emitEvent(event)
                }
            }
            // Sync game mode to LLM coach
            scope.launch {
                stateMachine.gameMode.collect { mode ->
                    llmCoachService.setGameMode(mode)
                }
            }
        }
        scope.launch {
            bridge.lockfileData.collect { data ->
                stateMachine.onLockfileChanged(data)
            }
        }

        // Voice Coaching logic
        scope.launch {
            combine(dashboardViewModel.voiceEnabled, dashboardViewModel.selectedDevice) { enabled, device ->
                enabled to device
            }.collect { (enabled, device) ->
                if (enabled) {
                    AppLogger.info("Voice", "Starting wake-word detector...")
                    wakeWordDetector.start(device)
                } else {
                    wakeWordDetector.stop()
                }
            }
        }

        scope.launch {
            wakeWordDetector.onWakeWordDetected.collect {
                AppLogger.event("Voice", "Wake-word detected! Listening for query...")
                // For now, we simulate a query for testing, or we would start recording STT
                // In a real implementation, we would start AudioBufferRecorder and send to Gemini STT
                llmCoachService?.askCoach("What should I do now?") 
            }
        }
    }

    // ── Dashboard Window (main informative UI) ──
    Window(
        onCloseRequest = {
            ttsListener.stop()
            bridge.stop()
            exitApplication()
        },
        state = WindowState(
            size = DpSize(1200.dp, 800.dp),
            position = WindowPosition(Alignment.Center)
        ),
        title = "LoL Support Strategist — Dashboard",
        resizable = true
    ) {
        DashboardContent(dashboardViewModel)
    }

    // ── Overlay Window (compact, always on top) ──
    val events by viewModel.visibleEvents.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val overlayGameMode by stateMachine.gameMode.collectAsState()

    Window(
        onCloseRequest = { /* close only from dashboard */ },
        state = WindowState(
            size = DpSize(380.dp, 400.dp),
            position = WindowPosition(Alignment.TopEnd)
        ),
        title = "LoL Coach Overlay",
        transparent = true,
        undecorated = true,
        alwaysOnTop = true,
        focusable = false,
        resizable = false
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            StatusBar(currentState, overlayGameMode)
            Spacer(modifier = Modifier.height(8.dp))
            OverlayPanel(events)
        }
    }
}
