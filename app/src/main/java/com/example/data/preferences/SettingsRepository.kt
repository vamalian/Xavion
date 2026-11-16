package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiProviderType
import com.example.data.model.AssistantLanguage
import com.example.data.model.AvatarMode
import com.example.data.model.GeminiLiveModelOption
import com.example.data.model.GeminiRestModelOption
import com.example.data.model.PersonalityPreset
import com.example.data.model.VerbosityLevel
import com.example.data.model.XavionSettings
import com.example.data.model.XavionVoiceOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "xavion_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LIVE_MODEL_ID = stringPreferencesKey("live_model_id")
        val REST_MODEL_ID = stringPreferencesKey("rest_model_id")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val VERBOSITY = stringPreferencesKey("verbosity")
        val ADDRESS_AS_SIR = booleanPreferencesKey("address_as_sir")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val LANGUAGE = stringPreferencesKey("language")
        val PERSONALITY = stringPreferencesKey("personality")
        val CUSTOM_INSTRUCTIONS = stringPreferencesKey("custom_instructions")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val AVATAR_MODE = stringPreferencesKey("avatar_mode")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val FLOATING_BUBBLE_ENABLED = booleanPreferencesKey("floating_bubble_enabled")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val REQUIRE_CONFIRMATION = booleanPreferencesKey("require_confirmation")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC_UNLOCK_ENABLED = booleanPreferencesKey("biometric_unlock_enabled")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val CUSTOM_PROVIDER_URL = stringPreferencesKey("custom_provider_url")
        val CUSTOM_PROVIDER_MODEL = stringPreferencesKey("custom_provider_model")
    }

    val settingsFlow: Flow<XavionSettings> = context.settingsDataStore.data.map { prefs ->
        XavionSettings(
            liveModelId = prefs[Keys.LIVE_MODEL_ID] ?: GeminiLiveModelOption.GEMINI_2_5_FLASH_NATIVE_AUDIO.modelId,
            restModelId = prefs[Keys.REST_MODEL_ID] ?: GeminiRestModelOption.GEMINI_2_5_FLASH.modelId,
            voiceName = prefs[Keys.VOICE_NAME] ?: XavionVoiceOption.CHARON.voiceName,
            speechSpeed = prefs[Keys.SPEECH_SPEED] ?: 1.0f,
            verbosity = prefs[Keys.VERBOSITY]?.let { runCatching { VerbosityLevel.valueOf(it) }.getOrNull() }
                ?: VerbosityLevel.CONCISE,
            addressAsSir = prefs[Keys.ADDRESS_AS_SIR] ?: true,
            ownerName = prefs[Keys.OWNER_NAME] ?: "Sir",
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AssistantLanguage.valueOf(it) }.getOrNull() }
                ?: AssistantLanguage.BILINGUAL,
            personalityPreset = prefs[Keys.PERSONALITY]?.let { runCatching { PersonalityPreset.valueOf(it) }.getOrNull() }
                ?: PersonalityPreset.JARVIS_CLASSIC,
            customSystemInstructions = prefs[Keys.CUSTOM_INSTRUCTIONS] ?: "",
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            avatarMode = prefs[Keys.AVATAR_MODE]?.let { runCatching { AvatarMode.valueOf(it) }.getOrNull() }
                ?: AvatarMode.MALE_VIRTUAL_AVATAR,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            floatingBubbleEnabled = prefs[Keys.FLOATING_BUBBLE_ENABLED] ?: false,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: "San Francisco",
            requireConfirmationForSensitiveActions = prefs[Keys.REQUIRE_CONFIRMATION] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            biometricUnlockEnabled = prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] ?: false,
            screenshotProtectionEnabled = prefs[Keys.SCREENSHOT_PROTECTION] ?: true,
            activeProvider = prefs[Keys.ACTIVE_PROVIDER]?.let { runCatching { AiProviderType.valueOf(it) }.getOrNull() }
                ?: AiProviderType.GEMINI_LIVE,
            customProviderBaseUrl = prefs[Keys.CUSTOM_PROVIDER_URL] ?: "",
            customProviderModel = prefs[Keys.CUSTOM_PROVIDER_MODEL] ?: ""
        )
    }

    suspend fun current(): XavionSettings = settingsFlow.first()

    suspend fun update(transform: (XavionSettings) -> XavionSettings) {
        val updated = transform(current())
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LIVE_MODEL_ID] = updated.liveModelId
            prefs[Keys.REST_MODEL_ID] = updated.restModelId
            prefs[Keys.VOICE_NAME] = updated.voiceName
            prefs[Keys.SPEECH_SPEED] = updated.speechSpeed.coerceIn(0.75f, 1.5f)
            prefs[Keys.VERBOSITY] = updated.verbosity.name
            prefs[Keys.ADDRESS_AS_SIR] = updated.addressAsSir
            prefs[Keys.OWNER_NAME] = updated.ownerName
            prefs[Keys.LANGUAGE] = updated.language.name
            prefs[Keys.PERSONALITY] = updated.personalityPreset.name
            prefs[Keys.CUSTOM_INSTRUCTIONS] = updated.customSystemInstructions
            prefs[Keys.MEMORY_ENABLED] = updated.memoryEnabled
            prefs[Keys.AVATAR_MODE] = updated.avatarMode.name
            prefs[Keys.WAKE_WORD_ENABLED] = updated.wakeWordEnabled
            prefs[Keys.FLOATING_BUBBLE_ENABLED] = updated.floatingBubbleEnabled
            prefs[Keys.WEATHER_CITY] = updated.weatherCity
            prefs[Keys.REQUIRE_CONFIRMATION] = updated.requireConfirmationForSensitiveActions
            prefs[Keys.APP_LOCK_ENABLED] = updated.appLockEnabled
            prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] = updated.biometricUnlockEnabled
            prefs[Keys.SCREENSHOT_PROTECTION] = updated.screenshotProtectionEnabled
            prefs[Keys.ACTIVE_PROVIDER] = updated.activeProvider.name
            prefs[Keys.CUSTOM_PROVIDER_URL] = updated.customProviderBaseUrl
            prefs[Keys.CUSTOM_PROVIDER_MODEL] = updated.customProviderModel
        }
    }

    suspend fun resetAll() {
        context.settingsDataStore.edit { it.clear() }
    }
}
