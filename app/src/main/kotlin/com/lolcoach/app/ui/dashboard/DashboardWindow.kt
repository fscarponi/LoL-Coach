package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.app.viewmodel.DashboardViewModel

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LolColors.BlueDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection + State + Voice Row (Top priority)
            Row(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ConnectionAndStatePanel(
                    status = connectionStatus,
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
                    downloadState = downloadState,
                    onDownload = { viewModel.downloadModel() },
                    wakeWord = viewModel.wakeWord.collectAsState().value,
                    onWakeWordChanged = { viewModel.updateWakeWord(it) },
                    isListening = viewModel.isListeningForQuery.collectAsState().value,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }

            // Middle Row: Game Info + LLM Analysis
            Row(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GameInfoPanel(
                    snapshot = lastSnapshot,
                    modifier = Modifier.weight(1.5f).fillMaxHeight()
                )
                LlmAnalysisPanel(
                    analyses = llmAnalysis,
                    modifier = Modifier.weight(2f).fillMaxHeight()
                )
            }

            // Bottom Row: Players + Events
            Row(
                modifier = Modifier.fillMaxWidth().height(400.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PlayersPanel(
                    snapshot = lastSnapshot,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                EventsPanel(
                    events = allEvents,
                    modifier = Modifier.weight(2.5f).fillMaxHeight()
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
