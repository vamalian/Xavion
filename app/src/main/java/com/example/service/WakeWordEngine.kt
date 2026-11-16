package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Battery-conscious wake phrase listener ("Hey XAVION") operating strictly while the app
 * or its explicit Foreground Service is active. Does not claim to operate when app is closed.
 */
class WakeWordEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var shouldListen = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _statusMessage = MutableStateFlow("Standby (In-App / Foreground Service only)")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _wakeTriggered = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val wakeTriggered: SharedFlow<Unit> = _wakeTriggered.asSharedFlow()

    fun isSupported(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _statusMessage.value = "Microphone permission required for 'Hey XAVION'."
            return
        }
        if (!isSupported()) {
            _statusMessage.value = "On-device SpeechRecognizer unavailable on this device."
            return
        }
        shouldListen = true
        mainHandler.post { beginRecognitionCycle() }
    }

    fun stop() {
        shouldListen = false
        _isListening.value = false
        _statusMessage.value = "Wake word paused"
        mainHandler.post {
            runCatching {
                recognizer?.stopListening()
                recognizer?.destroy()
            }
            recognizer = null
        }
    }

    private fun beginRecognitionCycle() {
        if (!shouldListen) return
        runCatching { recognizer?.destroy() }
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = sr

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _statusMessage.value = "Listening for 'Hey XAVION' (Foreground active)"
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                if (shouldListen) {
                    val delayMs = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1500L else 900L
                    mainHandler.postDelayed({ beginRecognitionCycle() }, delayMs)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (matches.any { containsWakePhrase(it) }) {
                    _statusMessage.value = "Wake phrase detected!"
                    _wakeTriggered.tryEmit(Unit)
                    stop()
                    return
                }
                if (shouldListen) {
                    mainHandler.postDelayed({ beginRecognitionCycle() }, 400L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (partials.any { containsWakePhrase(it) }) {
                    _statusMessage.value = "Wake phrase detected!"
                    _wakeTriggered.tryEmit(Unit)
                    stop()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { sr.startListening(intent) }.onFailure {
            _statusMessage.value = "SpeechRecognizer error: ${it.localizedMessage}"
            _isListening.value = false
        }
    }

    private fun containsWakePhrase(phrase: String): Boolean {
        val norm = phrase.lowercase(Locale.ROOT)
        return norm.contains("hey xavion") ||
            norm.contains("xavion") ||
            norm.contains("hey javion") ||
            norm.contains("hey savion")
    }
}
