package com.lolcoach.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.brain.state.GameState

enum class SideTab(val label: String, val icon: String) {
    SETTINGS("Settings", "⚙️"),
    LOGS("Logs", "📋")
}

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
    val downloadState by viewModel.modelDownloadState.collectAsState()
    val wakeWord by viewModel.wakeWord.collectAsState()
    val isListening by viewModel.isListeningForQuery.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val lastCoachMessage by viewModel.lastCoachMessage.collectAsState()

    var sideTabOpen by remember { mutableStateOf<SideTab?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(LolColors.BlueDeep)
    ) {
        // ── Top Status Bar ──
        TopStatusBar(
            status = connectionStatus,
            state = currentState,
            gameMode = currentGameMode,
            isListening = isListening,
            lastCoachMessage = lastCoachMessage,
            onSettingsClick = { sideTabOpen = if (sideTabOpen == SideTab.SETTINGS) null else SideTab.SETTINGS },
            onLogsClick = { sideTabOpen = if (sideTabOpen == SideTab.LOGS) null else SideTab.LOGS },
            activeSideTab = sideTabOpen
        )

        // ── Main Content Area ──
        Row(modifier = Modifier.fillMaxSize()) {
            // Main panels — no verticalScroll, panels with LazyColumn use weight()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (currentState) {
                    is GameState.Idle, is GameState.PostGame -> {
                        GameInfoPanel(snapshot = lastSnapshot, modifier = Modifier.fillMaxWidth())
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayersPanel(snapshot = lastSnapshot, modifier = Modifier.weight(1f).fillMaxHeight())
                            LlmAnalysisPanel(analyses = llmAnalysis, modifier = Modifier.weight(1.5f).fillMaxHeight())
                        }
                        EventsPanel(events = allEvents, modifier = Modifier.fillMaxWidth().weight(0.6f))
                    }
                    is GameState.ChampSelect -> {
                        PlayersPanel(snapshot = lastSnapshot, modifier = Modifier.fillMaxWidth())
                        LlmAnalysisPanel(analyses = llmAnalysis, modifier = Modifier.fillMaxWidth().weight(1f))
                        EventsPanel(events = allEvents, modifier = Modifier.fillMaxWidth().weight(0.5f))
                    }
                    is GameState.Loading -> {
                        LlmAnalysisPanel(analyses = llmAnalysis, modifier = Modifier.fillMaxWidth().weight(1f))
                        PlayersPanel(snapshot = lastSnapshot, modifier = Modifier.fillMaxWidth())
                    }
                    is GameState.InGame -> {
                        GameInfoPanel(snapshot = lastSnapshot, modifier = Modifier.fillMaxWidth())
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayersPanel(snapshot = lastSnapshot, modifier = Modifier.weight(1f).fillMaxHeight())
                            EventsPanel(events = allEvents, modifier = Modifier.weight(1.5f).fillMaxHeight())
                        }
                        LlmAnalysisPanel(analyses = llmAnalysis, modifier = Modifier.fillMaxWidth().weight(0.6f))
                    }
                }
            }

            // ── Side Panel (Settings / Logs) ──
            AnimatedVisibility(
                visible = sideTabOpen != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .background(LolColors.Surface)
                        .border(width = 1.dp, color = LolColors.BorderGold, shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (sideTabOpen) {
                        SideTab.SETTINGS -> {
                            SidePanelHeader("Settings", "⚙️") { sideTabOpen = null }
                            // Settings content scrollable (no LazyColumn inside)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SettingsPanel(
                                    settings = appSettings,
                                    onSettingsChanged = { viewModel.updateAppSettings(it) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                VoiceSettingsPanel(
                                    enabled = voiceEnabled,
                                    onToggle = { viewModel.toggleVoice(it) },
                                    availableDevices = availableDevices,
                                    selectedDevice = selectedDevice,
                                    onDeviceSelected = { viewModel.setVoiceDevice(it) },
                                    onRefresh = { viewModel.refreshDevices() },
                                    downloadState = downloadState,
                                    onDownload = { viewModel.downloadModel() },
                                    wakeWord = wakeWord,
                                    onWakeWordChanged = { viewModel.updateWakeWord(it) },
                                    isListening = isListening,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        SideTab.LOGS -> {
                            SidePanelHeader("Logs", "📋") { sideTabOpen = null }
                            // LogPanel has LazyColumn, give bounded height via weight
                            LogPanel(
                                logs = filteredLogs,
                                selectedLevel = selectedLogLevel,
                                onFilterChanged = { viewModel.setLogFilter(it) },
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun SidePanelHeader(title: String, icon: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, fontSize = 14.sp)
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LolColors.Gold,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable { onClose() }
                .background(LolColors.BlueMedium),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", fontSize = 12.sp, color = LolColors.OnSurface.copy(alpha = 0.6f))
        }
    }
}
