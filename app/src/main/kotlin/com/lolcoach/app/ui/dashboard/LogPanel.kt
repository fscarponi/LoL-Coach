package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lolcoach.app.logging.LogEntry
import com.lolcoach.app.logging.LogLevel
import com.lolcoach.app.ui.theme.LolColors

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
