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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MemoryRecordEntity
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.theme.XavionBlack
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.viewmodel.XavionViewModel

@Composable
fun MemoryScreen(viewModel: XavionViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()

    var editingRecord by remember { mutableStateOf<MemoryRecordEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showAddDialog || editingRecord != null) {
        MemoryEditorDialog(
            existing = editingRecord,
            onDismiss = {
                showAddDialog = false
                editingRecord = null
            },
            onSave = { id, cat, title, content, pinned ->
                viewModel.addOrUpdateMemory(id, cat, title, content, pinned)
                showAddDialog = false
                editingRecord = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = XavionPanelDark,
            title = { Text("Clear All Memories?", color = XavionCrimson) },
            text = { Text("This permanently deletes all saved preferences, facts, and custom memory records.", color = XavionTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMemories()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XavionCrimson, contentColor = XavionTextPrimary)
                ) { Text("Clear All") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) { Text("Cancel", color = XavionCyan) }
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
                    title = "Memory & Personalization",
                    subtitle = "${memories.size} stored memory records"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inject Persistent Memory into AI", style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                        Text("When enabled, XAVION remembers your preferences and saved facts across sessions.", style = MaterialTheme.typography.bodyMedium, color = XavionTextSecondary)
                    }
                    Switch(
                        checked = settings.memoryEnabled,
                        onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(memoryEnabled = enabled) } },
                        modifier = Modifier.testTag("memory_enabled_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = XavionCyan)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("memory_add_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Memory")
                    }
                    OutlinedButton(
                        onClick = { showClearConfirm = true },
                        enabled = memories.isNotEmpty(),
                        border = BorderStroke(1.dp, XavionCrimson)
                    ) {
                        Text("Clear Memory", color = XavionCrimson)
                    }
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                HoloPanel {
                    Text(
                        "No memories stored yet. Add user preferences, facts, or standing instructions—or ask XAVION to remember something in conversation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XavionTextMuted
                    )
                }
            }
        } else {
            items(memories, key = { it.id }) { mem ->
                HoloPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (mem.pinned) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = XavionCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Column {
                                Text(mem.title, style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                                Text(mem.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = XavionCyan)
                            }
                        }
                        Row {
                            IconButton(onClick = { editingRecord = mem }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit memory", tint = XavionCyan)
                            }
                            IconButton(onClick = { viewModel.deleteMemory(mem.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = XavionCrimson)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mem.content, style = MaterialTheme.typography.bodyMedium, color = XavionTextSecondary)
                }
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(
                    title = "Conversation History",
                    subtitle = "${conversations.size} saved threads",
                    trailing = {
                        if (conversations.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.clearAllConversations() },
                                border = BorderStroke(1.dp, XavionCrimson)
                            ) {
                                Text("Clear History", color = XavionCrimson, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (conversations.isEmpty()) {
                    Text("No saved conversations.", style = MaterialTheme.typography.bodyMedium, color = XavionTextMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        conversations.forEach { conv ->
                            Surface(
                                color = XavionPanelDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, XavionBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(conv.title, style = MaterialTheme.typography.bodyLarge, color = XavionTextPrimary)
                                        Text(conv.mode, style = MaterialTheme.typography.labelSmall, color = XavionCyan)
                                    }
                                    IconButton(onClick = { viewModel.deleteConversation(conv.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete conversation", tint = XavionCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryEditorDialog(
    existing: MemoryRecordEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Boolean) -> Unit
) {
    var category by remember { mutableStateOf(existing?.category ?: "Preference") }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }
    var pinned by remember { mutableStateOf(existing?.pinned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = XavionPanelDark,
        title = { Text(if (existing == null) "Add Memory" else "Edit Memory", color = XavionCyan) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Preference", "Fact", "Instruction").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details to remember") },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                    Text("Pin high priority", color = XavionTextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(existing?.id ?: 0L, category, title.ifBlank { category }, content, pinned) },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
            ) { Text("Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel", color = XavionTextSecondary) }
        }
    )
}
