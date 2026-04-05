package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    val lastSnapshot by viewModel.lastSnapshot.collectAsState()
    val allEvents by viewModel.allEvents.collectAsState()
    val filteredLogs by viewModel.filteredLogs.collectAsState()
    val selectedLogLevel by viewModel.selectedLogLevel.collectAsState()

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = CardBackground,
            onBackground = TextPrimary,
            onSurface = TextPrimary,
            primary = AccentBlue
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column: Connection + State + Game Info
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConnectionPanel(connectionStatus)
                GameStatePanel(currentState)
                GameInfoPanel(lastSnapshot)
                PlayersPanel(lastSnapshot)
            }

            // Right column: Events + Logs
            Column(
                modifier = Modifier.weight(1.5f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EventsPanel(
                    events = allEvents,
                    modifier = Modifier.weight(0.4f)
                )
                LogPanel(
                    logs = filteredLogs,
                    selectedLevel = selectedLogLevel,
                    onFilterChanged = { viewModel.setLogFilter(it) },
                    modifier = Modifier.weight(0.6f)
                )
            }
        }
    }
}
