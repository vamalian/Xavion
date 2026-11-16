package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricStatus
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
fun AboutSecurityScreen(viewModel: XavionViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasPin by viewModel.hasPinConfigured.collectAsStateWithLifecycle()
    val bioStatus = remember { BiometricAuthHelper.checkStatus(context) }

    var pinInput by remember { mutableStateOf("") }
    var showWipeDialog by remember { mutableStateOf(false) }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            containerColor = XavionPanelDark,
            title = { Text("Wipe All XAVION Data?", color = XavionCrimson) },
            text = {
                Text(
                    "This removes your encrypted API keys, PIN, saved conversations, memories, tool logs, and settings from this device.",
                    color = XavionTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllAppData()
                        showWipeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XavionCrimson, contentColor = XavionTextPrimary)
                ) { Text("Confirm Wipe") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showWipeDialog = false }) { Text("Cancel", color = XavionCyan) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(
                    title = "Owner Security & Access Lock",
                    subtitle = if (settings.appLockEnabled) "App Lock Active" else "Unlocked Mode"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter(Char::isDigit).take(6) },
                        label = { Text(if (hasPin) "Change Owner PIN (4–6 digits)" else "Set Owner PIN (4–6 digits)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("security_pin_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    Button(
                        onClick = {
                            viewModel.setOwnerPin(pinInput)
                            pinInput = ""
                        },
                        enabled = pinInput.length >= 4,
                        modifier = Modifier.testTag("security_save_pin"),
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save PIN")
                    }
                }

                if (hasPin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearOwnerPin() },
                        border = BorderStroke(1.dp, XavionCrimson)
                    ) {
                        Text("Remove Owner PIN & Disable Lock", color = XavionCrimson)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (bioStatus == BiometricStatus.AVAILABLE) XavionEmerald else XavionAmber
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Biometric Authentication", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    when (bioStatus) {
                                        BiometricStatus.AVAILABLE -> "Biometric hardware verified & enrolled"
                                        BiometricStatus.NOT_ENROLLED -> "Hardware present, enroll biometrics in Android Settings"
                                        BiometricStatus.NO_HARDWARE -> "No biometric sensor detected on this device"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bioStatus == BiometricStatus.AVAILABLE) XavionEmerald else XavionAmber
                                )
                            }
                        }
                        Switch(
                            checked = settings.biometricUnlockEnabled,
                            enabled = bioStatus == BiometricStatus.AVAILABLE && hasPin,
                            onCheckedChange = { enabled ->
                                viewModel.updateSettings { it.copy(biometricUnlockEnabled = enabled) }
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
                            Text("Screenshot Protection (FLAG_SECURE)", style = MaterialTheme.typography.titleMedium)
                            Text("Prevents screenshots and screen recording while viewing XAVION.", style = MaterialTheme.typography.labelSmall, color = XavionTextMuted)
                        }
                        Switch(
                            checked = settings.screenshotProtectionEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateSettings { it.copy(screenshotProtectionEnabled = enabled) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showWipeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_wipe_all_button"),
                    border = BorderStroke(1.dp, XavionCrimson)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = XavionCrimson)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe All Local Data, Keys & History", color = XavionCrimson)
                }
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(title = "About XAVION AI", subtitle = "v1.0 • Tablet Holographic Edition")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "XAVION AI is a native Android tablet personal assistant built with Kotlin, Jetpack Compose, Room, DataStore, Android Keystore AES-256-GCM, CameraX, and the Google Gemini Live Bidirectional Streaming API.\n\n" +
                        "Privacy Commitment: All conversations, tasks, notes, and memories stay stored locally in your device's Room SQLite database. Your Gemini API key is encrypted inside Android Keystore and transmitted directly to Google's Gemini API endpoints only when you initiate a request.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = XavionTextSecondary
                )
            }
        }
    }
}
