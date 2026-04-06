package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lolcoach.app.logging.LogEntry
import com.lolcoach.app.logging.LogLevel
import com.lolcoach.app.viewmodel.ConnectionStatus
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot

val DarkBackground = Color(0xFF_0D1117)
val CardBackground = Color(0xFF_161B22)
val BorderColor = Color(0xFF_30363D)
val AccentBlue = Color(0xFF_58A6FF)
val AccentGreen = Color(0xFF_3FB950)
val AccentOrange = Color(0xFF_D29922)
val AccentRed = Color(0xFF_F85149)
val TextPrimary = Color(0xFF_C9D1D9)
val TextSecondary = Color(0xFF_8B949E)

@Composable
fun DashboardContent(viewModel: DashboardViewModel) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentState by viewModel.currentState.collectAsState()
    val currentGameMode by viewModel.currentGameMode.collectAsState()
    val lastSnapshot by viewModel.lastSnapshot.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val llmAnalysis by viewModel.llmAnalysis.collectAsState()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val selectedLogLevel by viewModel.selectedLogLevel.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = CardBackground,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            primary = AccentBlue
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Connection + State + Voice Row (Top priority)
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ConnectionPanel(
                        status = connectionStatus,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    GameStatePanel(
                        state = currentState,
                        gameMode = currentGameMode,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    VoiceSettingsPanel(
                        enabled = voiceEnabled,
                        onToggle = { viewModel.toggleVoice(it) },
                        availableDevices = availableDevices,
                        selectedDevice = selectedDevice,
                        onDeviceSelected = { viewModel.setVoiceDevice(it) },
                        onRefresh = { viewModel.refreshDevices() },
                        modifier = Modifier.weight(1.5f).fillMaxHeight()
                    )
                }

                // Middle Row: Game Info + LLM Analysis
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameInfoPanel(
                        snapshot = lastSnapshot,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    LlmAnalysisPanel(
                        analyses = llmAnalysis,
                        modifier = Modifier.weight(2.5f).fillMaxHeight().heightIn(min = 200.dp, max = 400.dp)
                    )
                }

                // Bottom Row: Players + Events
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayersPanel(
                        snapshot = lastSnapshot,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    EventsPanel(
                        events = allEvents,
                        modifier = Modifier.weight(2.5f).fillMaxHeight().heightIn(min = 150.dp, max = 300.dp)
                    )
                }

                // Logs (Full width at bottom)
                LogPanel(
                    logs = filteredLogs,
                    selectedLevel = selectedLogLevel,
                    onFilterChanged = { viewModel.setLogFilter(it) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp)
                )
            }
        }
    }
}
