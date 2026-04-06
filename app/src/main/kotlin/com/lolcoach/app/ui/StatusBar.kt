package com.lolcoach.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lolcoach.app.i18n.Strings
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState

@Composable
fun StatusBar(state: GameState, gameMode: GameMode = GameMode.UNKNOWN, barAlpha: Float = 0.9f) {
    val (statusText, statusColor) = when (state) {
        is GameState.Idle -> Strings.DisconnectedStatus to LolColors.Danger
        is GameState.ChampSelect -> Strings.ChampSelect to LolColors.Gold
        is GameState.Loading -> Strings.LoadingStatus to LolColors.GoldLight
        is GameState.InGame -> Strings.InGameStatus to LolColors.Success
        is GameState.PostGame -> Strings.PostGameStatus to LolColors.OnSurface.copy(alpha = 0.5f)
    }

    val modeLabel = if (gameMode != GameMode.UNKNOWN) " · ${gameMode.displayName}" else ""

    Row(
        modifier = Modifier
            .alpha(barAlpha)
            .clip(RoundedCornerShape(6.dp))
            .background(LolColors.BlueDeep.copy(alpha = 0.8f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Text(
            text = "LoL Coach · $statusText$modeLabel",
            color = LolColors.GoldLight,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
