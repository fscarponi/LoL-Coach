package com.lolcoach.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.i18n.Strings
import com.lolcoach.app.logging.LogEntry
import com.lolcoach.app.logging.LogLevel
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.app.viewmodel.ConnectionStatus
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import com.lolcoach.bridge.voice.DownloadState
import com.lolcoach.bridge.voice.VoiceDevice
import java.text.SimpleDateFormat
import java.util.*

// ─── Section Header ───────────────────────────────────────────

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
            color = LolColors.BlueLight,
            style = MaterialTheme.typography.labelLarge,
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
                .background(if (active) LolColors.Success else LolColors.Danger)
        )
        Text(label, color = LolColors.OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(detail, color = LolColors.OnSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
    }
}

// ─── Game State Panel ─────────────────────────────────────────

@Composable
fun GameStatePanel(state: GameState, gameMode: GameMode = GameMode.UNKNOWN, modifier: Modifier = Modifier) {
    val (stateName, stateColor, stateEmoji) = when (state) {
        is GameState.Idle -> Triple(Strings.Idle, LolColors.Danger, "⏸️")
        is GameState.ChampSelect -> Triple(Strings.ChampSelect, LolColors.Gold, "🎮")
        is GameState.Loading -> Triple(Strings.Loading, LolColors.GoldLight, "⏳")
        is GameState.InGame -> Triple(Strings.InGame, LolColors.Success, "⚔️")
        is GameState.PostGame -> Triple(Strings.PostGame, LolColors.OnSurface.copy(alpha = 0.5f), "🏁")
    }

    val modeColor = when (gameMode) {
        GameMode.SUMMONERS_RIFT -> LolColors.BlueLight
        GameMode.ARAM -> LolColors.Gold
        GameMode.ARAM_MAYHEM -> Color(0xFF_FF4081)
        GameMode.UNKNOWN -> LolColors.OnSurface.copy(alpha = 0.5f)
    }

    DashboardCard(modifier = modifier) {
        SectionHeader("Game State", "🎯")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stateEmoji, fontSize = 24.sp)
            Column {
                Text(
                    text = stateName,
                    color = stateColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗺️", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = gameMode.displayName,
                        color = modeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Game Info Panel ──────────────────────────────────────────

@Composable
fun GameInfoPanel(snapshot: GameSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader("GAME INFO", "📊")

        if (snapshot == null || snapshot.activePlayer == null) {
            Text(
                text = "No game data available",
                color = LolColors.OnSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
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
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "BASE STATS",
                    color = LolColors.BlueLight,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatChip("HP", "%.0f".format(stats.maxHealth))
                    StatChip("AD", "%.0f".format(stats.attackDamage))
                    StatChip("AP", "%.0f".format(stats.abilityPower))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = LolColors.OnSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = LolColors.OnSurface, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Surface(
        color = LolColors.BlueMedium,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            "$label: $value",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = LolColors.GoldLight
        )
    }
}

// ─── Players Panel ────────────────────────────────────────────

@Composable
fun PlayersPanel(snapshot: GameSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader("Players", "👥")

        if (snapshot == null || snapshot.allPlayers.isEmpty()) {
            Text(
                "No players detected",
                color = LolColors.OnSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            val allies = snapshot.allPlayers.filter { it.team == "ORDER" }
            val enemies = snapshot.allPlayers.filter { it.team == "CHAOS" }

            if (allies.isNotEmpty()) {
                Text(
                    "🔵 ALLIES",
                    color = LolColors.BlueLight,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                allies.forEach { player ->
                    PlayerRow(player.championName, player.position, player.scores, Color(0xFF_58A6FF))
                }
            }
            if (enemies.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "🔴 ENEMIES",
                    color = LolColors.Danger,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                enemies.forEach { player ->
                    PlayerRow(player.championName, player.position, player.scores, LolColors.Danger)
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(teamColor.copy(alpha = 0.05f))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(teamColor)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                champion.ifEmpty { "?" },
                color = LolColors.GoldLight,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                position.ifEmpty { "—" }.lowercase(),
                color = LolColors.OnSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (scores != null) {
            Text(
                "${scores.kills}/${scores.deaths}/${scores.assists}",
                color = LolColors.Gold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "CS ${scores.creepScore}",
                color = LolColors.OnSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
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
                text = "Waiting for champion select for LLM analysis...",
                color = LolColors.OnSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(analyses, key = { it.section }) { analysis ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        LlmAnalysisRow(analysis)
                    }
                }
            }
        }
    }
}

@Composable
fun LlmAnalysisRow(analysis: GameEvent.LlmAnalysis) {
    val color = when (analysis.section) {
        "Comp Analysis" -> LolColors.BlueLight
        "Win Condition" -> LolColors.Success
        "What to Avoid" -> LolColors.Danger
        "Priority" -> LolColors.Gold
        "ERROR" -> LolColors.Danger
        else -> LolColors.GoldLight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Text(
            text = analysis.section.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = analysis.content,
            color = LolColors.OnSurface,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 18.sp
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
            Text(
                text = "No events generated",
                color = LolColors.OnSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(events, key = { it.timestamp }) { entry ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally { -40 } + fadeIn()
                    ) {
                        EventRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun EventRow(entry: DashboardViewModel.TimestampedGameEvent) {
    val timeStr = remember(entry.timestamp) { SimpleDateFormat("HH:mm:ss").format(Date(entry.timestamp)) }
    val (icon, color) = when (entry.event) {
        is GameEvent.Level2Approaching -> "⚔️" to LolColors.Danger
        is GameEvent.Level2Reached -> "🎯" to LolColors.Success
        is GameEvent.EnemySupportSelected -> "🛡️" to LolColors.Danger
        is GameEvent.VisionNeeded -> "👁️" to LolColors.BlueLight
        is GameEvent.DragonTimerWarning -> "🐉" to Color(0xFF_AA00FF)
        is GameEvent.ItemSuggestion -> "🛒" to LolColors.Gold
        is GameEvent.SynergyAdvice -> "🤝" to Color(0xFF_00BFA5)
        is GameEvent.GenericTip -> "💡" to LolColors.OnSurface.copy(alpha = 0.5f)
        is GameEvent.AramHealthPackReminder -> "💊" to LolColors.Success
        is GameEvent.AramTeamfightTip -> "⚔️" to LolColors.Gold
        is GameEvent.AramPokeWarning -> "🎯" to LolColors.Danger
        is GameEvent.AramSnowballAdvice -> "❄️" to LolColors.BlueLight
        is GameEvent.LlmAnalysis -> "🧠" to Color(0xFF_7C4DFF)
        is GameEvent.UserVoiceQuery -> "🎤" to LolColors.Success
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.event.message,
                color = LolColors.GoldLight,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timeStr,
                color = LolColors.OnSurface.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace
            )
        }
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
            Text(
                text = "No logs yet",
                color = LolColors.OnSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall
            )
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(logs) { entry ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn()
                    ) {
                        LogRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun LogFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) LolColors.Gold.copy(alpha = 0.2f) else Color.Transparent
    val border = if (selected) LolColors.Gold else LolColors.Border

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) LolColors.GoldLight else LolColors.OnSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LogRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> LolColors.OnSurface.copy(alpha = 0.4f)
        LogLevel.INFO -> LolColors.BlueLight
        LogLevel.WARN -> LolColors.Gold
        LogLevel.ERROR -> LolColors.Danger
        LogLevel.EVENT -> LolColors.Success
    }

    Text(
        text = entry.formatted(),
        color = levelColor,
        style = MaterialTheme.typography.labelSmall,
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
    downloadState: DownloadState,
    onDownload: () -> Unit,
    wakeWord: String,
    onWakeWordChanged: (String) -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(Strings.VoiceCoaching.uppercase(), "🎙️")
            DownloadStatusBadge(downloadState, onDownload)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LolColors.Gold,
                    checkedTrackColor = LolColors.Gold.copy(alpha = 0.5f),
                    uncheckedThumbColor = LolColors.OnSurface.copy(alpha = 0.3f),
                    uncheckedTrackColor = LolColors.BlueDeep
                )
            )
            Column {
                Text(
                    text = if (isListening) Strings.Listening else if (enabled) Strings.InGameStatus else Strings.DisconnectedStatus,
                    color = if (isListening) LolColors.BlueLight else if (enabled) LolColors.Success else LolColors.OnSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Wake Word Config
        Text(
            text = "Wake Word",
            color = LolColors.Gold.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(LolColors.BlueMedium)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = wakeWord,
                onValueChange = onWakeWordChanged,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = LolColors.GoldLight),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(LolColors.Gold),
                singleLine = true
            )
            if (wakeWord.isEmpty()) {
                Text(
                    text = "e.g. Hey Coach",
                    color = LolColors.OnSurface.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Guide / Instruction Section
        VoiceGuideSection()

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.Microphone.uppercase(),
                color = LolColors.Gold.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                Text("🔄", fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LolColors.BlueMedium)
                    .padding(8.dp)
            ) {
                Text(
                    text = selectedDevice?.name ?: Strings.NoDevices,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedDevice != null) LolColors.OnSurface else LolColors.Danger,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (availableDevices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .border(1.dp, LolColors.Border, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                availableDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = device.name == selectedDevice?.name,
                            onClick = { onDeviceSelected(device) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = LolColors.Gold,
                                unselectedColor = LolColors.Border
                            )
                        )
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.name == selectedDevice?.name) LolColors.GoldLight else LolColors.OnSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadStatusBadge(state: DownloadState, onDownload: () -> Unit) {
    when (state) {
        is DownloadState.Idle -> {
            Button(
                onClick = onDownload,
                colors = ButtonDefaults.buttonColors(containerColor = LolColors.BlueMedium),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(Strings.DownloadModel, style = MaterialTheme.typography.labelSmall, color = LolColors.GoldLight)
            }
        }
        is DownloadState.Downloading -> {
            Column(horizontalAlignment = Alignment.End) {
                Text(Strings.ModelDownloading, style = MaterialTheme.typography.labelSmall, color = LolColors.OnSurface.copy(alpha = 0.5f))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.width(80.dp).height(2.dp).clip(RoundedCornerShape(1.dp)),
                    color = LolColors.Gold,
                    trackColor = LolColors.Border
                )
            }
        }
        is DownloadState.Extracting -> {
            Text(Strings.ModelExtracting, style = MaterialTheme.typography.labelSmall, color = LolColors.BlueLight)
        }
        is DownloadState.Completed -> {
            Text("✨ ${Strings.ModelReady}", style = MaterialTheme.typography.labelSmall, color = LolColors.Success)
        }
        is DownloadState.Error -> {
            Text("⚠️ ${Strings.ModelError}", style = MaterialTheme.typography.labelSmall, color = LolColors.Danger, modifier = Modifier.clickable { onDownload() })
        }
    }
}

@Composable
fun VoiceGuideSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(LolColors.BlueDeep.copy(alpha = 0.4f))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚡", fontSize = 12.sp, color = LolColors.Gold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = Strings.VoiceSetupGuide.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = LolColors.GoldLight
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val instructions = listOf(
            Strings.VoiceSetupInstruction1,
            Strings.VoiceSetupInstruction2,
            Strings.VoiceSetupInstruction3,
            Strings.VoiceSetupInstruction4
        )
        instructions.forEach { instruction ->
            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                Text("•", color = LolColors.Gold, modifier = Modifier.padding(end = 4.dp), fontSize = 10.sp)
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.labelSmall,
                    color = LolColors.OnSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
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
            .background(LolColors.Surface)
            .border(
                width = 1.dp,
                brush = SolidColor(LolColors.Border),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(14.dp),
        content = content
    )
}
