package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LiveConnectionState
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.ConnectionStatusPill
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.components.XavionAvatarView
import com.example.ui.theme.XavionAmber
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionEmerald
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.viewmodel.XavionViewModel

@Composable
fun LiveVoiceScreen(
    viewModel: XavionViewModel,
    onOpenApiConfig: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val inputAmp by viewModel.inputAmplitude.collectAsStateWithLifecycle()
    val outputAmp by viewModel.outputAmplitude.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isVoiceDetected by viewModel.isVoiceDetected.collectAsStateWithLifecycle()
    val userCaption by viewModel.liveUserCaption.collectAsStateWithLifecycle()
    val modelCaption by viewModel.liveModelCaption.collectAsStateWithLifecycle()
    val toolFeed by viewModel.liveToolFeed.collectAsStateWithLifecycle()
    val wakeWordStatus by viewModel.wakeWordStatus.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()

    var quickText by remember { mutableStateOf("") }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.connectLiveSession()
    }

    val connectOrDisconnect = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startOrToggleLiveSession()
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isConnected = liveState !is LiveConnectionState.Disconnected && liveState !is LiveConnectionState.Error

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val wide = maxWidth >= 780.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HoloPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    borderColor = XavionBorderBright
                ) {
                    LiveVoiceStage(
                        settings = settings,
                        liveState = liveState,
                        isConnected = isConnected,
                        isMuted = isMuted,
                        isVoiceDetected = isVoiceDetected,
                        inputAmp = inputAmp,
                        outputAmp = outputAmp,
                        hasApiKey = hasApiKey,
                        onConnectToggle = connectOrDisconnect,
                        onToggleMic = { viewModel.toggleLiveMic() },
                        onInterrupt = { viewModel.interruptLiveResponse() },
                        onCycleAvatar = { viewModel.cycleAvatarMode() },
                        onOpenApiConfig = onOpenApiConfig
                    )
                }

                HoloPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    LiveTelemetryAndTranscriptPane(
                        settings = settings,
                        userCaption = userCaption,
                        modelCaption = modelCaption,
                        toolFeed = toolFeed,
                        wakeWordStatus = wakeWordStatus,
                        isConnected = isConnected,
                        quickText = quickText,
                        onQuickTextChange = { quickText = it },
                        onSendQuickText = {
                            viewModel.sendLiveTextTurn(quickText)
                            quickText = ""
                        },
                        onToggleWakeWord = { viewModel.setWakeWordEnabled(it) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HoloPanel(borderColor = XavionBorderBright) {
                        LiveVoiceStage(
                            settings = settings,
                            liveState = liveState,
                            isConnected = isConnected,
                            isMuted = isMuted,
                            isVoiceDetected = isVoiceDetected,
                            inputAmp = inputAmp,
                            outputAmp = outputAmp,
                            hasApiKey = hasApiKey,
                            onConnectToggle = connectOrDisconnect,
                            onToggleMic = { viewModel.toggleLiveMic() },
                            onInterrupt = { viewModel.interruptLiveResponse() },
                            onCycleAvatar = { viewModel.cycleAvatarMode() },
                            onOpenApiConfig = onOpenApiConfig
                        )
                    }
                }
                item {
                    HoloPanel {
                        LiveTelemetryAndTranscriptPane(
                            settings = settings,
                            userCaption = userCaption,
                            modelCaption = modelCaption,
                            toolFeed = toolFeed,
                            wakeWordStatus = wakeWordStatus,
                            isConnected = isConnected,
                            quickText = quickText,
                            onQuickTextChange = { quickText = it },
                            onSendQuickText = {
                                viewModel.sendLiveTextTurn(quickText)
                                quickText = ""
                            },
                            onToggleWakeWord = { viewModel.setWakeWordEnabled(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveVoiceStage(
    settings: com.example.data.model.XavionSettings,
    liveState: LiveConnectionState,
    isConnected: Boolean,
    isMuted: Boolean,
    isVoiceDetected: Boolean,
    inputAmp: Float,
    outputAmp: Float,
    hasApiKey: Boolean,
    onConnectToggle: () -> Unit,
    onToggleMic: () -> Unit,
    onInterrupt: () -> Unit,
    onCycleAvatar: () -> Unit,
    onOpenApiConfig: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GEMINI LIVE VOICE",
                    style = MaterialTheme.typography.titleLarge,
                    color = XavionCyan
                )
                Text(
                    text = "Voice: ${settings.voiceName} • ${settings.liveModelId.removePrefix("models/")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = XavionTextMuted
                )
            }
            ConnectionStatusPill(state = liveState)
        }

        Spacer(modifier = Modifier.height(14.dp))

        XavionAvatarView(
            avatarMode = settings.avatarMode,
            connectionState = liveState,
            inputAmplitude = inputAmp,
            outputAmplitude = outputAmp,
            size = 240.dp,
            onTap = onCycleAvatar
        )

        Text(
            text = "Tap avatar to switch visual mode (${settings.avatarMode.displayName})",
            style = MaterialTheme.typography.labelSmall,
            color = XavionTextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        AudioWaveformBar(
            inputAmplitude = inputAmp,
            outputAmplitude = outputAmp,
            isActive = isConnected
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                liveState is LiveConnectionState.Error -> liveState.message
                isVoiceDetected -> "Voice Activity Detected (VAD Active)"
                isMuted -> "Microphone Muted"
                isConnected -> "Full-Duplex 16kHz PCM Audio Link Active"
                else -> "Ready to initialize Gemini Live WebSocket"
            },
            style = MaterialTheme.typography.labelMedium,
            color = when {
                liveState is LiveConnectionState.Error -> XavionCrimson
                isVoiceDetected -> XavionEmerald
                else -> XavionTextSecondary
            }
        )

        if (!hasApiKey) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenApiConfig,
                border = BorderStroke(1.dp, XavionAmber),
                modifier = Modifier.testTag("live_open_api_config")
            ) {
                Text("Configure Gemini API Key First", color = XavionAmber)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onConnectToggle,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("live_connect_toggle"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) XavionCrimson else XavionCyan,
                    contentColor = if (isConnected) XavionTextPrimary else XavionBlack
                )
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CallEnd else Icons.Default.PowerSettingsNew,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isConnected) "Disconnect" else "Start Live Session")
            }

            OutlinedButton(
                onClick = onToggleMic,
                enabled = isConnected,
                modifier = Modifier
                    .height(50.dp)
                    .testTag("live_mute_toggle"),
                border = BorderStroke(1.dp, if (isMuted) XavionAmber else XavionBorderBright)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute microphone" else "Mute microphone",
                    tint = if (isMuted) XavionAmber else XavionCyan
                )
            }

            OutlinedButton(
                onClick = onInterrupt,
                enabled = isConnected,
                modifier = Modifier
                    .height(50.dp)
                    .testTag("live_interrupt_button"),
                border = BorderStroke(1.dp, XavionBorderSubtle)
            ) {
                Icon(
                    imageVector = Icons.Default.PanTool,
                    contentDescription = "Interrupt Xavion",
                    tint = XavionCyan
                )
            }
        }
    }
}

@Composable
private fun LiveTelemetryAndTranscriptPane(
    settings: com.example.data.model.XavionSettings,
    userCaption: String,
    modelCaption: String,
    toolFeed: List<String>,
    wakeWordStatus: String,
    isConnected: Boolean,
    quickText: String,
    onQuickTextChange: (String) -> Unit,
    onSendQuickText: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HoloSectionHeader(title = "Live Transcript Stream")

        Surface(
            color = XavionPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, XavionBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("YOU", style = MaterialTheme.typography.labelSmall, color = XavionEmerald)
                Text(
                    text = userCaption.ifBlank { "Speak naturally when Live Link is active..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (userCaption.isBlank()) XavionTextMuted else XavionTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("XAVION", style = MaterialTheme.typography.labelSmall, color = XavionCyan)
                Text(
                    text = modelCaption.ifBlank { "Awaiting voice or text prompt..." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (modelCaption.isBlank()) XavionTextMuted else XavionTextPrimary
                )
            }
        }

        // Send text turn inside active Live session
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = quickText,
                onValueChange = onQuickTextChange,
                enabled = isConnected,
                placeholder = { Text("Inject text prompt into Live session...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("live_text_inject_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XavionCyan,
                    unfocusedBorderColor = XavionBorderSubtle
                )
            )
            IconButton(
                onClick = onSendQuickText,
                enabled = isConnected && quickText.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("live_text_inject_send")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to Live", tint = XavionCyan)
            }
        }

        // Wake word quick toggle
        Surface(
            color = XavionPanelDark,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, XavionBorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = XavionCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("\"Hey XAVION\" Wake Phrase", style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                        Text(wakeWordStatus, style = MaterialTheme.typography.labelSmall, color = XavionTextMuted)
                    }
                }
                Switch(
                    checked = settings.wakeWordEnabled,
                    onCheckedChange = onToggleWakeWord,
                    modifier = Modifier.testTag("live_wake_word_switch"),
                    colors = SwitchDefaults.colors(checkedTrackColor = XavionCyan.copy(alpha = 0.4f), checkedThumbColor = XavionCyan)
                )
            }
        }

        HoloSectionHeader(title = "Function Call Telemetry")
        if (toolFeed.isEmpty()) {
            Text(
                text = "Device commands invoked by XAVION during Live sessions appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = XavionTextMuted
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(140.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(toolFeed) { item ->
                    Surface(
                        color = XavionPanelDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, XavionBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item, style = MaterialTheme.typography.labelSmall, color = XavionTextPrimary)
                        }
                    }
                }
            }
        }
    }
}
