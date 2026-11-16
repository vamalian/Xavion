package com.example.ai

import com.example.commands.DeviceCommandExecutor
import com.example.commands.DeviceCommandRegistry
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AiProviderType
import com.example.data.model.XavionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiRestClient(
    private val httpClient: OkHttpClient,
    private val commandExecutor: DeviceCommandExecutor
) : AiTextProvider {

    override val providerName: String = "Google Gemini REST"

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun testConnectivity(
        apiKey: String,
        settings: XavionSettings
    ): ConnectivityTestResult = withContext(Dispatchers.IO) {
        if (settings.activeProvider == AiProviderType.CUSTOM_OPENAI_COMPAT) {
            return@withContext testCustomEndpoint(apiKey, settings)
        }
        if (apiKey.isBlank()) {
            return@withContext ConnectivityTestResult(
                success = false,
                statusCode = 0,
                latencyMs = 0,
                message = "No Gemini API key stored in Android Keystore vault."
            )
        }
        val start = System.currentTimeMillis()
        try {
            val req = Request.Builder()
                .url("$BASE_URL/models?key=$apiKey")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = parseGeminiError(body, resp.code)
                    return@withContext ConnectivityTestResult(
                        success = false,
                        statusCode = resp.code,
                        latencyMs = latency,
                        message = errMsg
                    )
                }
                val modelsJson = JSONObject(body).optJSONArray("models") ?: JSONArray()
                val names = buildList {
                    for (i in 0 until modelsJson.length()) {
                        val name = modelsJson.getJSONObject(i).optString("name")
                        if (name.contains("gemini", ignoreCase = true)) {
                            add(name.removePrefix("models/"))
                        }
                    }
                }
                ConnectivityTestResult(
                    success = true,
                    statusCode = resp.code,
                    latencyMs = latency,
                    message = "Connected to Gemini API (${names.size} Gemini models available).",
                    discoveredModels = names.take(15)
                )
            }
        } catch (e: Exception) {
            ConnectivityTestResult(
                success = false,
                statusCode = 0,
                latencyMs = System.currentTimeMillis() - start,
                message = "Network error: ${e.localizedMessage ?: "Unable to reach Gemini API."}"
            )
        }
    }

    override suspend fun generateResponse(
        apiKey: String,
        settings: XavionSettings,
        systemInstruction: String,
        history: List<ChatMessageEntity>,
        userPrompt: String,
        imageBase64Jpeg: String?
    ): Result<AiChatResponse> = withContext(Dispatchers.IO) {
        if (settings.activeProvider == AiProviderType.CUSTOM_OPENAI_COMPAT) {
            return@withContext generateCustomOpenAiResponse(apiKey, settings, systemInstruction, history, userPrompt)
        }
        runCatching {
            require(apiKey.isNotBlank()) { "Gemini API key is not configured. Open API Configuration to add your key." }
            val model = settings.restModelId.removePrefix("models/").ifBlank { "gemini-2.5-flash" }
            val contents = JSONArray()

            // Recent history (skip tool/system UI banners)
            history.takeLast(14).forEach { msg ->
                val role = if (msg.sender == "USER") "user" else if (msg.sender == "XAVION") "model" else return@forEach
                if (msg.content.isNotBlank()) {
                    contents.put(
                        JSONObject()
                            .put("role", role)
                            .put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                    )
                }
            }

            val currentParts = JSONArray()
            if (!imageBase64Jpeg.isNullOrBlank()) {
                currentParts.put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", imageBase64Jpeg)
                    )
                )
            }
            currentParts.put(JSONObject().put("text", userPrompt))
            contents.put(JSONObject().put("role", "user").put("parts", currentParts))

            val executedTools = mutableListOf<String>()
            var rounds = 0
            var finalText = ""

            while (rounds < 3) {
                rounds++
                val payload = JSONObject()
                    .put(
                        "systemInstruction",
                        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    )
                    .put("contents", contents)
                    .put("tools", DeviceCommandRegistry.buildGeminiToolsJson())

                val req = Request.Builder()
                    .url("$BASE_URL/models/$model:generateContent?key=$apiKey")
                    .post(payload.toString().toRequestBody(JSON_MEDIA))
                    .build()

                val respJson = httpClient.newCall(req).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        error(parseGeminiError(bodyStr, resp.code))
                    }
                    JSONObject(bodyStr)
                }

                val candidates = respJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    error("Gemini returned no response candidates.")
                }
                val content = candidates.getJSONObject(0).optJSONObject("content")
                    ?: error("Empty candidate content from Gemini.")
                val parts = content.optJSONArray("parts") ?: JSONArray()

                // Append model turn
                contents.put(content)

                val functionCalls = mutableListOf<JSONObject>()
                val textAccumulator = StringBuilder()

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("text")) {
                        textAccumulator.append(part.optString("text"))
                    }
                    if (part.has("functionCall")) {
                        functionCalls.add(part.getJSONObject("functionCall"))
                    }
                }

                if (functionCalls.isEmpty()) {
                    finalText = textAccumulator.toString().trim()
                    break
                }

                // Execute each requested Android function and send tool response back
                val responseParts = JSONArray()
                for (call in functionCalls) {
                    val fnName = call.getString("name")
                    val fnArgs = call.optJSONObject("args") ?: JSONObject()
                    val callId = "rest_${System.currentTimeMillis()}_$fnName"
                    val result = commandExecutor.executeFromGemini(callId, fnName, fnArgs)
                    executedTools.add("$fnName -> ${result.optString("status")}")
                    responseParts.put(
                        JSONObject().put(
                            "functionResponse",
                            JSONObject()
                                .put("name", fnName)
                                .put("response", result)
                        )
                    )
                }
                contents.put(JSONObject().put("role", "user").put("parts", responseParts))
            }

            AiChatResponse(
                text = finalText.ifBlank { "Command processed, Sir." },
                executedTools = executedTools
            )
        }
    }

    private fun testCustomEndpoint(apiKey: String, settings: XavionSettings): ConnectivityTestResult {
        val base = settings.customProviderBaseUrl.trim().trimEnd('/')
        if (base.isBlank()) {
            return ConnectivityTestResult(false, 0, 0, "Custom provider base URL is empty.")
        }
        val start = System.currentTimeMillis()
        return try {
            val reqBuilder = Request.Builder().url("$base/models").get()
            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                if (resp.isSuccessful) {
                    ConnectivityTestResult(true, resp.code, latency, "Connected to custom OpenAI-compatible endpoint.")
                } else {
                    ConnectivityTestResult(false, resp.code, latency, "Custom endpoint returned HTTP ${resp.code}.")
                }
            }
        } catch (e: Exception) {
            ConnectivityTestResult(false, 0, System.currentTimeMillis() - start, "Connection failed: ${e.message}")
        }
    }

    private fun generateCustomOpenAiResponse(
        apiKey: String,
        settings: XavionSettings,
        systemInstruction: String,
        history: List<ChatMessageEntity>,
        userPrompt: String
    ): Result<AiChatResponse> = runCatching {
        val base = settings.customProviderBaseUrl.trim().trimEnd('/')
        require(base.isNotBlank()) { "Custom provider Base URL is not configured." }
        val model = settings.customProviderModel.ifBlank { "gpt-4o-mini" }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        history.takeLast(10).forEach { msg ->
            val role = if (msg.sender == "USER") "user" else if (msg.sender == "XAVION") "assistant" else return@forEach
            messages.put(JSONObject().put("role", role).put("content", msg.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userPrompt))

        val payload = JSONObject()
            .put("model", model)
            .put("messages", messages)

        val reqBuilder = Request.Builder()
            .url("$base/chat/completions")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Custom provider HTTP ${resp.code}")
            val json = JSONObject(body)
            val text = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            AiChatResponse(text = text.trim())
        }
    }

    private fun parseGeminiError(body: String, code: Int): String {
        val apiMessage = runCatching {
            JSONObject(body).getJSONObject("error").optString("message")
        }.getOrNull()
        return when (code) {
            400 -> apiMessage ?: "Invalid Gemini API request or unsupported model."
            401, 403 -> apiMessage ?: "API key rejected or lacks permission (HTTP $code)."
            429 -> "Gemini API quota/rate limit reached (HTTP 429). Please retry shortly or check billing."
            else -> apiMessage ?: "Gemini API error (HTTP $code)."
        }
    }
}
