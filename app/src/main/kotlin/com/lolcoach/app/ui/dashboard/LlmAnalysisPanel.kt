package com.lolcoach.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.brain.event.GameEvent

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
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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
