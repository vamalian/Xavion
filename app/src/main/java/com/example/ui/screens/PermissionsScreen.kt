package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
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
fun PermissionsScreen(viewModel: XavionViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val serviceRunning by viewModel.serviceRunning.collectAsStateWithLifecycle()
    val wakeStatus by viewModel.wakeWordStatus.collectAsStateWithLifecycle()
    val bubbleShowing by viewModel.bubbleShowing.collectAsStateWithLifecycle()
    val notifListenerConnected by viewModel.notificationListenerConnected.collectAsStateWithLifecycle()

    var refreshTick by remember { mutableIntStateOf(0) }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTick++
        viewModel.refreshCalendarAgenda()
    }

    val micGranted = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val camGranted = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val notifGranted = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    val calGranted = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
    val overlayGranted = remember(refreshTick) { Settings.canDrawOverlays(context) }
    val writeSettingsGranted = remember(refreshTick) { Settings.System.canWrite(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(
                    title = "Background Service, Wake Word & Bubble",
                    subtitle = if (serviceRunning) "Foreground Service Active" else "Service Stopped"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.startForegroundAssistant() },
                        enabled = micGranted && !serviceRunning,
                        modifier = Modifier.testTag("perms_start_service"),
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) { Text("Start Service") }
                    OutlinedButton(
                        onClick = { viewModel.stopForegroundAssistant() },
                        enabled = serviceRunning,
                        modifier = Modifier.testTag("perms_stop_service"),
                        border = BorderStroke(1.dp, XavionCrimson)
                    ) { Text("Stop Service", color = XavionCrimson) }
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
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("\"Hey XAVION\" Wake Phrase", style = MaterialTheme.typography.titleMedium)
                            Text(wakeStatus, style = MaterialTheme.typography.labelSmall, color = XavionCyan)
                        }
                        Switch(
                            checked = settings.wakeWordEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !micGranted) {
                                    permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                } else {
                                    viewModel.setWakeWordEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = XavionPanelDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, XavionBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Floating Assistant Bubble", style = MaterialTheme.typography.titleMedium)
                            Text(
                                when {
                                    !overlayGranted -> "Overlay permission required"
                                    bubbleShowing -> "Floating orb visible over apps"
                                    else -> "Ready (requires active foreground service)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (overlayGranted) XavionCyan else XavionAmber
                            )
                        }
                        Switch(
                            checked = settings.floatingBubbleEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !overlayGranted) {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } else {
                                    viewModel.setFloatingBubbleEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Android Background Restriction Notice: Android does not permit third-party apps to keep the microphone open after the app is force-closed. Wake-word detection and persistent voice operate strictly while XAVION or its explicit Foreground Service notification is running.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = XavionTextSecondary
                )
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(title = "Runtime & Special Permissions")
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PermissionRow(
                        title = "Microphone (Voice & Wake Word)",
                        granted = micGranted,
                        actionLabel = "Grant",
                        onAction = { permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) }
                    )
                    PermissionRow(
                        title = "Camera (Vision Mode & Torch)",
                        granted = camGranted,
                        actionLabel = "Grant",
                        onAction = { permLauncher.launch(arrayOf(Manifest.permission.CAMERA)) }
                    )
                    PermissionRow(
                        title = "Post Notifications (Service Status)",
                        granted = notifGranted,
                        actionLabel = "Grant",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            }
                        }
                    )
                    PermissionRow(
                        title = "Calendar Read / Write",
                        granted = calGranted,
                        actionLabel = "Grant",
                        onAction = {
                            permLauncher.launch(
                                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                            )
                        }
                    )
                    PermissionRow(
                        title = "Notification Listener Access",
                        granted = notifListenerConnected,
                        actionLabel = "Open Settings",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                    PermissionRow(
                        title = "Floating Overlay (Display Over Apps)",
                        granted = overlayGranted,
                        actionLabel = "Open Settings",
                        onAction = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                    PermissionRow(
                        title = "Modify System Settings (Brightness)",
                        granted = writeSettingsGranted,
                        actionLabel = "Open Settings",
                        onAction = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    border = BorderStroke(1.dp, XavionBorderBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Battery Optimization Guidance Settings", color = XavionCyan)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        color = XavionPanelDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (granted) XavionEmerald.copy(alpha = 0.45f) else XavionBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                Text(
                    if (granted) "GRANTED" else "NOT GRANTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) XavionEmerald else XavionAmber
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onAction, border = BorderStroke(1.dp, XavionCyan)) {
                    Text(actionLabel, color = XavionCyan, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = XavionEmerald)
            }
        }
    }
}
