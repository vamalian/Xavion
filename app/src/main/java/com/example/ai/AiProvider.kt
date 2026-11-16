package com.example.ai

import com.example.data.local.ChatMessageEntity
import com.example.data.model.XavionSettings

data class ConnectivityTestResult(
    val success: Boolean,
    val statusCode: Int,
    val latencyMs: Long,
    val message: String,
    val discoveredModels: List<String> = emptyList()
)

data class AiChatResponse(
    val text: String,
    val executedTools: List<String> = emptyList()
)

interface AiTextProvider {
    val providerName: String

    suspend fun testConnectivity(apiKey: String, settings: XavionSettings): ConnectivityTestResult

    suspend fun generateResponse(
        apiKey: String,
        settings: XavionSettings,
        systemInstruction: String,
        history: List<ChatMessageEntity>,
        userPrompt: String,
        imageBase64Jpeg: String? = null
    ): Result<AiChatResponse>
}
