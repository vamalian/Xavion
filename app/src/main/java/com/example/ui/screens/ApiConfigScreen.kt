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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiProviderType
import com.example.ui.components.ConnectionStatusPill
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
fun ApiConfigScreen(viewModel: XavionViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val maskedKey by viewModel.maskedApiKey.collectAsStateWithLifecycle()
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val testResult by viewModel.connectivityResult.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnectivity.collectAsStateWithLifecycle()

    var keyDraft by remember { mutableStateOf("") }
    var showDraft by remember { mutableStateOf(false) }
    var customBaseUrl by remember(settings.customProviderBaseUrl) { mutableStateOf(settings.customProviderBaseUrl) }
    var customModel by remember(settings.customProviderModel) { mutableStateOf(settings.customProviderModel) }
    var customKeyDraft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(
                    title = "Encrypted API Key Vault",
                    subtitle = "Protected by Android Keystore AES-256-GCM",
                    trailing = { ConnectionStatusPill(state = liveState) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = XavionPanelDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (hasApiKey) XavionEmerald.copy(alpha = 0.5f) else XavionAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (hasApiKey) XavionEmerald else XavionAmber
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasApiKey) "Gemini API Key Stored" else "No Gemini API Key Stored",
                                style = MaterialTheme.typography.titleMedium,
                                color = XavionTextPrimary
                            )
                            Text(
                                text = if (hasApiKey) maskedKey else "Enter your Google AI Studio Gemini API key below for personal testing.",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (hasApiKey) XavionCyan else XavionTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyDraft,
                    onValueChange = { keyDraft = it },
                    label = { Text(if (hasApiKey) "Replace Gemini API Key" else "Enter Gemini API Key") },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    visualTransformation = if (showDraft) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showDraft = !showDraft }) {
                            Icon(
                                imageVector = if (showDraft) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle key visibility",
                                tint = XavionCyan
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XavionCyan,
                        unfocusedBorderColor = XavionBorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveApiKey(keyDraft)
                            keyDraft = ""
                        },
                        enabled = keyDraft.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("api_key_save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) {
                        Text(if (hasApiKey) "Update Key" else "Save Key")
                    }

                    OutlinedButton(
                        onClick = { viewModel.testApiConnectivity() },
                        enabled = !isTesting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("api_test_connectivity_button"),
                        border = BorderStroke(1.dp, XavionBorderBright)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = XavionCyan)
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = XavionCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Link", color = XavionCyan)
                        }
                    }

                    if (hasApiKey) {
                        OutlinedButton(
                            onClick = { viewModel.clearApiKey() },
                            modifier = Modifier.testTag("api_key_clear_button"),
                            border = BorderStroke(1.dp, XavionCrimson)
                        ) {
                            Text("Clear", color = XavionCrimson)
                        }
                    }
                }

                testResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = XavionPanelDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (result.success) XavionEmerald else XavionCrimson)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (result.success) XavionEmerald else XavionCrimson
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (result.success) {
                                        "VERIFIED • HTTP ${result.statusCode} • ${result.latencyMs}ms"
                                    } else {
                                        "FAILED • ${if (result.statusCode > 0) "HTTP ${result.statusCode}" else "NETWORK"}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (result.success) XavionEmerald else XavionCrimson
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(result.message, style = MaterialTheme.typography.bodyMedium, color = XavionTextPrimary)
                            if (result.discoveredModels.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Models: ${result.discoveredModels.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = XavionCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optional Future AI Provider Configuration
        item {
            HoloPanel {
                HoloSectionHeader(
                    title = "Extensible Provider Interface",
                    subtitle = "Active: ${settings.activeProvider.displayName}"
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customBaseUrl,
                    onValueChange = { customBaseUrl = it },
                    label = { Text("Custom OpenAI-Compatible Base URL (optional)") },
                    placeholder = { Text("https://api.example.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    label = { Text("Custom Provider Model ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customKeyDraft,
                    onValueChange = { customKeyDraft = it },
                    label = { Text("Custom Provider Token (Keystore Encrypted)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            viewModel.updateSettings {
                                it.copy(
                                    customProviderBaseUrl = customBaseUrl.trim(),
                                    customProviderModel = customModel.trim()
                                )
                            }
                            if (customKeyDraft.isNotBlank()) {
                                viewModel.saveApiKey(customKeyDraft, isCustomProvider = true)
                                customKeyDraft = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) { Text("Save Custom Provider") }

                    OutlinedButton(
                        onClick = {
                            val next = if (settings.activeProvider == AiProviderType.GEMINI_LIVE) {
                                AiProviderType.CUSTOM_OPENAI_COMPAT
                            } else {
                                AiProviderType.GEMINI_LIVE
                            }
                            viewModel.updateSettings { it.copy(activeProvider = next) }
                        },
                        border = BorderStroke(1.dp, XavionBorderBright)
                    ) {
                        Text(
                            if (settings.activeProvider == AiProviderType.GEMINI_LIVE) "Switch to Custom" else "Switch to Gemini",
                            color = XavionCyan
                        )
                    }
                }
            }
        }

        // Security Architecture Note
        item {
            HoloPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = XavionCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Credential Security Architecture", style = MaterialTheme.typography.titleMedium, color = XavionCyan)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Personal Testing: Manually entered API keys are encrypted locally using an AES-256-GCM key stored inside Android Keystore hardware/TEE and never written to Logcat.\n" +
                        "• Screenshot Protection: Keep FLAG_SECURE enabled in About/Security so API keys cannot be captured in screenshots.\n" +
                        "• Production Deployment: Never bundle a permanent API key inside a distributed APK. For public releases, issue short-lived ephemeral tokens from your protected backend server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = XavionTextSecondary
                )
            }
        }
    }
}
