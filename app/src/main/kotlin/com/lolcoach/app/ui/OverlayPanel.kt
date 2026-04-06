package com.lolcoach.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.viewmodel.OverlayViewModel
import com.lolcoach.brain.event.GameEvent

@Composable
fun OverlayPanel(events: List<OverlayViewModel.TimedEvent>, overlayAlpha: Float = 0.85f) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .width(350.dp)
            .alpha(overlayAlpha),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.End
    ) {
        events.forEach { timedEvent ->
            EventCard(timedEvent.event)
        }
    }
}

@Composable
fun EventCard(event: GameEvent) {
    val (bgColor, icon) = when (event) {
        is GameEvent.Level2Approaching -> Color(0xAA_FF8C00) to "⚔️"
        is GameEvent.Level2Reached -> Color(0xAA_00C853) to "🎯"
        is GameEvent.EnemySupportSelected -> Color(0xAA_D50000) to "🛡️"
        is GameEvent.VisionNeeded -> Color(0xAA_2962FF) to "👁️"
        is GameEvent.DragonTimerWarning -> Color(0xAA_AA00FF) to "🐉"
        is GameEvent.ItemSuggestion -> Color(0xAA_FFD600) to "🛒"
        is GameEvent.SynergyAdvice -> Color(0xAA_00BFA5) to "🤝"
        is GameEvent.GenericTip -> Color(0xAA_546E7A) to "💡"
        is GameEvent.AramHealthPackReminder -> Color(0xAA_00C853) to "💊"
        is GameEvent.AramTeamfightTip -> Color(0xAA_FF8C00) to "⚔️"
        is GameEvent.AramPokeWarning -> Color(0xAA_D50000) to "🎯"
        is GameEvent.AramSnowballAdvice -> Color(0xAA_2962FF) to "❄️"
        is GameEvent.LlmAnalysis -> Color(0xAA_7C4DFF) to "🧠"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "$icon ${event.message}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
