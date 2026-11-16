package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.theme.XavionBorderBright
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCobalt
import com.example.ui.theme.XavionCrimson
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionEmerald
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionPanelElevated
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTextSecondary
import com.example.ui.viewmodel.AsyncActionState
import com.example.ui.viewmodel.XavionViewModel

@Composable
fun TextChatScreen(viewModel: XavionViewModel) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val chatState by viewModel.chatState.collectAsStateWithLifecycle()

    var input by remember { mutableStateOf("") }
    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    var speakReplies by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) attachedUri = uri
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isWideTablet = maxWidth >= 780.dp

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isWideTablet) {
                HoloPanel(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                ) {
                    HoloSectionHeader(
                        title = "Sessions",
                        trailing = {
                            IconButton(
                                onClick = { viewModel.startNewTextConversation() },
                                modifier = Modifier.testTag("chat_new_session_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "New session", tint = XavionCyan)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ConversationSidebarList(
                        conversations = conversations,
                        activeId = activeId,
                        onSelect = { viewModel.openConversation(it) },
                        onDelete = { viewModel.deleteConversation(it) }
                    )
                }
            }

            HoloPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                borderColor = XavionBorderBright
            ) {
                HoloSectionHeader(
                    title = "XAVION Text Interface",
                    subtitle = "Multimodal Gemini REST + Device Tools",
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = speakReplies,
                                onClick = { speakReplies = !speakReplies },
                                label = { Text("Speak", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                            if (!isWideTablet) {
                                IconButton(onClick = { viewModel.startNewTextConversation() }) {
                                    Icon(Icons.Default.Add, contentDescription = "New chat", tint = XavionCyan)
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Productivity & Utility Prompt Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPrompts = listOf(
                        "Summarize" to "Summarize the following text concisely:\n",
                        "Translate EN↔UR" to "Translate naturally between English and Urdu:\n",
                        "Calculate" to "Calculate and explain step by step:\n",
                        "Document Help" to "Review and structure this document:\n"
                    )
                    quickPrompts.forEach { (label, prefix) ->
                        AssistChip(
                            onClick = { input = prefix },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = XavionPanelDark,
                                labelColor = XavionCyan
                            ),
                            border = BorderStroke(1.dp, XavionBorderSubtle)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Surface(
                                color = XavionPanelDark,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, XavionBorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Good day, Sir. XAVION systems are online. How may I assist you?",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = XavionCyan
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Ask questions in English or Urdu, attach an image for analysis, request calculations/summaries, or issue device commands.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = XavionTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            ChatBubble(
                                message = msg,
                                onSpeak = { viewModel.speakText(msg.content) }
                            )
                        }
                    }
                }

                if (chatState is AsyncActionState.Loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = XavionCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("XAVION is reasoning...", style = MaterialTheme.typography.labelMedium, color = XavionCyan)
                    }
                }

                attachedUri?.let { uri ->
                    Surface(
                        color = XavionPanelDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, XavionBorderBright),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Attached image",
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Image attached", style = MaterialTheme.typography.labelMedium, color = XavionTextPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { attachedUri = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove attachment", tint = XavionCrimson)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_attach_image_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach image", tint = XavionCyan)
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Message XAVION (English / Urdu)...") },
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XavionCyan,
                            unfocusedBorderColor = XavionBorderSubtle
                        )
                    )

                    IconButton(
                        onClick = {
                            val prompt = input
                            val uri = attachedUri
                            input = ""
                            attachedUri = null
                            viewModel.sendTextChatMessage(prompt, uri, speakReplies)
                        },
                        enabled = (input.isNotBlank() || attachedUri != null) && chatState !is AsyncActionState.Loading,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = XavionCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationSidebarList(
    conversations: List<ConversationEntity>,
    activeId: Long?,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (conversations.isEmpty()) {
        Text("No conversations yet.", style = MaterialTheme.typography.bodyMedium, color = XavionTextMuted)
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(conversations, key = { it.id }) { conv ->
            val selected = conv.id == activeId
            Surface(
                color = if (selected) XavionPanelElevated else XavionPanelDark,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (selected) XavionCyan else XavionBorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(conv.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conv.title,
                            style = MaterialTheme.typography.bodyMedium,
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
                    IconButton(
                        onClick = { onDelete(conv.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete conversation", tint = XavionTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == "USER"
    val isError = message.sender == "SYSTEM"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = when {
                isUser -> XavionCobalt.copy(alpha = 0.25f)
                isError -> XavionCrimson.copy(alpha = 0.18f)
                else -> XavionPanelDark
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                1.dp,
                when {
                    isUser -> XavionCobalt
                    isError -> XavionCrimson
                    else -> XavionBorderSubtle
                }
            ),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) XavionEmerald else if (isError) XavionCrimson else XavionCyan
                    )
                    if (!isUser && !isError) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Speak response",
                                tint = XavionCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                if (!message.imageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "User attached photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = XavionTextPrimary
                )
            }
        }
    }
}
