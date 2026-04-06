package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.lolcoach.app.i18n.Strings
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.app.viewmodel.ConnectionStatus
import com.lolcoach.brain.state.GameMode
import com.lolcoach.brain.state.GameState

@Composable
fun TopStatusBar(
    status: ConnectionStatus,
    state: GameState,
    gameMode: GameMode,
    isListening: Boolean,
    lastCoachMessage: String?,
    onSettingsClick: () -> Unit,
    onLogsClick: () -> Unit,
    activeSideTab: SideTab?
) {
    val (stateName, stateColor) = when (state) {
        is GameState.Idle -> Strings.Idle to LolColors.Muted
        is GameState.ChampSelect -> Strings.ChampSelect to LolColors.Gold
        is GameState.Loading -> Strings.Loading to LolColors.GoldLight
        is GameState.InGame -> Strings.InGame to LolColors.SuccessGreen
        is GameState.PostGame -> Strings.PostGame to LolColors.BlueLight
    }

    val modeColor = when (gameMode) {
        GameMode.SUMMONERS_RIFT -> LolColors.BlueLight
        GameMode.ARAM -> LolColors.Gold
        GameMode.ARAM_MAYHEM -> Color(0xFFFF4081)
        GameMode.UNKNOWN -> LolColors.Muted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LolColors.TopBarGradient)
            .border(width = 1.dp, color = LolColors.BorderGold)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App title
        Text(
            "LoL Coach",
            style = MaterialTheme.typography.titleSmall,
            color = LolColors.Gold,
            fontWeight = FontWeight.Bold
        )

        // Divider
        Box(Modifier.width(1.dp).height(20.dp).background(LolColors.Border))

        // Connection dots
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusDot(active = status.lockfileFound, label = "LCU")
            StatusDot(active = status.liveClientActive, label = "Live")
        }

        // Divider
        Box(Modifier.width(1.dp).height(20.dp).background(LolColors.Border))

        // Game state badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(stateColor.copy(alpha = 0.15f))
                .border(1.dp, stateColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(stateColor))
            Text(stateName, style = MaterialTheme.typography.labelSmall, color = stateColor, fontWeight = FontWeight.Bold)
        }

        // Game mode
        if (gameMode != GameMode.UNKNOWN) {
            Text(gameMode.displayName, style = MaterialTheme.typography.labelSmall, color = modeColor)
        }

        // Voice indicator
        if (isListening) {
            Text("🎙️ ${Strings.Listening}", style = MaterialTheme.typography.labelSmall, color = LolColors.BlueLight)
        }

        // Coach feedback in top bar
        if (lastCoachMessage != null) {
            Box(Modifier.width(1.dp).height(20.dp).background(LolColors.Border))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🧠", fontSize = 12.sp)
                Text(
                    lastCoachMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB388FF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        // Action buttons
        TopBarButton("⚙️", active = activeSideTab == SideTab.SETTINGS, onClick = onSettingsClick)
        TopBarButton("📋", active = activeSideTab == SideTab.LOGS, onClick = onLogsClick)
    }
}

@Composable
private fun StatusDot(active: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(6.dp).clip(CircleShape)
                .background(if (active) LolColors.SuccessGreen else LolColors.Danger)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = LolColors.OnSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun TopBarButton(icon: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) LolColors.Gold.copy(alpha = 0.2f) else Color.Transparent
    val border = if (active) LolColors.Gold.copy(alpha = 0.4f) else LolColors.Border
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 14.sp)
    }
}
