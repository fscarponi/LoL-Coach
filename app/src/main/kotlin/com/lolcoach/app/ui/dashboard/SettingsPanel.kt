package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.ui.theme.LolColors

// ─── Settings Panel ───────────────────────────────────────────

data class AppSettings(
    val overlayEnabled: Boolean = true,
    val ttsEnabled: Boolean = true,
    val llmEnabled: Boolean = true,
    val llmBaseUrl: String = "http://localhost:11434/v1",
    val llmModel: String = "",
    val llmTemperature: Double = 0.7,
    val pollingIntervalMs: Long = 1000
)

@Composable
fun SettingsPanel(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── General ──
        DashboardCard {
            SectionHeader("General", "🎮")

            SettingsToggleRow(
                label = "Overlay Window",
                description = "Show compact overlay on top of game",
                checked = settings.overlayEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(overlayEnabled = it)) }
            )

            Spacer(Modifier.height(6.dp))

            SettingsToggleRow(
                label = "TTS Notifications",
                description = "Read strategy events aloud",
                checked = settings.ttsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(ttsEnabled = it)) }
            )
        }

        // ── LLM Configuration ──
        DashboardCard {
            SectionHeader("LLM Coach", "🧠")

            SettingsToggleRow(
                label = "Enable LLM Analysis",
                description = "Use AI for champion select analysis",
                checked = settings.llmEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(llmEnabled = it)) }
            )

            Spacer(Modifier.height(8.dp))

            SettingsTextField(
                label = "Base URL",
                value = settings.llmBaseUrl,
                onValueChange = { onSettingsChanged(settings.copy(llmBaseUrl = it)) },
                placeholder = "http://localhost:11434/v1",
                enabled = settings.llmEnabled
            )

            Spacer(Modifier.height(6.dp))

            SettingsTextField(
                label = "Model",
                value = settings.llmModel,
                onValueChange = { onSettingsChanged(settings.copy(llmModel = it)) },
                placeholder = "llama3, gemini-1.5-flash, ...",
                enabled = settings.llmEnabled
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Temperature", style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
                Text(
                    "%.1f".format(settings.llmTemperature),
                    style = MaterialTheme.typography.labelSmall,
                    color = LolColors.GoldLight,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = settings.llmTemperature.toFloat(),
                onValueChange = { onSettingsChanged(settings.copy(llmTemperature = it.toDouble())) },
                valueRange = 0f..1.5f,
                steps = 14,
                enabled = settings.llmEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = LolColors.Gold,
                    activeTrackColor = LolColors.GoldDark,
                    inactiveTrackColor = LolColors.BlueMedium
                )
            )
        }

        // ── Polling ──
        DashboardCard {
            SectionHeader("Live Client", "📡")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Polling Interval", style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
                Text(
                    "${settings.pollingIntervalMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = LolColors.GoldLight,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = settings.pollingIntervalMs.toFloat(),
                onValueChange = { onSettingsChanged(settings.copy(pollingIntervalMs = it.toLong())) },
                valueRange = 500f..3000f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = LolColors.Gold,
                    activeTrackColor = LolColors.GoldDark,
                    inactiveTrackColor = LolColors.BlueMedium
                )
            )
            Text(
                "Lower = more responsive, higher = less CPU usage",
                style = MaterialTheme.typography.labelSmall,
                color = LolColors.Muted,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = LolColors.OnSurface, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LolColors.Gold,
                checkedTrackColor = LolColors.GoldDark,
                uncheckedThumbColor = LolColors.Muted,
                uncheckedTrackColor = LolColors.BlueMedium
            )
        )
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    enabled: Boolean = true
) {
    Text(label, style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
    Spacer(Modifier.height(3.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) LolColors.BlueMedium else LolColors.BlueMedium.copy(alpha = 0.5f))
            .border(1.dp, LolColors.Border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = if (enabled) LolColors.GoldLight else LolColors.Muted
            ),
            modifier = Modifier.fillMaxWidth(),
            cursorBrush = SolidColor(LolColors.Gold),
            singleLine = true
        )
        if (value.isEmpty()) {
            Text(placeholder, color = LolColors.Muted.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
        }
    }
}
