package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.i18n.Strings
import com.lolcoach.app.logging.LogEntry
import com.lolcoach.app.logging.LogLevel
import com.lolcoach.app.viewmodel.ConnectionStatus
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import com.lolcoach.bridge.voice.VoiceDevice
import java.text.SimpleDateFormat
import java.util.Date

// ─── Section Header ───────────────────────────────────────────

val BackgroundSecondary = Color(0xFF_161B22)

@Composable
fun SectionHeader(title: String, emoji: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Text(emoji, fontSize = 14.sp)
        Text(
            text = title.uppercase(),
            color = AccentBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun ConnectionPanel(status: ConnectionStatus, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader(Strings.Connection.uppercase(), "🔌")

        StatusRow(
            label = Strings.Lockfile,
            active = status.lockfileFound,
            detail = if (status.lockfileFound) "Port ${status.lockfilePort}" else Strings.NotFound
        )
        Spacer(Modifier.height(4.dp))
        StatusRow(
            label = Strings.LiveClient,
            active = status.liveClientActive,
            detail = if (status.liveClientActive) {
                status.lastSnapshotTime?.let {
                    val ago = (System.currentTimeMillis() - it) / 1000
                    Strings.lastSnapshot(ago)
                } ?: Strings.Connected
            } else Strings.NotActive
        )
    }
}

@Composable
fun StatusRow(label: String, active: Boolean, detail: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) AccentGreen else AccentRed)
        )
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(detail, color = TextSecondary, fontSize = 11.sp)
    }
}

// ─── Game State Panel ─────────────────────────────────────────

@Composable
fun GameStatePanel(state: GameState, gameMode: GameMode = GameMode.UNKNOWN, modifier: Modifier = Modifier) {
    val (stateName, stateColor, stateEmoji) = when (state) {
        is GameState.Idle -> Triple(Strings.Idle, AccentRed, "⏸️")
        is GameState.ChampSelect -> Triple(Strings.ChampSelect, AccentOrange, "🎮")
        is GameState.Loading -> Triple(Strings.Loading, Color(0xFF_FFC107), "⏳")
        is GameState.InGame -> Triple(Strings.InGame, AccentGreen, "⚔️")
        is GameState.PostGame -> Triple(Strings.PostGame, TextSecondary, "🏁")
    }

    val modeColor = when (gameMode) {
        GameMode.SUMMONERS_RIFT -> AccentBlue
        GameMode.ARAM -> AccentOrange
        GameMode.ARAM_MAYHEM -> Color(0xFF_FF4081)
        GameMode.UNKNOWN -> TextSecondary
    }

    DashboardCard(modifier = modifier) {
        SectionHeader("Game State", "🎯")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stateEmoji, fontSize = 18.sp)
            Text(
                text = stateName,
                color = stateColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("🗺️", fontSize = 12.sp)
            Text(
                text = gameMode.displayName,
                color = modeColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Game Info Panel ──────────────────────────────────────────

@Composable
fun GameInfoPanel(snapshot: GameSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader("GAME INFO", "📊")

        if (snapshot == null || snapshot.activePlayer == null) {
            Text("No game data available", color = TextSecondary, fontSize = 12.sp)
        } else {
            val ap = snapshot.activePlayer!!
            val gd = snapshot.gameData

            InfoRow("Summoner", ap.summonerName.ifEmpty { ap.riotId })
            InfoRow("Level", "${ap.level}")
            InfoRow("Gold", "%.0f".format(ap.currentGold))

            if (gd != null) {
                val minutes = (gd.gameTime / 60).toInt()
                val seconds = (gd.gameTime % 60).toInt()
                InfoRow("Time", "%d:%02d".format(minutes, seconds))
                InfoRow("Mode", gd.gameMode)
                InfoRow("Map", gd.mapName.ifEmpty { "Map ${gd.mapNumber}" })
            }

            ap.championStats?.let { stats ->
                Spacer(Modifier.height(6.dp))
                Text("Stats", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("HP", "%.0f/%.0f".format(stats.currentHealth, stats.maxHealth))
                    StatChip("AD", "%.0f".format(stats.attackDamage))
                    StatChip("AP", "%.0f".format(stats.abilityPower))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("ARM", "%.0f".format(stats.armor))
                    StatChip("MR", "%.0f".format(stats.magicResist))
                    StatChip("MS", "%.0f".format(stats.moveSpeed))
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(value, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Players Panel ────────────────────────────────────────────

@Composable
fun PlayersPanel(snapshot: GameSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader("Players", "👥")

        if (snapshot == null || snapshot.allPlayers.isEmpty()) {
            Text("No players detected", color = TextSecondary, fontSize = 12.sp)
        } else {
            val allies = snapshot.allPlayers.filter { it.team == "ORDER" }
            val enemies = snapshot.allPlayers.filter { it.team == "CHAOS" }

            if (allies.isNotEmpty()) {
                Text("🔵 Allies", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                allies.forEach { player ->
                    PlayerRow(player.championName, player.position, player.scores, Color(0xFF_58A6FF))
                }
            }
            if (enemies.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("🔴 Enemies", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                enemies.forEach { player ->
                    PlayerRow(player.championName, player.position, player.scores, AccentRed)
                }
            }
        }
    }
}

@Composable
fun PlayerRow(
    champion: String,
    position: String,
    scores: com.lolcoach.bridge.model.liveclient.PlayerScores?,
    teamColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(4.dp).clip(CircleShape).background(teamColor)
        )
        Text(
            champion.ifEmpty { "?" },
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(90.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            position.ifEmpty { "—" },
            color = TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.width(55.dp)
        )
        if (scores != null) {
            Text(
                "${scores.kills}/${scores.deaths}/${scores.assists}",
                color = TextPrimary,
                fontSize = 10.sp
            )
            Spacer(Modifier.weight(1f))
            Text("CS ${scores.creepScore}", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

// ─── LLM Analysis Panel ───────────────────────────────────────

@Composable
fun LlmAnalysisPanel(
    analyses: List<GameEvent.LlmAnalysis>,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        SectionHeader("LLM Coach Analysis", "🧠")

        if (analyses.isEmpty()) {
            Text(
                "Waiting for champion select for LLM analysis...", color = TextSecondary, fontSize = 12.sp
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(analyses) { analysis ->
                    LlmAnalysisRow(analysis)
                }
            }
        }
    }
}

@Composable
fun LlmAnalysisRow(analysis: GameEvent.LlmAnalysis) {
    val (icon, color) = when (analysis.section) {
        "Comp Analysis" -> "📊" to AccentBlue
        "Win Condition" -> "🏆" to AccentGreen
        "What to Avoid" -> "⚠️" to AccentRed
        "Priority" -> "🎯" to AccentOrange
        "ERROR" -> "❌" to AccentRed
        else -> "🧠" to Color(0xFF_7C4DFF)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            "$icon ${analysis.section}",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            analysis.content,
            color = TextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

// ─── Events Panel ─────────────────────────────────────────────

@Composable
fun EventsPanel(
    events: List<DashboardViewModel.TimestampedGameEvent>,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        SectionHeader("Strategy Events", "🧠")

        if (events.isEmpty()) {
            Text("No events generated", color = TextSecondary, fontSize = 12.sp)
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(events) { entry ->
                    EventRow(entry)
                }
            }
        }
    }
}

@Composable
fun EventRow(entry: DashboardViewModel.TimestampedGameEvent) {
    val timeStr = SimpleDateFormat("HH:mm:ss").format(Date(entry.timestamp))
    val (icon, color) = when (entry.event) {
        is GameEvent.Level2Approaching -> "⚔️" to Color(0xFF_FF8C00)
        is GameEvent.Level2Reached -> "🎯" to AccentGreen
        is GameEvent.EnemySupportSelected -> "🛡️" to AccentRed
        is GameEvent.VisionNeeded -> "👁️" to AccentBlue
        is GameEvent.DragonTimerWarning -> "🐉" to Color(0xFF_AA00FF)
        is GameEvent.ItemSuggestion -> "🛒" to AccentOrange
        is GameEvent.SynergyAdvice -> "🤝" to Color(0xFF_00BFA5)
        is GameEvent.GenericTip -> "💡" to TextSecondary
        is GameEvent.AramHealthPackReminder -> "💊" to AccentGreen
        is GameEvent.AramTeamfightTip -> "⚔️" to AccentOrange
        is GameEvent.AramPokeWarning -> "🎯" to AccentRed
        is GameEvent.AramSnowballAdvice -> "❄️" to AccentBlue
        is GameEvent.LlmAnalysis -> "🧠" to Color(0xFF_7C4DFF)
        is GameEvent.UserVoiceQuery -> "🎤" to AccentGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(timeStr, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(icon, fontSize = 12.sp)
        Text(
            entry.event.message,
            color = TextPrimary,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Log Panel ────────────────────────────────────────────────

@Composable
fun LogPanel(
    logs: List<LogEntry>,
    selectedLevel: LogLevel?,
    onFilterChanged: (LogLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionHeader("Logs", "📋")
            Spacer(Modifier.weight(1f))

            // Filter chips
            LogFilterChip("All", selected = selectedLevel == null) { onFilterChanged(null) }
            LogLevel.entries.forEach { level ->
                LogFilterChip(
                    level.emoji,
                    selected = selectedLevel == level
                ) { onFilterChanged(level) }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (logs.isEmpty()) {
            Text("Nessun log", color = TextSecondary, fontSize = 12.sp)
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(logs) { entry ->
                    LogRow(entry)
                }
            }
        }
    }
}

@Composable
fun LogFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AccentBlue.copy(alpha = 0.3f) else Color.Transparent
    val border = if (selected) AccentBlue else BorderColor

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = if (selected) AccentBlue else TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun LogRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> TextSecondary
        LogLevel.INFO -> AccentBlue
        LogLevel.WARN -> AccentOrange
        LogLevel.ERROR -> AccentRed
        LogLevel.EVENT -> AccentGreen
    }

    Text(
        text = entry.formatted(),
        color = levelColor,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

// ─── Voice Settings Panel ───────────────────────────────────

@Composable
fun VoiceSettingsPanel(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    availableDevices: List<VoiceDevice>,
    selectedDevice: VoiceDevice?,
    onDeviceSelected: (VoiceDevice) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        SectionHeader(Strings.VoiceCoaching, "🎙️")
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentGreen,
                    checkedTrackColor = AccentGreen.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Strings.EhyCoach,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) AccentGreen else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Strings.Microphone,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BackgroundSecondary)
                    .padding(8.dp)
            ) {
                Text(
                    text = selectedDevice?.name ?: Strings.NoDevices,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selectedDevice != null) TextPrimary else AccentRed
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = onRefresh) {
                Text("🔄", fontSize = 18.sp)
            }
        }
        
        if (availableDevices.isNotEmpty() && enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                availableDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = device == selectedDevice,
                            onClick = { onDeviceSelected(device) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentGreen)
                        )
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (device == selectedDevice) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ─── Card Container ───────────────────────────────────────────

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .background(CardBackground)
            .padding(12.dp),
        content = content
    )
}
