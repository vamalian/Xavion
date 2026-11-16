package com.example.ai

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sqrt

/**
 * Real-time bidirectional PCM16 audio engine for Gemini Live (16kHz mic input -> 24kHz speaker output)
 * plus optional Android TextToSpeech for text-mode readouts.
 */
class XavionAudioEngine(private val context: Context) {

    companion object {
        const val INPUT_SAMPLE_RATE = 16_000
        const val OUTPUT_SAMPLE_RATE = 24_000
        private const val VAD_THRESHOLD = 0.045f
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _inputAmplitude = MutableStateFlow(0f)
    val inputAmplitude: StateFlow<Float> = _inputAmplitude.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isVoiceDetected = MutableStateFlow(false)
    val isVoiceDetected: StateFlow<Boolean> = _isVoiceDetected.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var recordJob: Job? = null

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val outputQueue = Channel<ByteArray>(capacity = 256)

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.language = Locale.US
                tts?.setPitch(0.92f) // Calm, deeper male pitch for text readout
            }
        }
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            _inputAmplitude.value = 0f
            _isVoiceDetected.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startMicrophoneCapture(onPcmChunk: (ByteArray) -> Unit) {
        if (recordJob?.isActive == true) return
        val minBuf = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(3200)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }

        val sessionId = record.audioSessionId
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = runCatching {
                AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
            }.getOrNull()
        }
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = runCatching {
                NoiseSuppressor.create(sessionId)?.apply { enabled = true }
            }.getOrNull()
        }

        audioRecord = record
        record.startRecording()
        _isMicActive.value = true

        // 2048 bytes = 1024 samples = 64ms chunks at 16kHz PCM16 mono
        val buffer = ByteArray(2048)
        recordJob = scope.launch {
            try {
                while (isActive && _isMicActive.value) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        if (_isMuted.value) {
                            _inputAmplitude.value = 0f
                            _isVoiceDetected.value = false
                        } else {
                            val chunk = buffer.copyOf(read)
                            val rms = computeNormalizedRms(chunk, read)
                            _inputAmplitude.value = rms
                            _isVoiceDetected.value = rms > VAD_THRESHOLD
                            onPcmChunk(chunk)
                        }
                    } else {
                        delay(10)
                    }
                }
            } finally {
                stopMicrophoneInternal()
            }
        }
    }

    fun stopMicrophoneCapture() {
        _isMicActive.value = false
        recordJob?.cancel()
        recordJob = null
        stopMicrophoneInternal()
    }

    private fun stopMicrophoneInternal() {
        _inputAmplitude.value = 0f
        _isVoiceDetected.value = false
        runCatching {
            audioRecord?.stop()
            audioRecord?.release()
        }
        audioRecord = null
        runCatching { echoCanceler?.release() }
        echoCanceler = null
        runCatching { noiseSuppressor?.release() }
        noiseSuppressor = null
    }

    fun startAudioPlayback() {
        if (playbackJob?.isActive == true) return
        val minBuf = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4800)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack = track
        track.play()

        playbackJob = scope.launch {
            try {
                for (chunk in outputQueue) {
                    if (!isActive) break
                    val rms = computeNormalizedRms(chunk, chunk.size)
                    _outputAmplitude.value = rms
                    track.write(chunk, 0, chunk.size)
                    if (outputQueue.isEmpty) {
                        delay(40)
                        if (outputQueue.isEmpty) {
                            _outputAmplitude.value = 0f
                        }
                    }
                }
            } finally {
                _outputAmplitude.value = 0f
                runCatching {
                    track.stop()
                    track.release()
                }
                audioTrack = null
            }
        }
    }

    fun enqueuePcmOutput(pcmBytes: ByteArray) {
        if (playbackJob?.isActive != true) {
            startAudioPlayback()
        }
        outputQueue.trySend(pcmBytes)
    }

    fun clearOutputBuffer() {
        while (outputQueue.tryReceive().isSuccess) {
            // Drain pending queued audio on barge-in interruption
        }
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        }
        _outputAmplitude.value = 0f
    }

    fun stopAudioPlayback() {
        clearOutputBuffer()
        playbackJob?.cancel()
        playbackJob = null
    }

    fun speakTextTts(text: String, speed: Float = 1.0f) {
        if (!ttsReady || text.isBlank()) return
        tts?.setSpeechRate(speed.coerceIn(0.75f, 1.5f))
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "xavion_tts_${System.currentTimeMillis()}")
    }

    fun stopTts() {
        tts?.stop()
    }

    fun release() {
        stopMicrophoneCapture()
        stopAudioPlayback()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private fun computeNormalizedRms(pcm16: ByteArray, length: Int): Float {
        if (length < 2) return 0f
        var sumSq = 0.0
        var samples = 0
        var i = 0
        while (i + 1 < length) {
            val lo = pcm16[i].toInt() and 0xFF
            val hi = pcm16[i + 1].toInt()
            val sample = (hi shl 8) or lo
            val normalized = sample / 32768.0
            sumSq += normalized * normalized
            samples++
            i += 2
        }
        if (samples == 0) return 0f
        val rms = sqrt(sumSq / samples).toFloat()
        return (rms * 3.8f).coerceIn(0f, 1f)
    }
}
