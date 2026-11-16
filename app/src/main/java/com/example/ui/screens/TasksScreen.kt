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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.HoloPanel
import com.example.ui.components.HoloSectionHeader
import com.example.ui.theme.XavionAmber
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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(viewModel: XavionViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val agenda by viewModel.calendarAgenda.collectAsStateWithLifecycle()
    val briefing by viewModel.dailyBriefing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tasks", "Notes", "Alarms & Timers", "Calendar")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = XavionPanelDark,
            contentColor = XavionCyan,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = XavionCyan
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        when (selectedTab) {
            0 -> TasksAndRemindersTab(tasks = tasks, viewModel = viewModel)
            1 -> NotesTab(notes = notes, viewModel = viewModel)
            2 -> AlarmsAndTimersTab(viewModel = viewModel)
            3 -> CalendarAndBriefingTab(agenda = agenda, briefing = briefing, viewModel = viewModel)
        }
    }
}

@Composable
private fun TasksAndRemindersTab(
    tasks: List<com.example.data.local.TaskItemEntity>,
    viewModel: XavionViewModel
) {
    var title by remember { mutableStateOf("") }
    var dueLabel by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var isReminder by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(title = "New Task or Reminder")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Reminder title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasks_new_title"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueLabel,
                    onValueChange = { dueLabel = it },
                    label = { Text("Due time (e.g., Today 18:00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Low", "Normal", "High").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    FilterChip(
                        selected = isReminder,
                        onClick = { isReminder = !isReminder },
                        label = { Text("Reminder", style = MaterialTheme.typography.labelSmall) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addTask(title.trim(), "", dueLabel.trim(), priority, isReminder)
                        title = ""
                        dueLabel = ""
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("tasks_add_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Item")
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                HoloPanel {
                    Text("No tasks or reminders yet.", style = MaterialTheme.typography.bodyMedium, color = XavionTextMuted)
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                HoloPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { viewModel.toggleTask(task) },
                            colors = CheckboxDefaults.colors(checkedColor = XavionCyan)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (task.isCompleted) XavionTextMuted else XavionTextPrimary,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = listOfNotNull(
                                    if (task.isReminder) "Reminder" else "Task",
                                    task.priority,
                                    task.dueLabel.takeIf { it.isNotBlank() }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.priority == "High") XavionAmber else XavionCyan
                            )
                        }
                        IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = XavionCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesTab(
    notes: List<com.example.data.local.NoteEntity>,
    viewModel: XavionViewModel
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("General") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(title = "Create Note")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note content") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("General", "Work", "Personal", "AI").forEach { t ->
                        FilterChip(
                            selected = tag == t,
                            onClick = { tag = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addOrUpdateNote(0L, title.ifBlank { "Untitled Note" }, content, tag)
                        title = ""
                        content = ""
                    },
                    enabled = content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                ) {
                    Text("Save Note")
                }
            }
        }

        items(notes, key = { it.id }) { note ->
            HoloPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(note.title, style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                        Text(note.tag, style = MaterialTheme.typography.labelSmall, color = XavionCyan)
                    }
                    IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete note", tint = XavionCrimson)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(note.content, style = MaterialTheme.typography.bodyMedium, color = XavionTextSecondary)
            }
        }
    }
}

@Composable
private fun AlarmsAndTimersTab(viewModel: XavionViewModel) {
    var alarmHour by remember { mutableStateOf("07") }
    var alarmMinute by remember { mutableStateOf("30") }
    var alarmLabel by remember { mutableStateOf("Morning Briefing") }
    var timerMinutes by remember { mutableStateOf("5") }
    var timerLabel by remember { mutableStateOf("Focus Sprint") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(title = "Android Alarm Clock")
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alarmHour,
                        onValueChange = { alarmHour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Hour (0-23)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    OutlinedTextField(
                        value = alarmMinute,
                        onValueChange = { alarmMinute = it.filter(Char::isDigit).take(2) },
                        label = { Text("Min (0-59)") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = { alarmLabel = it },
                    label = { Text("Alarm label") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.runDirectCommand(
                            "set_alarm",
                            JSONObject()
                                .put("hour", alarmHour.toIntOrNull() ?: 7)
                                .put("minute", alarmMinute.toIntOrNull() ?: 30)
                                .put("label", alarmLabel)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set System Alarm")
                }
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(title = "Countdown Timer")
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timerMinutes,
                        onValueChange = { timerMinutes = it.filter(Char::isDigit).take(3) },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    OutlinedTextField(
                        value = timerLabel,
                        onValueChange = { timerLabel = it },
                        label = { Text("Label") },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val secs = (timerMinutes.toIntOrNull() ?: 5) * 60
                        viewModel.runDirectCommand(
                            "set_timer",
                            JSONObject().put("seconds", secs).put("label", timerLabel)
                        )
                    },
                    border = BorderStroke(1.dp, XavionBorderBright)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = XavionCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Timer", color = XavionCyan)
                }
            }
        }
    }
}

@Composable
private fun CalendarAndBriefingTab(
    agenda: List<com.example.commands.CalendarAgendaItem>,
    briefing: String,
    viewModel: XavionViewModel
) {
    var eventTitle by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HoloPanel(borderColor = XavionBorderBright) {
                HoloSectionHeader(
                    title = "Calendar Agenda",
                    trailing = {
                        OutlinedButton(
                            onClick = { viewModel.refreshCalendarAgenda() },
                            border = BorderStroke(1.dp, XavionBorderSubtle)
                        ) {
                            Text("Sync", color = XavionCyan, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (agenda.isEmpty()) {
                    Text(
                        "No upcoming events found or Calendar Read permission is not granted (check Permissions).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XavionTextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        agenda.forEach { ev ->
                            Surface(
                                color = XavionPanelDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, XavionBorderSubtle)
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = XavionCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(ev.title, style = MaterialTheme.typography.titleMedium, color = XavionTextPrimary)
                                        Text(
                                            SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault()).format(Date(ev.startTimeMillis)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = XavionCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Quick Event Title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = XavionCyan, unfocusedBorderColor = XavionBorderSubtle)
                    )
                    Button(
                        onClick = {
                            viewModel.runDirectCommand(
                                "create_calendar_event",
                                JSONObject().put("title", eventTitle).put("minutes_from_now", 60)
                            )
                            eventTitle = ""
                        },
                        enabled = eventTitle.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                    ) { Text("Schedule") }
                }
            }
        }

        item {
            HoloPanel {
                HoloSectionHeader(
                    title = "Executive Daily Briefing",
                    trailing = {
                        Button(
                            onClick = { viewModel.generateDailyBriefing(speakAloud = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = XavionCyan, contentColor = XavionBlack)
                        ) { Text("Generate & Speak") }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = briefing.ifBlank { "Tap 'Generate & Speak' to create your personalized daily summary." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (briefing.isBlank()) XavionTextMuted else XavionTextPrimary
                )
            }
        }
    }
}
