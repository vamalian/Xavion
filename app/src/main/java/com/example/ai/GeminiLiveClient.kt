package com.example.ai

import android.util.Base64
import com.example.commands.DeviceCommandExecutor
import com.example.commands.DeviceCommandRegistry
import com.example.data.model.LiveConnectionState
import com.example.data.model.XavionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

sealed interface LiveTranscriptEvent {
    data class UserTranscript(val text: String, val isFinal: Boolean) : LiveTranscriptEvent
    data class ModelTranscript(val text: String, val isFinal: Boolean) : LiveTranscriptEvent
    data class ToolEvent(val summary: String) : LiveTranscriptEvent
}

/**
 * Real-time Gemini Live Bidirectional Streaming client over WebSocket (BidiGenerateContent).
 * Supports 16kHz PCM16 audio input, 24kHz PCM16 native audio output, vision frame injection,
 * voice interruption (barge-in), function calling, and automatic reconnection.
 */
class GeminiLiveClient(
    private val okHttpClient: OkHttpClient,
    private val audioEngine: XavionAudioEngine,
    private val commandExecutor: DeviceCommandExecutor
) {
    companion object {
        private const val WS_ENDPOINT =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
        private const val MAX_RECONNECT_ATTEMPTS = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<LiveConnectionState>(LiveConnectionState.Disconnected)
    val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

    private val _liveUserCaption = MutableStateFlow("")
    val liveUserCaption: StateFlow<String> = _liveUserCaption.asStateFlow()

    private val _liveModelCaption = MutableStateFlow("")
    val liveModelCaption: StateFlow<String> = _liveModelCaption.asStateFlow()

    private val _transcriptEvents = MutableSharedFlow<LiveTranscriptEvent>(extraBufferCapacity = 32)
    val transcriptEvents: SharedFlow<LiveTranscriptEvent> = _transcriptEvents.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var activeApiKey: String = ""
    private var activeSettings: XavionSettings? = null
    private var activeSystemInstruction: String = ""
    private var manualDisconnect = false
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private var setupCompleted = false

    private val modelTurnTextBuffer = StringBuilder()

    fun connect(
        apiKey: String,
        settings: XavionSettings,
        systemInstruction: String,
        autoStartMic: Boolean = true
    ) {
        if (apiKey.isBlank()) {
            _connectionState.value = LiveConnectionState.Error(
                "Gemini API key missing. Add your key in API Configuration first."
            )
            return
        }
        manualDisconnect = false
        reconnectAttempts = 0
        reconnectJob?.cancel()
        activeApiKey = apiKey
        activeSettings = settings
        activeSystemInstruction = systemInstruction
        openWebSocket(autoStartMic)
    }

    private fun openWebSocket(autoStartMic: Boolean) {
        webSocket?.close(1000, "Reopening session")
        setupCompleted = false
        modelTurnTextBuffer.clear()
        _liveUserCaption.value = ""
        _liveModelCaption.value = ""
        _connectionState.value = if (reconnectAttempts == 0) {
            LiveConnectionState.Connecting
        } else {
            LiveConnectionState.Reconnecting(reconnectAttempts)
        }

        val request = Request.Builder()
            .url("$WS_ENDPOINT?key=$activeApiKey")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                sendSetupMessage(webSocket)
                audioEngine.startAudioPlayback()
                if (autoStartMic) {
                    audioEngine.startMicrophoneCapture { pcmBytes ->
                        sendAudioChunk(pcmBytes)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerPayload(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                handleServerPayload(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!manualDisconnect && code != 1000) {
                    handleUnexpectedDisconnect("Session closed ($code): ${reason.ifBlank { "Connection lost" }}")
                } else {
                    teardownAudioAndState()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val httpCode = response?.code ?: 0
                val isQuota = httpCode == 429 || (t.message?.contains("429") == true)
                val msg = when (httpCode) {
                    401, 403 -> "API key unauthorized for Gemini Live (HTTP $httpCode)."
                    429 -> "Gemini Live quota limit reached (HTTP 429)."
                    else -> t.localizedMessage ?: "Live connection interrupted."
                }
                if (!manualDisconnect && httpCode !in listOf(401, 403)) {
                    handleUnexpectedDisconnect(msg, isQuota)
                } else {
                    audioEngine.stopMicrophoneCapture()
                    audioEngine.stopAudioPlayback()
                    _connectionState.value = LiveConnectionState.Error(msg, isQuota)
                }
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val settings = activeSettings ?: return
        val modelName = if (settings.liveModelId.startsWith("models/")) {
            settings.liveModelId
        } else {
            "models/${settings.liveModelId}"
        }

        val setupObj = JSONObject()
            .put("model", modelName)
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put(
                                "prebuiltVoiceConfig",
                                JSONObject().put("voiceName", settings.voiceName)
                            )
                        )
                    )
            )
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", activeSystemInstruction))
                )
            )
            .put("inputAudioTranscription", JSONObject())
            .put("outputAudioTranscription", JSONObject())
            .put("tools", DeviceCommandRegistry.buildGeminiToolsJson())

        ws.send(JSONObject().put("setup", setupObj).toString())
    }

    fun sendAudioChunk(pcm16Bytes: ByteArray) {
        if (!setupCompleted) return
        val ws = webSocket ?: return
        val b64 = Base64.encodeToString(pcm16Bytes, Base64.NO_WRAP)
        val payload = JSONObject().put(
            "realtimeInput",
            JSONObject().put(
                "mediaChunks",
                JSONArray().put(
                    JSONObject()
                        .put("mimeType", "audio/pcm;rate=16000")
                        .put("data", b64)
                )
            )
        )
        ws.send(payload.toString())
    }

    fun sendVisionFrame(jpegBytes: ByteArray) {
        if (!setupCompleted) return
        val ws = webSocket ?: return
        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val payload = JSONObject().put(
            "realtimeInput",
            JSONObject().put(
                "mediaChunks",
                JSONArray().put(
                    JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", b64)
                )
            )
        )
        ws.send(payload.toString())
    }

    fun sendTextTurn(prompt: String) {
        val ws = webSocket ?: return
        if (!setupCompleted || prompt.isBlank()) return
        _liveUserCaption.value = prompt
        _transcriptEvents.tryEmit(LiveTranscriptEvent.UserTranscript(prompt, isFinal = true))
        _connectionState.value = LiveConnectionState.Thinking
        val payload = JSONObject().put(
            "clientContent",
            JSONObject()
                .put(
                    "turns",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    )
                )
                .put("turnComplete", true)
        )
        ws.send(payload.toString())
    }

    fun interruptResponse() {
        audioEngine.clearOutputBuffer()
        if (setupCompleted) {
            _connectionState.value = if (audioEngine.isMicActive.value) {
                LiveConnectionState.Listening
            } else {
                LiveConnectionState.ConnectedIdle
            }
        }
    }

    fun toggleMic(): Boolean {
        return if (audioEngine.isMicActive.value) {
            val nextMuted = !audioEngine.isMuted.value
            audioEngine.setMuted(nextMuted)
            if (setupCompleted && _connectionState.value !is LiveConnectionState.Speaking) {
                _connectionState.value = if (nextMuted) {
                    LiveConnectionState.ConnectedIdle
                } else {
                    LiveConnectionState.Listening
                }
            }
            !nextMuted
        } else {
            audioEngine.setMuted(false)
            audioEngine.startMicrophoneCapture { pcm -> sendAudioChunk(pcm) }
            if (setupCompleted) _connectionState.value = LiveConnectionState.Listening
            true
        }
    }

    fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        teardownAudioAndState()
    }

    private fun teardownAudioAndState() {
        setupCompleted = false
        audioEngine.stopMicrophoneCapture()
        audioEngine.stopAudioPlayback()
        _connectionState.value = LiveConnectionState.Disconnected
    }

    private fun handleUnexpectedDisconnect(reason: String, isQuota: Boolean = false) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS || isQuota) {
            audioEngine.stopMicrophoneCapture()
            audioEngine.stopAudioPlayback()
            _connectionState.value = LiveConnectionState.Error(reason, isQuota)
            return
        }
        reconnectAttempts++
        val attempt = reconnectAttempts
        _connectionState.value = LiveConnectionState.Reconnecting(attempt)
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(1500L * attempt)
            if (!manualDisconnect) {
                openWebSocket(autoStartMic = !audioEngine.isMuted.value)
            }
        }
    }

    private fun handleServerPayload(raw: String) {
        runCatching {
            val json = JSONObject(raw)

            if (json.has("setupComplete")) {
                setupCompleted = true
                _connectionState.value = if (audioEngine.isMicActive.value && !audioEngine.isMuted.value) {
                    LiveConnectionState.Listening
                } else {
                    LiveConnectionState.ConnectedIdle
                }
                // Request initial JARVIS greeting on connect if desired
                sendTextTurn("Systems just came online. Greet the owner concisely.")
                return
            }

            if (json.has("error")) {
                val err = json.getJSONObject("error")
                val msg = err.optString("message", "Gemini Live error")
                val code = err.optInt("code", 0)
                _connectionState.value = LiveConnectionState.Error(msg, isQuotaError = code == 429)
                return
            }

            if (json.has("toolCall")) {
                val calls = json.getJSONObject("toolCall").optJSONArray("functionCalls") ?: JSONArray()
                _connectionState.value = LiveConnectionState.Thinking
                scope.launch {
                    val responses = JSONArray()
                    for (i in 0 until calls.length()) {
                        val call = calls.getJSONObject(i)
                        val id = call.optString("id", "call_$i")
                        val name = call.getString("name")
                        val args = call.optJSONObject("args") ?: JSONObject()
                        _transcriptEvents.tryEmit(LiveTranscriptEvent.ToolEvent("Executing $name..."))
                        val result = commandExecutor.executeFromGemini(id, name, args)
                        _transcriptEvents.tryEmit(
                            LiveTranscriptEvent.ToolEvent("$name: ${result.optString("message")}")
                        )
                        responses.put(
                            JSONObject()
                                .put("id", id)
                                .put("name", name)
                                .put("response", result)
                        )
                    }
                    val toolResponse = JSONObject().put(
                        "toolResponse",
                        JSONObject().put("functionResponses", responses)
                    )
                    webSocket?.send(toolResponse.toString())
                }
            }

            if (json.has("serverContent")) {
                val content = json.getJSONObject("serverContent")

                if (content.optBoolean("interrupted", false)) {
                    audioEngine.clearOutputBuffer()
                    modelTurnTextBuffer.clear()
                    _connectionState.value = LiveConnectionState.Listening
                }

                val inputTrans = content.optJSONObject("inputTranscription")
                val userDelta = inputTrans?.optString("text").orEmpty()
                if (userDelta.isNotBlank()) {
                    _liveUserCaption.value = (_liveUserCaption.value + " " + userDelta).trim()
                }

                val outputTrans = content.optJSONObject("outputTranscription")
                val outDelta = outputTrans?.optString("text").orEmpty()
                if (outDelta.isNotBlank()) {
                    modelTurnTextBuffer.append(outDelta)
                    _liveModelCaption.value = modelTurnTextBuffer.toString().trim()
                }

                val modelTurn = content.optJSONObject("modelTurn")
                val parts = modelTurn?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inline = part.getJSONObject("inlineData")
                            val mime = inline.optString("mimeType")
                            if (mime.startsWith("audio/pcm")) {
                                val pcmBytes = Base64.decode(inline.getString("data"), Base64.DEFAULT)
                                _connectionState.value = LiveConnectionState.Speaking
                                audioEngine.enqueuePcmOutput(pcmBytes)
                            }
                        }
                        if (part.has("text")) {
                            val txt = part.optString("text")
                            if (txt.isNotBlank()) {
                                modelTurnTextBuffer.append(txt)
                                _liveModelCaption.value = modelTurnTextBuffer.toString().trim()
                            }
                        }
                    }
                }

                if (content.optBoolean("turnComplete", false)) {
                    val userText = _liveUserCaption.value.trim()
                    if (userText.isNotBlank() &&
                        userText != "Systems just came online. Greet the owner concisely."
                    ) {
                        _transcriptEvents.tryEmit(LiveTranscriptEvent.UserTranscript(userText, isFinal = true))
                    }
                    _liveUserCaption.value = ""

                    val finalModel = modelTurnTextBuffer.toString().trim()
                    if (finalModel.isNotBlank()) {
                        _transcriptEvents.tryEmit(LiveTranscriptEvent.ModelTranscript(finalModel, isFinal = true))
                    }
                    modelTurnTextBuffer.clear()

                    _connectionState.value = if (audioEngine.isMicActive.value && !audioEngine.isMuted.value) {
                        LiveConnectionState.Listening
                    } else {
                        LiveConnectionState.ConnectedIdle
                    }
                }
            }
        }
    }
}
