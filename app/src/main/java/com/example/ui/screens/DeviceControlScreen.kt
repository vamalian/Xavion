package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.theme.XavionAmber
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionEmerald
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.viewmodel.XavionViewModel
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceControlScreen(
    viewModel: XavionViewModel,
    onOpenPermissions: () -> Unit
) {
    val context = LocalContext.current
    val telemetry by viewModel.deviceTelemetry.collectAsStateWithLifecycle()
    val flashlightOn by viewModel.flashlightOn.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val notifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val notifConnected by viewModel.notificationListenerConnected.collectAsStateWithLifecycle()
    val toolLogs by viewModel.toolLogs.collectAsStateWithLifecycle()

    var volumeSlider by remember(telemetry.mediaVolumePercent) { mutableFloatStateOf(telemetry.mediaVolumePercent.toFloat()) }
    var brightnessSlider by remember { mutableFloatStateOf(70f) }
    var appFilter by remember { mutableStateOf("") }
    var websiteUrl by remember { mutableStateOf("https://ai.google.dev") }
    var sharePayload by remember { mutableStateOf("Status report from XAVION AI.") }

    val canWriteSettings = Settings.System.canWrite(context)

    val filteredApps = remember(installedApps, appFilter) {
        if (appFilter.isBlank()) installedApps.take(12)
        else installedApps.filter { it.label.contains(appFilter, ignoreCase = true) }.take(16)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(
                    title = "Hardware & System Controls",
                    subtitle = "Battery ${telemetry.batteryPercent}% • ${telemetry.networkType}"
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Flashlight & System Settings shortcuts
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.runDirectCommand(
                                "toggle_flashlight",
                                JSONObject().put("enabled", !flashlightOn)
                            )
                        },
                        modifier = Modifier.testTag("device_flashlight_toggle"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flashlightOn) XavionAmber else XavionCyan,
                            contentColor = XavionBlack
                        )
                    ) {
                        Icon(if (flashlightOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (flashlightOn) "Torch ON" else "Torch OFF")
                    }

                    OutlinedButton(
                        onClick = { viewModel.runDirectCommand("open_settings", JSONObject().put("setting_type", "wifi")) },
                        border = BorderStroke(1.dp, XavionBorderSubtle)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wi-Fi", color = XavionTextPrimary)
                    }

                    OutlinedButton(
                        onClick = { viewModel.runDirectCommand("open_settings", JSONObject().put("setting_type", "bluetooth")) },
                        border = BorderStroke(1.dp, XavionBorderSubtle)
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bluetooth", color = XavionTextPrimary)
                    }

                    OutlinedButton(
                        onClick = { viewModel.runDirectCommand("open_settings", JSONObject().put("setting_type", "general")) },
                        border = BorderStroke(1.dp, XavionBorderSubtle)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Settings", color = XavionTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Media Volume Slider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = XavionCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Media Volume: ${volumeSlider.roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                }
                Slider(
                    value = volumeSlider,
                    onValueChange = { volumeSlider = it },
                    onValueChangeFinished = {
                        viewModel.runDirectCommand(
                            "set_volume",
                            JSONObject().put("stream", "media").put("level_percent", volumeSlider.roundToInt())
                        )
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = XavionCyan, activeTrackColor = XavionCyan)
                )

                // Screen Brightness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness6, contentDescription = "Brightness", tint = XavionCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Screen Brightness: ${brightnessSlider.roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                    }
                    if (!canWriteSettings) {
                        OutlinedButton(onClick = onOpenPermissions, border = BorderStroke(1.dp, XavionAmber)) {
                            Text("Grant Write", color = XavionAmber, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Slider(
                    value = brightnessSlider,
                    onValueChange = { brightnessSlider = it },
                    onValueChangeFinished = {
                        viewModel.runDirectCommand(
                            "set_brightness",
                            JSONObject().put("percent", brightnessSlider.roundToInt())
                        )
                    },
                    enabled = canWriteSettings,
                    valueRange = 5f..100f,
                    colors = SliderDefaults.colors(thumbColor = XavionCyan, activeTrackColor = XavionCyan)
                )
            }
        }

        // Open Website & Share Intents
        item {
            HoloPanel {
                HoloSectionHeader(title = "Web & Share Intents")
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("HTTPS URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    Button(
                        onClick = { viewModel.runDirectCommand("open_website", JSONObject().put("url", websiteUrl)) },
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sharePayload,
                        onValueChange = { sharePayload = it },
                        label = { Text("Text to Share") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.runDirectCommand(
                                "share_text",
                                JSONObject().put("title", "XAVION AI").put("text", sharePayload)
                            )
                        },
                        border = BorderStroke(1.dp, XavionBorderBright)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = XavionCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = XavionCyan)
                    }
                }
            }
        }

        // Permitted Notifications Reader
        item {
            HoloPanel {
                HoloSectionHeader(
                    title = "Active Notifications",
                    subtitle = if (notifConnected) "${notifications.size} visible" else "Notification Access needed",
                    trailing = {
                        if (!notifConnected) {
                            OutlinedButton(onClick = onOpenPermissions, border = BorderStroke(1.dp, XavionAmber)) {
                                Text("Authorize", color = XavionAmber, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!notifConnected) {
                    Text(
                        "Enable Notification Access in Permissions so XAVION can read incoming alerts aloud when asked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XavionTextMuted
                    )
                } else if (notifications.isEmpty()) {
                    Text("No active notifications right now.", style = MaterialTheme.typography.bodyMedium, color = XavionTextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        notifications.take(5).forEach { n ->
                            Surface(
                                color = XavionPanelDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, XavionBorderSubtle)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${n.appName} • ${n.title}", style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                                        if (n.text.isNotBlank()) {
                                            Text(n.text, style = MaterialTheme.typography.bodyMedium, color = XavionTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Installed Applications Launcher
        item {
            HoloPanel {
                HoloSectionHeader(title = "Installed Applications", subtitle = "${installedApps.size} launchable apps")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = appFilter,
                    onValueChange = { appFilter = it },
                    placeholder = { Text("Filter installed apps...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredApps.forEach { app ->
                        Surface(
                            color = XavionPanelDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, XavionBorderSubtle),
                            modifier = Modifier.clickable { viewModel.launchInstalledApp(app.packageName) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(app.label, style = MaterialTheme.typography.labelMedium, color = XavionTextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Command Execution Audit Log
        item {
            HoloPanel {
                HoloSectionHeader(title = "Command Audit Trail")
                Spacer(modifier = Modifier.height(8.dp))
                if (toolLogs.isEmpty()) {
                    Text("No device commands executed yet.", style = MaterialTheme.typography.bodyMedium, color = XavionTextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        toolLogs.take(10).forEach { log ->
                            Surface(
                                color = XavionPanelDark,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, XavionBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.argumentsSummary, style = MaterialTheme.typography.bodyMedium, color = XavionTextPrimary)
                                        Text(log.resultMessage, style = MaterialTheme.typography.labelSmall, color = XavionTextMuted)
                                    }
                                    Text(
                                        log.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (log.status == "EXECUTED") XavionEmerald else XavionAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
