package com.lolcoach.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.app.viewmodel.OverlayViewModel
import com.lolcoach.brain.event.GameEvent

@Composable
fun OverlayPanel(events: List<OverlayViewModel.TimedEvent>, overlayAlpha: Float = 0.95f) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .width(360.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        events.forEach { timedEvent ->
            EventCard(timedEvent.event)
        }
    }
}

@Composable
fun EventCard(event: GameEvent) {
    val (color, icon) = when (event) {
        is GameEvent.Level2Approaching -> LolColors.Danger to "⚔️"
        is GameEvent.Level2Reached -> LolColors.Success to "🎯"
        is GameEvent.EnemySupportSelected -> LolColors.Danger to "🛡️"
        is GameEvent.VisionNeeded -> LolColors.BlueLight to "👁️"
        is GameEvent.DragonTimerWarning -> Color(0xFF_AA00FF) to "🐉"
        is GameEvent.ItemSuggestion -> LolColors.Gold to "🛒"
        is GameEvent.SynergyAdvice -> Color(0xFF_00BFA5) to "🤝"
        is GameEvent.GenericTip -> LolColors.OnSurface.copy(alpha = 0.5f) to "💡"
        is GameEvent.AramHealthPackReminder -> LolColors.Success to "💊"
        is GameEvent.AramTeamfightTip -> LolColors.Gold to "⚔️"
        is GameEvent.AramPokeWarning -> LolColors.Danger to "🎯"
        is GameEvent.AramSnowballAdvice -> LolColors.BlueLight to "❄️"
        is GameEvent.LlmAnalysis -> Color(0xFF_7C4DFF) to "🧠"
        is GameEvent.UserVoiceQuery -> LolColors.Success to "🎤"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.1f),
                        color.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = event.message,
                color = LolColors.GoldLight,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
