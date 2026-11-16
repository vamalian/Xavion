package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.vaultDataStore: DataStore<Preferences> by preferencesDataStore(name = "xavion_secure_vault")

/**
 * Hardware-backed Android Keystore AES-256-GCM vault.
 * Encrypts sensitive credentials (Gemini API key, custom provider key) before persisting in DataStore.
 * Never logs plaintext secrets.
 */
class KeystoreVault(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "xavion_master_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private val KEY_GEMINI_API_ENC = stringPreferencesKey("enc_gemini_api_key")
        private val KEY_CUSTOM_PROVIDER_ENC = stringPreferencesKey("enc_custom_provider_key")
        private val KEY_PIN_HASH = stringPreferencesKey("app_pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("app_pin_salt")
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val ivEncoded = Base64.encodeToString(iv, Base64.NO_WRAP)
        val dataEncoded = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$ivEncoded:$dataEncoded"
    }

    private fun decrypt(payload: String): String? {
        return try {
            val parts = payload.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val data = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(data), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    val hasGeminiApiKeyFlow: Flow<Boolean> = context.vaultDataStore.data.map { prefs ->
        !prefs[KEY_GEMINI_API_ENC].isNullOrBlank()
    }

    val maskedGeminiApiKeyFlow: Flow<String> = context.vaultDataStore.data.map { prefs ->
        val enc = prefs[KEY_GEMINI_API_ENC] ?: return@map ""
        val plain = decrypt(enc).orEmpty()
        maskKey(plain)
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        context.vaultDataStore.edit { prefs ->
            if (trimmed.isEmpty()) {
                prefs.remove(KEY_GEMINI_API_ENC)
            } else {
                prefs[KEY_GEMINI_API_ENC] = encrypt(trimmed)
            }
        }
    }

    suspend fun getGeminiApiKey(): String? {
        val enc = context.vaultDataStore.data.first()[KEY_GEMINI_API_ENC] ?: return null
        return decrypt(enc)?.takeIf { it.isNotBlank() }
    }

    suspend fun clearGeminiApiKey() {
        context.vaultDataStore.edit { prefs ->
            prefs.remove(KEY_GEMINI_API_ENC)
        }
    }

    suspend fun saveCustomProviderKey(apiKey: String) {
        val trimmed = apiKey.trim()
        context.vaultDataStore.edit { prefs ->
            if (trimmed.isEmpty()) {
                prefs.remove(KEY_CUSTOM_PROVIDER_ENC)
            } else {
                prefs[KEY_CUSTOM_PROVIDER_ENC] = encrypt(trimmed)
            }
        }
    }

    suspend fun getCustomProviderKey(): String? {
        val enc = context.vaultDataStore.data.first()[KEY_CUSTOM_PROVIDER_ENC] ?: return null
        return decrypt(enc)?.takeIf { it.isNotBlank() }
    }

    val hasPinConfiguredFlow: Flow<Boolean> = context.vaultDataStore.data.map { prefs ->
        !prefs[KEY_PIN_HASH].isNullOrBlank()
    }

    suspend fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hash = hashPin(pin, saltB64)
        context.vaultDataStore.edit { prefs ->
            prefs[KEY_PIN_SALT] = saltB64
            prefs[KEY_PIN_HASH] = hash
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.vaultDataStore.data.first()
        val salt = prefs[KEY_PIN_SALT] ?: return false
        val expectedHash = prefs[KEY_PIN_HASH] ?: return false
        return MessageDigest.isEqual(
            hashPin(pin, salt).toByteArray(StandardCharsets.UTF_8),
            expectedHash.toByteArray(StandardCharsets.UTF_8)
        )
    }

    suspend fun clearPin() {
        context.vaultDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs.remove(KEY_PIN_SALT)
        }
    }

    suspend fun wipeAllSecrets() {
        context.vaultDataStore.edit { it.clear() }
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun maskKey(key: String): String {
        if (key.isBlank()) return ""
        if (key.length <= 8) return "••••••••"
        return "${key.take(4)}••••••••••••${key.takeLast(4)}"
    }
}
