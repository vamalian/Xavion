package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.XavionApplication
import com.example.ai.ConnectivityTestResult
import com.example.ai.LiveTranscriptEvent
import com.example.ai.SystemPromptBuilder
import com.example.commands.CalendarAgendaItem
import com.example.commands.InstalledAppInfo
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryRecordEntity
import com.example.data.local.NoteEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.ToolExecutionLogEntity
import com.example.data.model.AiProviderType
import com.example.data.model.AvatarMode
import com.example.data.model.DeviceTelemetry
import com.example.data.model.LiveConnectionState
import com.example.data.model.WeatherInfo
import com.example.data.model.XavionSettings
import com.example.service.ActiveNotificationItem
import com.example.service.XavionAssistantService
import com.example.service.XavionNotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface AsyncActionState {
    data object Idle : AsyncActionState
    data object Loading : AsyncActionState
    data class Success(val message: String) : AsyncActionState
    data class Error(val message: String) : AsyncActionState
}

@OptIn(ExperimentalCoroutinesApi::class)
class XavionViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as XavionApplication).container
    private val settingsRepo = container.settingsRepository
    private val vault = container.keystoreVault
    private val conversationRepo = container.conversationRepository
    private val memoryRepo = container.memoryRepository
    private val productivityRepo = container.productivityRepository
    private val commandExecutor = container.commandExecutor
    private val audioEngine = container.audioEngine
    private val liveClient = container.liveClient
    private val restClient = container.restClient
    private val weatherRepo = container.weatherRepository
    val wakeWordEngine = container.wakeWordEngine
    val floatingBubbleManager = container.floatingBubbleManager

    val settings: StateFlow<XavionSettings> = settingsRepo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        XavionSettings()
    )

    val hasApiKey: StateFlow<Boolean> = vault.hasGeminiApiKeyFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    val maskedApiKey: StateFlow<String> = vault.maskedGeminiApiKeyFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ""
    )

    val hasPinConfigured: StateFlow<Boolean> = vault.hasPinConfiguredFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        false
    )

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _lockError = MutableStateFlow<String?>(null)
    val lockError: StateFlow<String?> = _lockError.asStateFlow()

    val liveState: StateFlow<LiveConnectionState> = liveClient.connectionState
    val liveUserCaption: StateFlow<String> = liveClient.liveUserCaption
    val liveModelCaption: StateFlow<String> = liveClient.liveModelCaption
    val inputAmplitude: StateFlow<Float> = audioEngine.inputAmplitude
    val outputAmplitude: StateFlow<Float> = audioEngine.outputAmplitude
    val isMicActive: StateFlow<Boolean> = audioEngine.isMicActive
    val isMuted: StateFlow<Boolean> = audioEngine.isMuted
    val isVoiceDetected: StateFlow<Boolean> = audioEngine.isVoiceDetected

    val pendingConfirmation = commandExecutor.pendingConfirmation
    val flashlightOn = commandExecutor.flashlightOn

    val conversations: StateFlow<List<ConversationEntity>> =
        conversationRepo.conversations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentConversations: StateFlow<List<ConversationEntity>> =
        conversationRepo.recentConversations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    val activeMessages: StateFlow<List<ChatMessageEntity>> = _activeConversationId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else conversationRepo.messages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryRecordEntity>> =
        memoryRepo.memories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskItemEntity>> =
        productivityRepo.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> =
        productivityRepo.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val toolLogs: StateFlow<List<ToolExecutionLogEntity>> =
        productivityRepo.toolLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotifications: StateFlow<List<ActiveNotificationItem>> = XavionNotificationListenerService.notifications
    val notificationListenerConnected: StateFlow<Boolean> = XavionNotificationListenerService.isConnected

    val wakeWordListening: StateFlow<Boolean> = wakeWordEngine.isListening
    val wakeWordStatus: StateFlow<String> = wakeWordEngine.statusMessage
    val serviceRunning: StateFlow<Boolean> = XavionAssistantService.isRunning
    val bubbleShowing: StateFlow<Boolean> = floatingBubbleManager.isShowing

    private val _chatState = MutableStateFlow<AsyncActionState>(AsyncActionState.Idle)
    val chatState: StateFlow<AsyncActionState> = _chatState.asStateFlow()

    private val _connectivityResult = MutableStateFlow<ConnectivityTestResult?>(null)
    val connectivityResult: StateFlow<ConnectivityTestResult?> = _connectivityResult.asStateFlow()

    private val _isTestingConnectivity = MutableStateFlow(false)
    val isTestingConnectivity: StateFlow<Boolean> = _isTestingConnectivity.asStateFlow()

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError: StateFlow<String?> = _weatherError.asStateFlow()

    private val _deviceTelemetry = MutableStateFlow(commandExecutor.readDeviceTelemetry())
    val deviceTelemetry: StateFlow<DeviceTelemetry> = _deviceTelemetry.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _calendarAgenda = MutableStateFlow<List<CalendarAgendaItem>>(emptyList())
    val calendarAgenda: StateFlow<List<CalendarAgendaItem>> = _calendarAgenda.asStateFlow()

    private val _liveToolFeed = MutableStateFlow<List<String>>(emptyList())
    val liveToolFeed: StateFlow<List<String>> = _liveToolFeed.asStateFlow()

    private val _visionAnalysisState = MutableStateFlow<AsyncActionState>(AsyncActionState.Idle)
    val visionAnalysisState: StateFlow<AsyncActionState> = _visionAnalysisState.asStateFlow()

    private val _visionOutputText = MutableStateFlow("")
    val visionOutputText: StateFlow<String> = _visionOutputText.asStateFlow()

    private val _dailyBriefing = MutableStateFlow("")
    val dailyBriefing: StateFlow<String> = _dailyBriefing.asStateFlow()

    private val _briefingLoading = MutableStateFlow(false)
    val briefingLoading: StateFlow<Boolean> = _briefingLoading.asStateFlow()

    private var liveConversationId: Long? = null

    init {
        viewModelScope.launch {
            val s = settingsRepo.current()
            val hasPin = vault.hasPinConfiguredFlow.first()
            if (s.appLockEnabled && (hasPin || s.biometricUnlockEnabled)) {
                _isLocked.value = true
            }
            refreshWeather()
            refreshInstalledApps()
            refreshCalendarAgenda()
        }

        // Periodic device telemetry refresh
        viewModelScope.launch {
            while (isActive) {
                _deviceTelemetry.value = commandExecutor.readDeviceTelemetry()
                delay(5000)
            }
        }

        // Persist Gemini Live transcripts into Room conversation log
        viewModelScope.launch {
            liveClient.transcriptEvents.collect { event ->
                when (event) {
                    is LiveTranscriptEvent.UserTranscript -> {
                        val convId = ensureLiveConversationId()
                        conversationRepo.addMessage(convId, "USER", event.text)
                    }
                    is LiveTranscriptEvent.ModelTranscript -> {
                        val convId = ensureLiveConversationId()
                        conversationRepo.addMessage(convId, "XAVION", event.text)
                    }
                    is LiveTranscriptEvent.ToolEvent -> {
                        _liveToolFeed.value = (listOf(event.summary) + _liveToolFeed.value).take(8)
                    }
                }
            }
        }
    }

    private suspend fun ensureLiveConversationId(): Long {
        liveConversationId?.let { return it }
        val stamp = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
        val created = conversationRepo.createConversation("Live Session • $stamp", "LIVE_VOICE")
        liveConversationId = created
        if (_activeConversationId.value == null) {
            _activeConversationId.value = created
        }
        return created
    }

    fun startOrToggleLiveSession() {
        when (liveState.value) {
            is LiveConnectionState.ConnectedIdle,
            is LiveConnectionState.Listening,
            is LiveConnectionState.Speaking,
            is LiveConnectionState.Thinking,
            is LiveConnectionState.Connecting,
            is LiveConnectionState.Reconnecting -> {
                disconnectLiveSession()
            }
            is LiveConnectionState.Disconnected,
            is LiveConnectionState.Error -> {
                connectLiveSession()
            }
        }
    }

    fun connectLiveSession() {
        viewModelScope.launch {
            wakeWordEngine.stop() // Prevent microphone contention
            val s = settingsRepo.current()
            val key = if (s.activeProvider == AiProviderType.CUSTOM_OPENAI_COMPAT) {
                vault.getCustomProviderKey().orEmpty()
            } else {
                vault.getGeminiApiKey().orEmpty()
            }
            if (key.isBlank()) {
                _chatState.value = AsyncActionState.Error("Please add your Gemini API key in API Configuration first.")
                liveClient.connect("", s, "")
                return@launch
            }
            liveConversationId = null
            val mems = if (s.memoryEnabled) memoryRepo.getAllOnce() else emptyList()
            val sysPrompt = SystemPromptBuilder.build(s, mems)
            liveClient.connect(key, s, sysPrompt, autoStartMic = true)
        }
    }

    fun disconnectLiveSession() {
        liveClient.disconnect()
        viewModelScope.launch {
            if (settingsRepo.current().wakeWordEnabled && serviceRunning.value) {
                wakeWordEngine.start()
            }
        }
    }

    fun toggleLiveMic() {
        liveClient.toggleMic()
    }

    fun interruptLiveResponse() {
        liveClient.interruptResponse()
    }

    fun sendLiveTextTurn(text: String) {
        if (text.isBlank()) return
        liveClient.sendTextTurn(text.trim())
    }

    fun sendVisionFrameToLive(jpegBytes: ByteArray) {
        liveClient.sendVisionFrame(jpegBytes)
    }

    fun openConversation(id: Long) {
        _activeConversationId.value = id
    }

    fun startNewTextConversation() {
        viewModelScope.launch {
            val stamp = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
            val id = conversationRepo.createConversation("New Session • $stamp", "TEXT_CHAT")
            _activeConversationId.value = id
        }
    }

    fun sendTextChatMessage(prompt: String, attachedImageUri: Uri? = null, speakAloud: Boolean = false) {
        val clean = prompt.trim()
        if (clean.isEmpty() && attachedImageUri == null) return

        viewModelScope.launch {
            _chatState.value = AsyncActionState.Loading
            try {
                val convId = _activeConversationId.value ?: run {
                    val stamp = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())
                    conversationRepo.createConversation("Chat • $stamp", "TEXT_CHAT").also {
                        _activeConversationId.value = it
                    }
                }
                val historyBefore = conversationRepo.messages(convId).first()
                val userDisplay = if (clean.isNotEmpty()) clean else "Analyze this image."
                conversationRepo.addMessage(convId, "USER", userDisplay, attachedImageUri?.toString())

                val s = settingsRepo.current()
                val key = if (s.activeProvider == AiProviderType.CUSTOM_OPENAI_COMPAT) {
                    vault.getCustomProviderKey().orEmpty()
                } else {
                    vault.getGeminiApiKey().orEmpty()
                }

                val mems = if (s.memoryEnabled) memoryRepo.getAllOnce() else emptyList()
                val sysPrompt = SystemPromptBuilder.build(s, mems)
                val imageB64 = attachedImageUri?.let { encodeUriToResizedJpegBase64(it) }

                val result = restClient.generateResponse(
                    apiKey = key,
                    settings = s,
                    systemInstruction = sysPrompt,
                    history = historyBefore,
                    userPrompt = userDisplay,
                    imageBase64Jpeg = imageB64
                )

                result.onSuccess { resp ->
                    conversationRepo.addMessage(convId, "XAVION", resp.text)
                    if (resp.executedTools.isNotEmpty()) {
                        _liveToolFeed.value = (resp.executedTools + _liveToolFeed.value).take(8)
                    }
                    if (speakAloud) {
                        audioEngine.speakTextTts(resp.text, s.speechSpeed)
                    }
                    _chatState.value = AsyncActionState.Success("Response received")
                }.onFailure { err ->
                    val errText = err.localizedMessage ?: "Failed to reach Gemini API."
                    conversationRepo.addMessage(convId, "SYSTEM", "Error: $errText")
                    _chatState.value = AsyncActionState.Error(errText)
                }
            } catch (e: Exception) {
                _chatState.value = AsyncActionState.Error(e.localizedMessage ?: "Unexpected chat error")
            }
        }
    }

    fun speakText(text: String) {
        viewModelScope.launch {
            audioEngine.speakTextTts(text, settingsRepo.current().speechSpeed)
        }
    }

    fun stopSpeakingTts() {
        audioEngine.stopTts()
    }

    fun analyzeVisionImage(jpegBytes: ByteArray, prompt: String, speakResult: Boolean = false) {
        viewModelScope.launch {
            _visionAnalysisState.value = AsyncActionState.Loading
            val s = settingsRepo.current()
            val key = vault.getGeminiApiKey().orEmpty()
            val mems = if (s.memoryEnabled) memoryRepo.getAllOnce() else emptyList()
            val sysPrompt = SystemPromptBuilder.build(s, mems)
            val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

            val result = restClient.generateResponse(
                apiKey = key,
                settings = s,
                systemInstruction = sysPrompt,
                history = emptyList(),
                userPrompt = prompt.ifBlank { "Identify objects, read any visible text (OCR), and summarize what you see clearly." },
                imageBase64Jpeg = b64
            )
            result.onSuccess { resp ->
                _visionOutputText.value = resp.text
                _visionAnalysisState.value = AsyncActionState.Success("Vision analysis complete")
                if (speakResult) {
                    audioEngine.speakTextTts(resp.text, s.speechSpeed)
                }
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Vision analysis failed"
                _visionOutputText.value = msg
                _visionAnalysisState.value = AsyncActionState.Error(msg)
            }
        }
    }

    fun analyzeVisionUri(uri: Uri, prompt: String, speakResult: Boolean = false) {
        viewModelScope.launch {
            _visionAnalysisState.value = AsyncActionState.Loading
            val b64 = encodeUriToResizedJpegBase64(uri)
            if (b64 == null) {
                _visionAnalysisState.value = AsyncActionState.Error("Unable to decode selected image.")
                return@launch
            }
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            analyzeVisionImage(bytes, prompt, speakResult)
        }
    }

    fun generateDailyBriefing(speakAloud: Boolean = false) {
        viewModelScope.launch {
            _briefingLoading.value = true
            val openTasks = productivityRepo.getOpenTasksOnce()
            val agenda = commandExecutor.readUpcomingCalendarEvents(5)
            _calendarAgenda.value = agenda
            val w = _weather.value
            val t = commandExecutor.readDeviceTelemetry()

            val contextSummary = buildString {
                appendLine("Generate a crisp, executive Daily Briefing for the user based ONLY on this verified device data:")
                appendLine("- Battery: ${t.batteryPercent}% (${if (t.isCharging) "Charging" else "Discharging"}), Network: ${t.networkType}")
                if (w != null) {
                    appendLine("- Weather in ${w.city}: ${w.temperatureCelsius}°C, ${w.conditionLabel}, Wind ${w.windKmh} km/h")
                } else {
                    appendLine("- Weather: Not yet fetched")
                }
                appendLine("- Open Tasks (${openTasks.size}): ${openTasks.take(6).joinToString("; ") { "${it.title} [${it.priority}]" }.ifBlank { "None" }}")
                appendLine("- Upcoming Calendar Events (${agenda.size}): ${agenda.joinToString("; ") { it.title }.ifBlank { "None or calendar permission not granted" }}")
            }

            val s = settingsRepo.current()
            val key = vault.getGeminiApiKey().orEmpty()
            if (key.isBlank()) {
                _dailyBriefing.value = "Good day, ${if (s.addressAsSir) s.ownerName else "Commander"}. Battery is at ${t.batteryPercent}% (${t.networkType}). You have ${openTasks.size} open task(s)" +
                    (w?.let { " and it is currently ${it.temperatureCelsius}°C (${it.conditionLabel}) in ${it.city}." } ?: ".") +
                    " Add a Gemini API key in API Configuration for full AI-synthesized briefings."
                _briefingLoading.value = false
                if (speakAloud) audioEngine.speakTextTts(_dailyBriefing.value, s.speechSpeed)
                return@launch
            }

            val mems = if (s.memoryEnabled) memoryRepo.getAllOnce() else emptyList()
            val sysPrompt = SystemPromptBuilder.build(s, mems)
            restClient.generateResponse(
                apiKey = key,
                settings = s,
                systemInstruction = sysPrompt,
                history = emptyList(),
                userPrompt = contextSummary
            ).onSuccess { resp ->
                _dailyBriefing.value = resp.text
                if (speakAloud) audioEngine.speakTextTts(resp.text, s.speechSpeed)
            }.onFailure { err ->
                _dailyBriefing.value = "Unable to generate AI briefing: ${err.localizedMessage}"
            }
            _briefingLoading.value = false
        }
    }

    fun saveApiKey(rawKey: String, isCustomProvider: Boolean = false) {
        viewModelScope.launch {
            if (isCustomProvider) {
                vault.saveCustomProviderKey(rawKey)
            } else {
                vault.saveGeminiApiKey(rawKey)
            }
            _connectivityResult.value = null
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            vault.clearGeminiApiKey()
            _connectivityResult.value = null
        }
    }

    fun testApiConnectivity() {
        viewModelScope.launch {
            _isTestingConnectivity.value = true
            val s = settingsRepo.current()
            val key = if (s.activeProvider == AiProviderType.CUSTOM_OPENAI_COMPAT) {
                vault.getCustomProviderKey().orEmpty()
            } else {
                vault.getGeminiApiKey().orEmpty()
            }
            _connectivityResult.value = restClient.testConnectivity(key, s)
            _isTestingConnectivity.value = false
        }
    }

    fun updateSettings(transform: (XavionSettings) -> XavionSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }

    fun cycleAvatarMode() {
        viewModelScope.launch {
            settingsRepo.update { current ->
                val modes = AvatarMode.entries
                val next = modes[(modes.indexOf(current.avatarMode) + 1) % modes.size]
                current.copy(avatarMode = next)
            }
        }
    }

    fun refreshWeather(cityOverride: String? = null) {
        viewModelScope.launch {
            val targetCity = cityOverride ?: settingsRepo.current().weatherCity
            weatherRepo.fetchWeatherForCity(targetCity)
                .onSuccess {
                    _weather.value = it
                    _weatherError.value = null
                }
                .onFailure {
                    _weatherError.value = it.localizedMessage
                }
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = commandExecutor.getInstalledLaunchableApps()
        }
    }

    fun refreshCalendarAgenda() {
        viewModelScope.launch(Dispatchers.IO) {
            _calendarAgenda.value = commandExecutor.readUpcomingCalendarEvents(8)
        }
    }

    fun resolveConfirmation(approved: Boolean) {
        commandExecutor.resolveConfirmation(approved)
    }

    fun runDirectCommand(functionName: String, args: JSONObject = JSONObject()) {
        viewModelScope.launch {
            commandExecutor.executeDirect(functionName, args)
            _deviceTelemetry.value = commandExecutor.readDeviceTelemetry()
        }
    }

    fun launchInstalledApp(packageName: String) {
        commandExecutor.launchAppByPackage(packageName)
    }

    // Memory & Productivity CRUD
    fun addOrUpdateMemory(id: Long = 0, category: String, title: String, content: String, pinned: Boolean) {
        viewModelScope.launch {
            if (id == 0L) {
                memoryRepo.addOrSave(category, title, content, pinned)
            } else {
                memoryRepo.update(MemoryRecordEntity(id, category, title, content, pinned))
            }
        }
    }

    fun deleteMemory(id: Long) = viewModelScope.launch { memoryRepo.delete(id) }
    fun clearAllMemories() = viewModelScope.launch { memoryRepo.clearAll() }
    fun deleteConversation(id: Long) = viewModelScope.launch {
        if (_activeConversationId.value == id) _activeConversationId.value = null
        conversationRepo.deleteConversation(id)
    }
    fun clearAllConversations() = viewModelScope.launch {
        _activeConversationId.value = null
        conversationRepo.clearAll()
    }

    fun addTask(title: String, details: String, dueLabel: String, priority: String, isReminder: Boolean) {
        viewModelScope.launch { productivityRepo.addTask(title, details, dueLabel, priority, isReminder) }
    }
    fun toggleTask(task: TaskItemEntity) = viewModelScope.launch { productivityRepo.toggleTask(task) }
    fun deleteTask(id: Long) = viewModelScope.launch { productivityRepo.deleteTask(id) }

    fun addOrUpdateNote(id: Long = 0, title: String, content: String, tag: String) {
        viewModelScope.launch {
            if (id == 0L) productivityRepo.addNote(title, content, tag)
            else productivityRepo.updateNote(NoteEntity(id, title, content, tag))
        }
    }
    fun deleteNote(id: Long) = viewModelScope.launch { productivityRepo.deleteNote(id) }

    // Background Service, Wake Word & Floating Bubble
    fun startForegroundAssistant() {
        XavionAssistantService.start(getApplication())
    }

    fun stopForegroundAssistant() {
        XavionAssistantService.stop(getApplication())
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(wakeWordEnabled = enabled) }
            if (enabled) {
                XavionAssistantService.start(getApplication())
                wakeWordEngine.start()
            } else {
                wakeWordEngine.stop()
            }
        }
    }

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(floatingBubbleEnabled = enabled) }
            if (enabled && floatingBubbleManager.hasOverlayPermission()) {
                XavionAssistantService.start(getApplication())
                floatingBubbleManager.show()
            } else {
                floatingBubbleManager.hide()
            }
        }
    }

    // Security & Owner Lock
    fun unlockWithBiometric() {
        _isLocked.value = false
        _lockError.value = null
    }

    fun verifyAndUnlockPin(pin: String) {
        viewModelScope.launch {
            if (vault.verifyPin(pin)) {
                _isLocked.value = false
                _lockError.value = null
            } else {
                _lockError.value = "Incorrect PIN. Please try again."
            }
        }
    }

    fun setOwnerPin(pin: String) {
        viewModelScope.launch {
            vault.setPin(pin)
            settingsRepo.update { it.copy(appLockEnabled = true) }
        }
    }

    fun clearOwnerPin() {
        viewModelScope.launch {
            vault.clearPin()
            settingsRepo.update { it.copy(appLockEnabled = false) }
        }
    }

    fun wipeAllAppData() {
        viewModelScope.launch {
            liveClient.disconnect()
            XavionAssistantService.stop(getApplication())
            conversationRepo.clearAll()
            memoryRepo.clearAll()
            productivityRepo.clearToolLogs()
            vault.wipeAllSecrets()
            settingsRepo.resetAll()
            _isLocked.value = false
        }
    }

    private suspend fun encodeUriToResizedJpegBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = getApplication<Application>().contentResolver
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@runCatching null

            val maxDim = 1024
            val scale = minOf(
                1f,
                maxDim.toFloat() / decoded.width.coerceAtLeast(1),
                maxDim.toFloat() / decoded.height.coerceAtLeast(1)
            )
            val target = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                decoded
            }

            val baos = ByteArrayOutputStream()
            target.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            if (target !== decoded) target.recycle()
            decoded.recycle()
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }
}
