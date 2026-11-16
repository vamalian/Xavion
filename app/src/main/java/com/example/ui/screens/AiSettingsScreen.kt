package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AiProviderType
import com.example.data.model.AssistantLanguage
import com.example.data.model.AvatarMode
import com.example.data.model.GeminiLiveModelOption
import com.example.data.model.GeminiRestModelOption
import com.example.data.model.PersonalityPreset
import com.example.data.model.VerbosityLevel
import com.example.data.model.XavionVoiceOption
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.viewmodel.XavionViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSettingsScreen(viewModel: XavionViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var customLiveModel by remember(settings.liveModelId) { mutableStateOf(settings.liveModelId) }
    var customInstructions by remember(settings.customSystemInstructions) { mutableStateOf(settings.customSystemInstructions) }
    var ownerName by remember(settings.ownerName) { mutableStateOf(settings.ownerName) }
    var weatherCity by remember(settings.weatherCity) { mutableStateOf(settings.weatherCity) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(title = "Gemini Models & Provider")
                Spacer(modifier = Modifier.height(10.dp))

                Text("Active AI Provider Interface", style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiProviderType.entries.forEach { provider ->
                        FilterChip(
                            selected = settings.activeProvider == provider,
                            onClick = { viewModel.updateSettings { it.copy(activeProvider = provider) } },
                            label = { Text(provider.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Gemini Live Real-Time Voice Model", style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GeminiLiveModelOption.entries.forEach { opt ->
                        FilterChip(
                            selected = settings.liveModelId == opt.modelId,
                            onClick = {
                                customLiveModel = opt.modelId
                                viewModel.updateSettings { it.copy(liveModelId = opt.modelId) }
                            },
                            label = { Text(opt.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customLiveModel,
                        onValueChange = { customLiveModel = it },
                        label = { Text("Custom Live Model ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    Button(
                        onClick = { viewModel.updateSettings { it.copy(liveModelId = customLiveModel.trim()) } },
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) { Text("Apply") }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Text & Vision REST Model", style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GeminiRestModelOption.entries.forEach { opt ->
                        FilterChip(
                            selected = settings.restModelId == opt.modelId,
                            onClick = { viewModel.updateSettings { it.copy(restModelId = opt.modelId) } },
                            label = { Text(opt.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(title = "Voice & Acoustic Settings")
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    XavionVoiceOption.entries.forEach { voice ->
                        FilterChip(
                            selected = settings.voiceName == voice.voiceName,
                            onClick = { viewModel.updateSettings { it.copy(voiceName = voice.voiceName) } },
                            label = { Text(voice.voiceName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                val selectedVoiceDesc = XavionVoiceOption.entries.firstOrNull { it.voiceName == settings.voiceName }?.description.orEmpty()
                if (selectedVoiceDesc.isNotBlank()) {
                    Text(selectedVoiceDesc, style = MaterialTheme.typography.labelSmall, color = XavionCyan)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("TTS Playback Speed: ${"%.2f".format(settings.speechSpeed)}x", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = settings.speechSpeed,
                    onValueChange = { spd -> viewModel.updateSettings { it.copy(speechSpeed = spd) } },
                    valueRange = 0.75f..1.4f,
                    colors = SliderDefaults.colors(thumbColor = XavionCyan, activeTrackColor = XavionCyan)
                )

                Text("Response Verbosity", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VerbosityLevel.entries.forEach { v ->
                        FilterChip(
                            selected = settings.verbosity == v,
                            onClick = { viewModel.updateSettings { it.copy(verbosity = v) } },
                            label = { Text(v.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(title = "Personality, Language & Instructions")
                Spacer(modifier = Modifier.height(10.dp))

                Text("Language Support", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = settings.language == lang,
                            onClick = { viewModel.updateSettings { it.copy(language = lang) } },
                            label = { Text(lang.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Persona Preset", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PersonalityPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.personalityPreset == preset,
                            onClick = { viewModel.updateSettings { it.copy(personalityPreset = preset) } },
                            label = { Text(preset.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Text(settings.personalityPreset.description, style = MaterialTheme.typography.bodyMedium, color = XavionTextSecondary)

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Address Owner as '${settings.ownerName.ifBlank { "Sir" }}'", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = settings.addressAsSir,
                        onCheckedChange = { checked -> viewModel.updateSettings { it.copy(addressAsSir = checked) } },
                        modifier = Modifier.testTag("settings_address_sir_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Honorific / Owner Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    Button(
                        onClick = { viewModel.updateSettings { it.copy(ownerName = ownerName.ifBlank { "Sir" }) } },
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) { Text("Save") }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customInstructions,
                    onValueChange = { customInstructions = it },
                    label = { Text("Custom System Instructions") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_system_instructions"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.updateSettings { it.copy(customSystemInstructions = customInstructions.trim()) } },
                    modifier = Modifier.testTag("settings_save_instructions"),
                    colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                ) { Text("Save System Instructions") }
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(title = "Interface, Memory & Safety")
                Spacer(modifier = Modifier.height(10.dp))

                Text("Visual Avatar Mode", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AvatarMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.avatarMode == mode,
                            onClick = { viewModel.updateSettings { it.copy(avatarMode = mode) } },
                            label = { Text(mode.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Enable Persistent Memory",
                    subtitle = "Include saved preferences & facts in Gemini context",
                    checked = settings.memoryEnabled,
                    onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(memoryEnabled = enabled) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Confirm Sensitive Device Actions",
                    subtitle = "Prompt before opening URLs, launching apps, or sharing text",
                    checked = settings.requireConfirmationForSensitiveActions,
                    onCheckedChange = { req -> viewModel.updateSettings { it.copy(requireConfirmationForSensitiveActions = req) } }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weatherCity,
                        onValueChange = { weatherCity = it },
                        label = { Text("Weather City") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    Button(
                        onClick = {
                            val city = weatherCity.trim().ifBlank { "San Francisco" }
                            viewModel.updateSettings { it.copy(weatherCity = city) }
                            viewModel.refreshWeather(city)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) { Text("Update") }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = XavionTextMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
            )
        }
    }
}
