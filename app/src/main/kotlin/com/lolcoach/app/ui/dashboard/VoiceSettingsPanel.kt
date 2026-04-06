package com.lolcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lolcoach.app.i18n.Strings
import com.lolcoach.app.ui.theme.LolColors
import com.lolcoach.bridge.voice.DownloadState
import com.lolcoach.bridge.voice.VoiceDevice

// ─── Voice Settings Panel ───────────────────────────────────

@Composable
fun VoiceSettingsPanel(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    availableDevices: List<VoiceDevice>,
    selectedDevice: VoiceDevice?,
    onDeviceSelected: (VoiceDevice) -> Unit,
    onRefresh: () -> Unit,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    wakeWord: String,
    onWakeWordChanged: (String) -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    DashboardCard(modifier = modifier) {
        // Enable toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(Strings.VoiceCoaching, "🎙️")
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LolColors.Gold,
                    checkedTrackColor = LolColors.GoldDark,
                    uncheckedThumbColor = LolColors.Muted,
                    uncheckedTrackColor = LolColors.BlueMedium
                )
            )
        }

        // Status line
        val statusText = if (isListening) "🎙️ ${Strings.Listening}" else if (enabled) "✅ Active" else "⏸️ Disabled"
        val statusColor = if (isListening) LolColors.BlueLight else if (enabled) LolColors.SuccessGreen else LolColors.Muted
        Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor)

        Spacer(Modifier.height(10.dp))

        // Model download
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vosk Model", style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
            DownloadStatusBadge(downloadState, onDownload)
        }

        Spacer(Modifier.height(10.dp))

        // Wake word
        Text("Wake Word", style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(LolColors.BlueMedium)
                .border(1.dp, LolColors.Border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            BasicTextField(
                value = wakeWord,
                onValueChange = onWakeWordChanged,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = LolColors.GoldLight),
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(LolColors.Gold),
                singleLine = true
            )
            if (wakeWord.isEmpty()) {
                Text("e.g. Hey Coach", color = LolColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Microphone selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(Strings.Microphone, style = MaterialTheme.typography.labelSmall, color = LolColors.Muted)
            Text("🔄", fontSize = 12.sp, modifier = Modifier.clickable { onRefresh() })
        }
        Spacer(Modifier.height(4.dp))

        // Selected device display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(LolColors.BlueMedium)
                .border(1.dp, LolColors.Border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(
                selectedDevice?.name ?: Strings.NoDevices,
                style = MaterialTheme.typography.bodySmall,
                color = if (selectedDevice != null) LolColors.OnSurface else LolColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Device list
        if (availableDevices.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, LolColors.Border.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                availableDevices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .clickable { onDeviceSelected(device) }
                            .background(
                                if (device.name == selectedDevice?.name) LolColors.Gold.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape)
                                .background(
                                    if (device.name == selectedDevice?.name) LolColors.Gold
                                    else LolColors.Border
                                )
                        )
                        Text(
                            device.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.name == selectedDevice?.name) LolColors.GoldLight else LolColors.OnSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Quick guide (collapsed)
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = LolColors.Border.copy(alpha = 0.3f), thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        Text("💡 Quick Guide", style = MaterialTheme.typography.labelSmall, color = LolColors.Gold)
        Spacer(Modifier.height(4.dp))
        val instructions = listOf(
            "Enable voice coaching → download model → select mic",
            "Say your wake word, then ask your question",
            "The coach will respond via TTS with game-aware advice"
        )
        instructions.forEach { text ->
            Text(
                "• $text",
                style = MaterialTheme.typography.labelSmall,
                color = LolColors.Muted,
                lineHeight = 14.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
fun DownloadStatusBadge(state: DownloadState, onDownload: () -> Unit) {
    when (state) {
        is DownloadState.Idle -> {
            Text(
                Strings.DownloadModel,
                style = MaterialTheme.typography.labelSmall,
                color = LolColors.BlueLight,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .clickable { onDownload() }
                    .background(LolColors.BlueLight.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        is DownloadState.Downloading -> {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = LolColors.Gold)
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.width(60.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = LolColors.Gold,
                    trackColor = LolColors.Border
                )
            }
        }
        is DownloadState.Extracting -> {
            Text("⏳ Extracting...", style = MaterialTheme.typography.labelSmall, color = LolColors.BlueLight)
        }
        is DownloadState.Completed -> {
            Text("✅ Ready", style = MaterialTheme.typography.labelSmall, color = LolColors.SuccessGreen)
        }
        is DownloadState.Error -> {
            Text(
                "⚠️ Error — Retry",
                style = MaterialTheme.typography.labelSmall,
                color = LolColors.Danger,
                modifier = Modifier.clickable { onDownload() }
            )
        }
    }
}
