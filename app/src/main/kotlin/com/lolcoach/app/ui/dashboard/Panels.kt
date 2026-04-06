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
import com.lolcoach.app.logging.LogEntry
import com.lolcoach.app.logging.LogLevel
import com.lolcoach.app.viewmodel.ConnectionStatus
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.brain.event.GameEvent
import com.lolcoach.brain.state.GameState
import com.lolcoach.bridge.model.liveclient.GameSnapshot
import java.text.SimpleDateFormat
import java.util.Date

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
            text = title,
            color = AccentBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

// ─── Connection Panel ─────────────────────────────────────────

@Composable
fun ConnectionPanel(status: ConnectionStatus) {
    DashboardCard {
        SectionHeader("CONNESSIONE", "🔌")

        StatusRow(
            label = "Lockfile",
            active = status.lockfileFound,
            detail = if (status.lockfileFound) "Porta ${status.lockfilePort}" else "Non trovato"
        )
        Spacer(Modifier.height(4.dp))
        StatusRow(
            label = "Live Client",
            active = status.liveClientActive,
            detail = if (status.liveClientActive) {
                status.lastSnapshotTime?.let {
                    val ago = (System.currentTimeMillis() - it) / 1000
                    "Ultimo snapshot ${ago}s fa"
                } ?: "Connesso"
            } else "Non attivo"
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
fun GameStatePanel(state: GameState) {
    val (stateName, stateColor, stateEmoji) = when (state) {
        is GameState.Idle -> Triple("IDLE — Disconnesso", AccentRed, "⏸️")
        is GameState.ChampSelect -> Triple("CHAMPION SELECT", AccentOrange, "🎮")
        is GameState.Loading -> Triple("CARICAMENTO", Color(0xFF_FFC107), "⏳")
        is GameState.InGame -> Triple("IN PARTITA", AccentGreen, "⚔️")
        is GameState.PostGame -> Triple("FINE PARTITA", TextSecondary, "🏁")
    }

    DashboardCard {
        SectionHeader("STATO GIOCO", "🎯")
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
    }
}

// ─── Game Info Panel ──────────────────────────────────────────

@Composable
fun GameInfoPanel(snapshot: GameSnapshot?) {
    DashboardCard {
        SectionHeader("INFO PARTITA", "📊")

        if (snapshot == null || snapshot.activePlayer == null) {
            Text("Nessun dato di gioco disponibile", color = TextSecondary, fontSize = 12.sp)
        } else {
            val ap = snapshot.activePlayer!!
            val gd = snapshot.gameData

            InfoRow("Summoner", ap.summonerName.ifEmpty { ap.riotId })
            InfoRow("Livello", "${ap.level}")
            InfoRow("Gold", "%.0f".format(ap.currentGold))

            if (gd != null) {
                val minutes = (gd.gameTime / 60).toInt()
                val seconds = (gd.gameTime % 60).toInt()
                InfoRow("Tempo", "%d:%02d".format(minutes, seconds))
                InfoRow("Modalità", gd.gameMode)
                InfoRow("Mappa", gd.mapName.ifEmpty { "Mappa ${gd.mapNumber}" })
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
fun PlayersPanel(snapshot: GameSnapshot?) {
    DashboardCard {
        SectionHeader("GIOCATORI", "👥")

        if (snapshot == null || snapshot.allPlayers.isEmpty()) {
            Text("Nessun giocatore rilevato", color = TextSecondary, fontSize = 12.sp)
        } else {
            val allies = snapshot.allPlayers.filter { it.team == "ORDER" }
            val enemies = snapshot.allPlayers.filter { it.team == "CHAOS" }

            if (allies.isNotEmpty()) {
                Text("🔵 Alleati", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                allies.forEach { player ->
                    PlayerRow(player.championName, player.position, player.scores, Color(0xFF_58A6FF))
                }
            }
            if (enemies.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("🔴 Nemici", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        SectionHeader("ANALISI LLM COACH", "🧠")

        if (analyses.isEmpty()) {
            Text(
                "In attesa della champion select per l'analisi LLM...",
                color = TextSecondary,
                fontSize = 12.sp
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
        "Analisi Comp" -> "📊" to AccentBlue
        "Win Condition" -> "🏆" to AccentGreen
        "Cosa Evitare" -> "⚠️" to AccentRed
        "Priorità" -> "🎯" to AccentOrange
        "ERRORE" -> "❌" to AccentRed
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
        SectionHeader("EVENTI STRATEGIA", "🧠")

        if (events.isEmpty()) {
            Text("Nessun evento generato", color = TextSecondary, fontSize = 12.sp)
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
        is GameEvent.LlmAnalysis -> "🧠" to Color(0xFF_7C4DFF)
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
            SectionHeader("LOG", "📋")
            Spacer(Modifier.weight(1f))

            // Filter chips
            LogFilterChip("Tutti", selected = selectedLevel == null) { onFilterChanged(null) }
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
