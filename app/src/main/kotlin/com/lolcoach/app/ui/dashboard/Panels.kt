package com.lolcoach.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.brain.event.GameEvent
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import com.lolcoach.bridge.voice.DownloadState
import com.lolcoach.bridge.voice.VoiceDevice
import java.text.SimpleDateFormat
import java.util.*

// ─── Shared Components ────────────────────────────────────────

@Composable
fun SectionHeader(title: String, emoji: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(
            text = title.uppercase(),
            color = LolColors.Gold,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(LolColors.CardGradient)
            .border(1.dp, LolColors.Border.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(12.dp),
        content = content
    )
}

// ─── Game Info Panel ──────────────────────────────────────────

@Composable
fun GameInfoPanel(snapshot: GameSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader("Game Info", "📊")

        if (snapshot == null || snapshot.activePlayer == null) {
            EmptyState("No game data available")
        } else {
            val ap = snapshot.activePlayer!!
            val gd = snapshot.gameData

            // Champion + Level + Time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Summoner info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ap.summonerName.ifEmpty { ap.riotId },
                        style = MaterialTheme.typography.titleSmall,
                        color = LolColors.GoldLight,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBadge("Lvl ${ap.level}", LolColors.BlueLight)
                        StatBadge("%.0f Gold".format(ap.currentGold), LolColors.Gold)
                        if (gd != null) {
                            val minutes = (gd.gameTime / 60).toInt()
                            val seconds = (gd.gameTime % 60).toInt()
                            StatBadge("%d:%02d".format(minutes, seconds), LolColors.OnSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                // EXP bar
                if (ap.neededExp > 0) {
                    Column(modifier = Modifier.width(120.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            "EXP %.0f / %.0f".format(ap.currentExp, ap.neededExp),
                            style = MaterialTheme.typography.labelSmall,
                            color = LolColors.OnSurface.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { (ap.currentExp / ap.neededExp).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = LolColors.BlueLight,
                            trackColor = LolColors.BlueMedium
                        )
                    }
                }
            }

            // Stats grid
            ap.championStats?.let { stats ->
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = LolColors.Border.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip("HP", "%.0f".format(stats.maxHealth), LolColors.SuccessGreen)
                    StatChip("AD", "%.0f".format(stats.attackDamage), LolColors.Danger)
                    StatChip("AP", "%.0f".format(stats.abilityPower), LolColors.BlueLight)
                    StatChip("ARM", "%.0f".format(stats.armor), LolColors.Gold)
                    StatChip("MR", "%.0f".format(stats.magicResist), Color(0xFF_AA66CC))
                    StatChip("MS", "%.0f".format(stats.moveSpeed), LolColors.OnSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun StatBadge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun StatChip(label: String, value: String, color: Color = LolColors.GoldLight) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = LolColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

// ─── Players Panel ────────────────────────────────────────────

@Composable
fun PlayersPanel(snapshot: GameSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader("Players", "👥")

        if (snapshot == null || snapshot.allPlayers.isEmpty()) {
            EmptyState("No players detected")
        } else {
            val allies = snapshot.allPlayers.filter { it.team == "ORDER" }
            val enemies = snapshot.allPlayers.filter { it.team == "CHAOS" }

            if (allies.isNotEmpty()) {
                TeamLabel("ALLIES", LolColors.BlueLight, "🔵")
                allies.forEach { player ->
                    PlayerRow(player.championName, player.position, player.scores, LolColors.BlueLight, player.level)
                }
            }
            if (enemies.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TeamLabel("ENEMIES", LolColors.DangerMuted, "🔴")
                enemies.forEach { player ->
                    PlayerRow(player.championName, player.position, player.scores, LolColors.DangerMuted, player.level)
                }
            }
        }
    }
}

@Composable
private fun TeamLabel(text: String, color: Color, icon: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(icon, fontSize = 10.sp)
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun PlayerRow(
    champion: String,
    position: String,
    scores: com.lolcoach.bridge.model.liveclient.PlayerScores?,
    teamColor: Color,
    level: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(teamColor.copy(alpha = 0.05f))
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Team color indicator
        Box(Modifier.size(3.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(teamColor))

        // Champion name + position
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    champion.ifEmpty { "?" },
                    color = LolColors.GoldLight,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Lv$level",
                    color = LolColors.Muted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (position.isNotEmpty()) {
                Text(
                    position.lowercase(),
                    color = LolColors.Muted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // KDA + CS
        if (scores != null) {
            Text(
                "${scores.kills}/${scores.deaths}/${scores.assists}",
                color = LolColors.Gold,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${scores.creepScore} CS",
                color = LolColors.Muted,
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
        SectionHeader("Coach Analysis", "🧠")

        if (analyses.isEmpty()) {
            EmptyState("Waiting for champion select for analysis...")
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(analyses, key = { it.section }) { analysis ->
                    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
                        LlmAnalysisRow(analysis)
                    }
                }
            }
        }
    }
}

@Composable
fun LlmAnalysisRow(analysis: GameEvent.LlmAnalysis) {
    val (color, icon) = when (analysis.section) {
        "Comp Analysis" -> LolColors.BlueLight to "📊"
        "Win Condition" -> LolColors.SuccessGreen to "🏆"
        "What to Avoid" -> LolColors.DangerMuted to "⚠️"
        "Priority" -> LolColors.Gold to "🎯"
        "ERROR" -> LolColors.Danger to "❌"
        else -> LolColors.GoldLight to "💡"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.06f))
            .border(1.dp, color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon, fontSize = 12.sp)
            Text(
                analysis.section.uppercase(),
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            analysis.content,
            color = LolColors.OnSurface.copy(alpha = 0.85f),
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
        SectionHeader("Strategy Events", "⚡")

        if (events.isEmpty()) {
            EmptyState("No events generated")
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(events, key = { it.timestamp }) { entry ->
                    AnimatedVisibility(visible = true, enter = slideInHorizontally { -30 } + fadeIn()) {
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
        is GameEvent.Level2Reached -> "🎯" to LolColors.SuccessGreen
        is GameEvent.EnemySupportSelected -> "🛡️" to LolColors.DangerMuted
        is GameEvent.VisionNeeded -> "👁️" to LolColors.BlueLight
        is GameEvent.DragonTimerWarning -> "🐉" to Color(0xFFAA00FF)
        is GameEvent.ItemSuggestion -> "🛒" to LolColors.Gold
        is GameEvent.SynergyAdvice -> "🤝" to Color(0xFF00BFA5)
        is GameEvent.GenericTip -> "💡" to LolColors.Muted
        is GameEvent.AramHealthPackReminder -> "💊" to LolColors.SuccessGreen
        is GameEvent.AramTeamfightTip -> "⚔️" to LolColors.Gold
        is GameEvent.AramPokeWarning -> "🎯" to LolColors.Danger
        is GameEvent.AramSnowballAdvice -> "❄️" to LolColors.BlueLight
        is GameEvent.LlmAnalysis -> "🧠" to Color(0xFF7C4DFF)
        is GameEvent.UserVoiceQuery -> "🎤" to LolColors.BlueLight
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 14.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.event.message,
                color = LolColors.OnSurface.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            timeStr,
            color = LolColors.Muted,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace
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
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionHeader("Logs", "📋")
            Spacer(Modifier.weight(1f))
            LogFilterChip("All", selected = selectedLevel == null) { onFilterChanged(null) }
            LogLevel.entries.forEach { level ->
                LogFilterChip(level.emoji, selected = selectedLevel == level) { onFilterChanged(level) }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (logs.isEmpty()) {
            EmptyState("No logs yet")
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
    val bg = if (selected) LolColors.Gold.copy(alpha = 0.15f) else Color.Transparent
    val border = if (selected) LolColors.Gold.copy(alpha = 0.4f) else LolColors.Border.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, border, RoundedCornerShape(3.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            label,
            color = if (selected) LolColors.GoldLight else LolColors.Muted,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun LogRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> LolColors.Muted
        LogLevel.INFO -> LolColors.BlueLight
        LogLevel.WARN -> LolColors.Gold
        LogLevel.ERROR -> LolColors.Danger
        LogLevel.EVENT -> LolColors.SuccessGreen
    }

    Text(
        entry.formatted(),
        color = levelColor,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp)
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
        // Enable toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(Strings.VoiceCoaching, "🎙️")
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LolColors.Gold,
                    checkedTrackColor = LolColors.GoldDark,
                    uncheckedThumbColor = LolColors.Muted,
                    uncheckedTrackColor = LolColors.BlueMedium
                )
            )
        }

        // Status line
        val statusText = if (isListening) "🎙️ ${Strings.Listening}" else if (enabled) "✅ Active" else "⏸️ Disabled"
        val statusColor = if (isListening) LolColors.BlueLight else if (enabled) LolColors.SuccessGreen else LolColors.Muted
        Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)

        Spacer(Modifier.height(10.dp))

        // Model download
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vosk Model", style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
            DownloadStatusBadge(downloadState, onDownload)
        }

        Spacer(Modifier.height(10.dp))

        // Wake word
        Text("Wake Word", style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(LolColors.BlueMedium)
                .border(1.dp, LolColors.Border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
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
                Text("e.g. Hey Coach", color = LolColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Microphone selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Strings.Microphone, style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
            Text("🔄", fontSize = 12.sp, modifier = Modifier.clickable { onRefresh() })
        }
        Spacer(Modifier.height(4.dp))

        // Selected device display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(LolColors.BlueMedium)
                .border(1.dp, LolColors.Border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(
                selectedDevice?.name ?: Strings.NoDevices,
                style = MaterialTheme.typography.bodySmall,
                color = if (selectedDevice != null) LolColors.OnSurface else LolColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Device list
        if (availableDevices.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, LolColors.Border.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                availableDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .clickable { onDeviceSelected(device) }
                            .background(
                                if (device.name == selectedDevice?.name) LolColors.Gold.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(
                                    if (device.name == selectedDevice?.name) LolColors.Gold
                                    else LolColors.Border
                                )
                        )
                        Text(
                            device.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.name == selectedDevice?.name) LolColors.GoldLight else LolColors.OnSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Quick guide (collapsed)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = LolColors.Border.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        Text("💡 Quick Guide", style = MaterialTheme.typography.labelSmall, color = LolColors.Gold)
        Spacer(Modifier.height(4.dp))
        val instructions = listOf(
            "Enable voice coaching → download model → select mic",
            "Say your wake word, then ask your question",
            "The coach will respond via TTS with game-aware advice"
        )
        instructions.forEach { text ->
            Text(
                "• $text",
                style = MaterialTheme.typography.labelSmall,
                color = LolColors.Muted,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
fun DownloadStatusBadge(state: DownloadState, onDownload: () -> Unit) {
    when (state) {
        is DownloadState.Idle -> {
            Text(
                Strings.DownloadModel,
                style = MaterialTheme.typography.labelSmall,
                color = LolColors.BlueLight,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { onDownload() }
                    .background(LolColors.BlueLight.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        is DownloadState.Downloading -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = LolColors.Gold)
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.width(60.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = LolColors.Gold,
                    trackColor = LolColors.Border
                )
            }
        }
        is DownloadState.Extracting -> {
            Text("⏳ Extracting...", style = MaterialTheme.typography.labelSmall, color = LolColors.BlueLight)
        }
        is DownloadState.Completed -> {
            Text("✅ Ready", style = MaterialTheme.typography.labelSmall, color = LolColors.SuccessGreen)
        }
        is DownloadState.Error -> {
            Text(
                "⚠️ Error — Retry",
                style = MaterialTheme.typography.labelSmall,
                color = LolColors.Danger,
                modifier = Modifier.clickable { onDownload() }
            )
        }
    }
}
