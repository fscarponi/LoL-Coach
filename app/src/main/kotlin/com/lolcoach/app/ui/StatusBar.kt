package com.lolcoach.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.lolcoach.brain.state.GameState

@Composable
fun StatusBar(state: GameState, barAlpha: Float = 0.85f) {
    val (statusText, statusColor) = when (state) {
        is GameState.Idle -> "Disconnesso" to Color(0xFF_F44336)
        is GameState.ChampSelect -> "Champion Select" to Color(0xFF_FF9800)
        is GameState.Loading -> "Caricamento..." to Color(0xFF_FFC107)
        is GameState.InGame -> "In Partita" to Color(0xFF_4CAF50)
        is GameState.PostGame -> "Fine Partita" to Color(0xFF_9E9E9E)
    }

    Row(
        modifier = Modifier
            .alpha(barAlpha)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xAA_1A1A2E))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Text(
            text = "LoL Coach · $statusText",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
