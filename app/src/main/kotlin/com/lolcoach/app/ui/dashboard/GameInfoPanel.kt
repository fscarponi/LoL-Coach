package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.model.liveclient.GameSnapshot

// ─── Game Info Panel ──────────────────────────────────────────

@Composable
fun GameInfoPanel(snapshot: GameSnapshot?, modifier: Modifier = Modifier) {
    DashboardCard(modifier = modifier) {
        SectionHeader("Game Info", "📊")

        if (snapshot == null || snapshot.activePlayer == null) {
            EmptyState("No game data available")
        } else {
            val ap = snapshot.activePlayer!!
            val gd = snapshot.gameData

            // Champion + Level + Time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Summoner info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ap.summonerName.ifEmpty { ap.riotId },
                        style = MaterialTheme.typography.titleSmall,
                        color = LolColors.GoldLight,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBadge("Lvl ${ap.level}", LolColors.BlueLight)
                        StatBadge("%.0f Gold".format(ap.currentGold), LolColors.Gold)
                        if (gd != null) {
                            val minutes = (gd.gameTime / 60).toInt()
                            val seconds = (gd.gameTime % 60).toInt()
                            StatBadge("%d:%02d".format(minutes, seconds), LolColors.OnSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                // EXP bar
                if (ap.neededExp > 0) {
                    Column(modifier = Modifier.width(120.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            "EXP %.0f / %.0f".format(ap.currentExp, ap.neededExp),
                            style = MaterialTheme.typography.labelSmall,
                            color = LolColors.OnSurface.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { (ap.currentExp / ap.neededExp).toFloat() },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = LolColors.BlueLight,
                            trackColor = LolColors.BlueMedium
                        )
                    }
                }
            }

            // Stats grid
            ap.championStats?.let { stats ->
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = LolColors.Border.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip("HP", "%.0f".format(stats.maxHealth), LolColors.SuccessGreen)
                    StatChip("AD", "%.0f".format(stats.attackDamage), LolColors.Danger)
                    StatChip("AP", "%.0f".format(stats.abilityPower), LolColors.BlueLight)
                    StatChip("ARM", "%.0f".format(stats.armor), LolColors.Gold)
                    StatChip("MR", "%.0f".format(stats.magicResist), Color(0xFF_AA66CC))
                    StatChip("MS", "%.0f".format(stats.moveSpeed), LolColors.OnSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun StatBadge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun StatChip(label: String, value: String, color: Color = LolColors.GoldLight) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
    }
}
