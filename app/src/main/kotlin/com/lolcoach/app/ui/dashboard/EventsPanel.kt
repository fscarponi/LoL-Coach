package com.lolcoach.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.app.viewmodel.DashboardViewModel
import com.lolcoach.brain.event.GameEvent
import java.text.SimpleDateFormat
import java.util.*

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
