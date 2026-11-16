package com.example.data.model

enum class AvatarMode(val displayName: String, val subtitle: String) {
    MALE_VIRTUAL_AVATAR("Male Virtual Avatar", "Holographic neural portrait with reactive telemetry"),
    HOLOGRAPHIC_CORE("Holographic Energy Core", "Rotating arc-reactor geometry & plasma pulses"),
    MINIMAL_VOICE("Minimal Voice Assistant", "Clean acoustic waveform & crisp HUD captions")
}

enum class AssistantLanguage(val displayName: String, val promptDirective: String) {
    ENGLISH("English", "Respond primarily in clear, natural English."),
    URDU("Urdu", "Respond naturally in Urdu (Roman Urdu or Urdu script as appropriate to user input)."),
    BILINGUAL("English & Urdu", "Fluently understand and respond in both English and Urdu, matching the user's language naturally.")
}

enum class VerbosityLevel(val displayName: String, val directive: String) {
    CONCISE("Concise", "Keep answers sharp, concise, and executive unless asked for depth."),
    BALANCED("Balanced", "Provide clear, helpful answers with essential context."),
    DETAILED("Detailed", "Provide thorough, structured explanations and step-by-step analysis.")
}

enum class PersonalityPreset(val displayName: String, val description: String, val basePrompt: String) {
    JARVIS_CLASSIC(
        "JARVIS Classic",
        "Sophisticated, calm, composed, subtly witty British-style butler intelligence.",
        "You are XAVION, a sophisticated, calm, highly intelligent male AI personal companion inspired by an advanced holographic butler. You are confident, respectful, observant, and warm without ever being robotic."
    ),
    TACTICAL_BRIEF(
        "Tactical Brief",
        "Ultra-crisp, mission-ready telemetry style with minimal filler.",
        "You are XAVION, a precision tactical AI advisor. Deliver crisp, actionable intelligence, system status confirmations, and direct answers with high clarity."
    ),
    MENTOR_COMPANION(
        "Executive Strategist",
        "Thoughtful, analytical, collaborative problem-solving partner.",
        "You are XAVION, an executive strategist and engineering companion. Reason carefully, offer structured options, and anticipate practical follow-ups."
    ),
    CUSTOM(
        "Custom Persona",
        "Driven exclusively by your custom system instructions below.",
        "You are XAVION, a capable male AI assistant."
    )
}

enum class AiProviderType(val displayName: String) {
    GEMINI_LIVE("Google Gemini (Live + REST)"),
    CUSTOM_OPENAI_COMPAT("Custom OpenAI-Compatible Endpoint")
}

enum class GeminiLiveModelOption(val modelId: String, val label: String, val notes: String) {
    GEMINI_2_5_FLASH_NATIVE_AUDIO(
        "models/gemini-2.5-flash-native-audio-preview-09-2025",
        "Gemini 2.5 Flash Native Audio",
        "Recommended for Gemini Live real-time native audio & tool calling"
    ),
    GEMINI_2_0_FLASH_LIVE(
        "models/gemini-2.0-flash-live-001",
        "Gemini 2.0 Flash Live",
        "Low-latency bidirectional voice & vision live session"
    ),
    GEMINI_LIVE_2_5_FLASH_PREVIEW(
        "models/gemini-live-2.5-flash-preview",
        "Gemini Live 2.5 Flash Preview",
        "Multimodal bidirectional live streaming preview"
    )
}

enum class GeminiRestModelOption(val modelId: String, val label: String) {
    GEMINI_2_5_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash (Fast Multimodal)"),
    GEMINI_2_5_PRO("gemini-2.5-pro", "Gemini 2.5 Pro (Complex Reasoning)"),
    GEMINI_2_0_FLASH("gemini-2.0-flash", "Gemini 2.0 Flash (Legacy Fallback)")
}

enum class XavionVoiceOption(val voiceName: String, val description: String) {
    CHARON("Charon", "Calm, authoritative deep male voice (Default)"),
    ORUS("Orus", "Composed, articulate executive male voice"),
    FENRIR("Fenrir", "Warm, resonant confident male voice"),
    PUCK("Puck", "Energetic, crisp conversational voice"),
    AOEDE("Aoede", "Clear, expressive neutral-high voice"),
    KORE("Kore", "Balanced neutral studio voice")
}

data class XavionSettings(
    val liveModelId: String = GeminiLiveModelOption.GEMINI_2_5_FLASH_NATIVE_AUDIO.modelId,
    val restModelId: String = GeminiRestModelOption.GEMINI_2_5_FLASH.modelId,
    val voiceName: String = XavionVoiceOption.CHARON.voiceName,
    val speechSpeed: Float = 1.0f,
    val verbosity: VerbosityLevel = VerbosityLevel.CONCISE,
    val addressAsSir: Boolean = true,
    val ownerName: String = "Sir",
    val language: AssistantLanguage = AssistantLanguage.BILINGUAL,
    val personalityPreset: PersonalityPreset = PersonalityPreset.JARVIS_CLASSIC,
    val customSystemInstructions: String = "",
    val memoryEnabled: Boolean = true,
    val avatarMode: AvatarMode = AvatarMode.MALE_VIRTUAL_AVATAR,
    val wakeWordEnabled: Boolean = false,
    val floatingBubbleEnabled: Boolean = false,
    val weatherCity: String = "San Francisco",
    val requireConfirmationForSensitiveActions: Boolean = true,
    val appLockEnabled: Boolean = false,
    val biometricUnlockEnabled: Boolean = false,
    val screenshotProtectionEnabled: Boolean = true,
    val activeProvider: AiProviderType = AiProviderType.GEMINI_LIVE,
    val customProviderBaseUrl: String = "",
    val customProviderModel: String = ""
)

sealed interface LiveConnectionState {
    data object Disconnected : LiveConnectionState
    data object Connecting : LiveConnectionState
    data object ConnectedIdle : LiveConnectionState
    data object Listening : LiveConnectionState
    data object Thinking : LiveConnectionState
    data object Speaking : LiveConnectionState
    data class Reconnecting(val attempt: Int) : LiveConnectionState
    data class Error(val message: String, val isQuotaError: Boolean = false) : LiveConnectionState
}

data class WeatherInfo(
    val city: String,
    val temperatureCelsius: Double,
    val conditionLabel: String,
    val windKmh: Double,
    val updatedAtMillis: Long
)

data class DeviceTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val networkAvailable: Boolean,
    val networkType: String,
    val mediaVolumePercent: Int,
    val flashlightOn: Boolean
)

data class PendingCommandConfirmation(
    val callId: String,
    val functionName: String,
    val summary: String,
    val details: String,
    val argumentsJson: String
)
