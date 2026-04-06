package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.model.liveclient.GameSnapshot
import com.lolcoach.model.liveclient.PlayerScores

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
    scores: PlayerScores?,
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
