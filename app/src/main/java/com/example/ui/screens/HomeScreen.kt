package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AvatarMode
import com.example.data.model.LiveConnectionState
import com.example.ui.components.AudioWaveformBar
import com.example.ui.components.ConnectionStatusPill
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.components.XavionAvatarView
import com.example.ui.navigation.XavionRoute
import com.example.ui.theme.XavionAmber
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCobalt
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionEmerald
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.viewmodel.XavionViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: XavionViewModel,
    onNavigate: (XavionRoute) -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val inputAmp by viewModel.inputAmplitude.collectAsStateWithLifecycle()
    val outputAmp by viewModel.outputAmplitude.collectAsStateWithLifecycle()
    val isMicActive by viewModel.isMicActive.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val telemetry by viewModel.deviceTelemetry.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val weatherError by viewModel.weatherError.collectAsStateWithLifecycle()
    val recentConversations by viewModel.recentConversations.collectAsStateWithLifecycle()
    val dailyBriefing by viewModel.dailyBriefing.collectAsStateWithLifecycle()
    val briefingLoading by viewModel.briefingLoading.collectAsStateWithLifecycle()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startOrToggleLiveSession()
            onNavigate(XavionRoute.LiveVoice)
        }
    }

    val handleTapToTalk = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startOrToggleLiveSession()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isLiveConnected = liveState !is LiveConnectionState.Disconnected && liveState !is LiveConnectionState.Error

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideTablet = maxWidth >= 780.dp

        if (isWideTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Focal Holographic Core Column
                HoloPanel(
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxSize(),
                    borderColor = XavionBorderBright
                ) {
                    HomeFocalAvatarColumn(
                        settings = settings,
                        liveState = liveState,
                        isLiveConnected = isLiveConnected,
                        hasApiKey = hasApiKey,
                        inputAmp = inputAmp,
                        outputAmp = outputAmp,
                        isMicActive = isMicActive,
                        onSelectAvatarMode = { mode ->
                            viewModel.updateSettings { it.copy(avatarMode = mode) }
                        },
                        onTapToTalk = handleTapToTalk,
                        onOpenLive = { onNavigate(XavionRoute.LiveVoice) },
                        onOpenApiConfig = { onNavigate(XavionRoute.ApiConfig) },
                        onOpenSettings = { onNavigate(XavionRoute.AiSettings) }
                    )
                }

                // Right Telemetry, Weather, Quick Actions & Conversations
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        HomeTelemetryAndClockCard(
                            nowMillis = nowMillis,
                            telemetry = telemetry,
                            weather = weather,
                            weatherError = weatherError,
                            city = settings.weatherCity,
                            onRefreshWeather = { viewModel.refreshWeather() }
                        )
                    }
                    item {
                        HomeBriefingCard(
                            briefing = dailyBriefing,
                            loading = briefingLoading,
                            onGenerate = { viewModel.generateDailyBriefing(speakAloud = false) },
                            onSpeak = { viewModel.generateDailyBriefing(speakAloud = true) }
                        )
                    }
                    item {
                        HomeQuickActionsPanel(onNavigate = onNavigate)
                    }
                    item {
                        HomeRecentConversationsPanel(
                            conversations = recentConversations,
                            onOpenConversation = { id ->
                                viewModel.openConversation(id)
                                onNavigate(XavionRoute.TextChat)
                            },
                            onNewChat = {
                                viewModel.startNewTextConversation()
                                onNavigate(XavionRoute.TextChat)
                            }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HoloPanel(borderColor = XavionBorderBright) {
                        HomeFocalAvatarColumn(
                            settings = settings,
                            liveState = liveState,
                            isLiveConnected = isLiveConnected,
                            hasApiKey = hasApiKey,
                            inputAmp = inputAmp,
                            outputAmp = outputAmp,
                            isMicActive = isMicActive,
                            onSelectAvatarMode = { mode ->
                                viewModel.updateSettings { it.copy(avatarMode = mode) }
                            },
                            onTapToTalk = handleTapToTalk,
                            onOpenLive = { onNavigate(XavionRoute.LiveVoice) },
                            onOpenApiConfig = { onNavigate(XavionRoute.ApiConfig) },
                            onOpenSettings = { onNavigate(XavionRoute.AiSettings) }
                        )
                    }
                }
                item {
                    HomeTelemetryAndClockCard(
                        nowMillis = nowMillis,
                        telemetry = telemetry,
                        weather = weather,
                        weatherError = weatherError,
                        city = settings.weatherCity,
                        onRefreshWeather = { viewModel.refreshWeather() }
                    )
                }
                item {
                    HomeBriefingCard(
                        briefing = dailyBriefing,
                        loading = briefingLoading,
                        onGenerate = { viewModel.generateDailyBriefing(speakAloud = false) },
                        onSpeak = { viewModel.generateDailyBriefing(speakAloud = true) }
                    )
                }
                item { HomeQuickActionsPanel(onNavigate = onNavigate) }
                item {
                    HomeRecentConversationsPanel(
                        conversations = recentConversations,
                        onOpenConversation = { id ->
                            viewModel.openConversation(id)
                            onNavigate(XavionRoute.TextChat)
                        },
                        onNewChat = {
                            viewModel.startNewTextConversation()
                            onNavigate(XavionRoute.TextChat)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFocalAvatarColumn(
    settings: com.example.data.model.XavionSettings,
    liveState: LiveConnectionState,
    isLiveConnected: Boolean,
    hasApiKey: Boolean,
    inputAmp: Float,
    outputAmp: Float,
    isMicActive: Boolean,
    onSelectAvatarMode: (AvatarMode) -> Unit,
    onTapToTalk: () -> Unit,
    onOpenLive: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenSettings: () -> Unit
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
            Column {
                Text(
                    text = "XAVION AI",
                    style = MaterialTheme.typography.headlineMedium,
                    color = XavionCyan
                )
                Text(
                    text = "GEMINI LIVE COMPANION",
                    style = MaterialTheme.typography.labelSmall,
                    color = XavionTextMuted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionStatusPill(state = liveState)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("home_settings_shortcut")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "AI Settings",
                        tint = XavionCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Switchable Avatar Mode Selector
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarMode.entries.forEach { mode ->
                val shortLabel = when (mode) {
                    AvatarMode.MALE_VIRTUAL_AVATAR -> "Avatar"
                    AvatarMode.HOLOGRAPHIC_CORE -> "Core"
                    AvatarMode.MINIMAL_VOICE -> "Minimal"
                }
                FilterChip(
                    selected = settings.avatarMode == mode,
                    onClick = { onSelectAvatarMode(mode) },
                    modifier = Modifier.testTag("avatar_mode_${mode.name.lowercase()}"),
                    label = { Text(shortLabel, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = XavionCyan.copy(alpha = 0.2f),
                        selectedLabelColor = XavionCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        XavionAvatarView(
            avatarMode = settings.avatarMode,
            connectionState = liveState,
            inputAmplitude = inputAmp,
            outputAmplitude = outputAmp,
            size = 228.dp,
            onTap = onTapToTalk
        )

        Spacer(modifier = Modifier.height(10.dp))

        AudioWaveformBar(
            inputAmplitude = inputAmp,
            outputAmplitude = outputAmp,
            isActive = isLiveConnected || isMicActive
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (settings.addressAsSir) {
                "Good day, ${settings.ownerName.ifBlank { "Sir" }}. XAVION systems are online."
            } else {
                "XAVION systems are online. How may I assist you?"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = XavionTextSecondary
        )

        if (!hasApiKey) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = XavionAmber.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, XavionAmber.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenApiConfig() }
                    .testTag("home_api_key_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = XavionAmber)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini API Key not set. Tap here to configure securely in Android Keystore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XavionTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onTapToTalk,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("home_tap_to_talk_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLiveConnected) XavionCrimson else XavionCyan,
                    contentColor = if (isLiveConnected) XavionTextPrimary else XavionBlack
                )
            ) {
                Icon(
                    imageVector = if (isLiveConnected) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLiveConnected) "End Live Link" else "Tap to Talk",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onOpenLive,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("home_open_cockpit_button"),
                border = BorderStroke(1.dp, XavionBorderBright)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = XavionCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cockpit", color = XavionCyan)
            }
        }
    }
}

@Composable
private fun HomeTelemetryAndClockCard(
    nowMillis: Long,
    telemetry: com.example.data.model.DeviceTelemetry,
    weather: com.example.data.model.WeatherInfo?,
    weatherError: String?,
    city: String,
    onRefreshWeather: () -> Unit
) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(nowMillis))
    val dateStr = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(nowMillis))

    HoloPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.displaySmall,
                    color = XavionTextPrimary
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = XavionCyan
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        contentDescription = "Battery",
                        tint = if (telemetry.batteryPercent > 20) XavionEmerald else XavionCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${telemetry.batteryPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = XavionTextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (telemetry.networkAvailable) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = "Network",
                        tint = if (telemetry.networkAvailable) XavionCyan else XavionCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = telemetry.networkType,
                        style = MaterialTheme.typography.labelSmall,
                        color = XavionTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    Icon(Icons.Default.Cloud, contentDescription = "Weather", tint = XavionCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        if (weather != null) {
                            Text(
                                text = "${weather.city} • ${weather.temperatureCelsius}°C",
                                style = MaterialTheme.typography.titleMedium,
                                color = XavionTextPrimary
                            )
                            Text(
                                text = "${weather.conditionLabel} • Wind ${weather.windKmh} km/h",
                                style = MaterialTheme.typography.labelSmall,
                                color = XavionTextSecondary
                            )
                        } else {
                            Text(
                                text = "Weather ($city)",
                                style = MaterialTheme.typography.titleMedium,
                                color = XavionTextPrimary
                            )
                            Text(
                                text = weatherError ?: "Tap refresh to sync weather telemetry",
                                style = MaterialTheme.typography.labelSmall,
                                color = XavionTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onRefreshWeather,
                    modifier = Modifier.testTag("home_refresh_weather")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh weather", tint = XavionCyan)
                }
            }
        }
    }
}

@Composable
private fun HomeBriefingCard(
    briefing: String,
    loading: Boolean,
    onGenerate: () -> Unit,
    onSpeak: () -> Unit
) {
    HoloPanel {
        HoloSectionHeader(
            title = "Daily Briefing",
            subtitle = "Weather, tasks & calendar summary",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onSpeak,
                        enabled = !loading,
                        modifier = Modifier.testTag("home_speak_briefing")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speak briefing", tint = XavionCyan)
                    }
                    OutlinedButton(
                        onClick = onGenerate,
                        enabled = !loading,
                        modifier = Modifier.testTag("home_generate_briefing"),
                        border = BorderStroke(1.dp, XavionBorderBright),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = XavionCyan)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Brief Me", color = XavionCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = briefing.ifBlank {
                "Tap 'Brief Me' to compile your live battery, weather, open tasks, and upcoming calendar events."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (briefing.isBlank()) XavionTextMuted else XavionTextPrimary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeQuickActionsPanel(onNavigate: (XavionRoute) -> Unit) {
    HoloPanel {
        HoloSectionHeader(title = "Command Modules")
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionButton("Text Chat", Icons.Default.Chat, "qa_chat") { onNavigate(XavionRoute.TextChat) }
            QuickActionButton("Vision Mode", Icons.Default.CameraAlt, "qa_vision") { onNavigate(XavionRoute.Vision) }
            QuickActionButton("Device Ctrl", Icons.Default.Devices, "qa_device") { onNavigate(XavionRoute.DeviceControl) }
            QuickActionButton("Memory", Icons.Default.Psychology, "qa_memory") { onNavigate(XavionRoute.Memory) }
            QuickActionButton("API Vault", Icons.Default.Key, "qa_api") { onNavigate(XavionRoute.ApiConfig) }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        border = BorderStroke(1.dp, XavionBorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = XavionPanelDark)
    ) {
        Icon(icon, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = XavionTextPrimary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun HomeRecentConversationsPanel(
    conversations: List<com.example.data.local.ConversationEntity>,
    onOpenConversation: (Long) -> Unit,
    onNewChat: () -> Unit
) {
    HoloPanel {
        HoloSectionHeader(
            title = "Recent Sessions",
            trailing = {
                OutlinedButton(
                    onClick = onNewChat,
                    modifier = Modifier.testTag("home_new_chat"),
                    border = BorderStroke(1.dp, XavionBorderSubtle),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+ New Chat", color = XavionCyan, style = MaterialTheme.typography.labelSmall)
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (conversations.isEmpty()) {
            Text(
                text = "No saved sessions yet. Start a Live Voice link or Text Chat to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = XavionTextMuted
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                conversations.forEach { conv ->
                    Surface(
                        color = XavionPanelDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, XavionBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenConversation(conv.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = conv.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = XavionTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = conv.mode.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = XavionCyan
                                )
                            }
                            Text(
                                text = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(conv.updatedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = XavionTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
