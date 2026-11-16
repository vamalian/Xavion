package com.example.ai

import com.example.data.local.MemoryRecordEntity
import com.example.data.model.XavionSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SystemPromptBuilder {

    fun build(
        settings: XavionSettings,
        memories: List<MemoryRecordEntity>
    ): String {
        val nowFormatted = SimpleDateFormat("EEEE, MMMM d, yyyy HH:mm", Locale.getDefault()).format(Date())
        val ownerSalutation = if (settings.addressAsSir) {
            "Address the user respectfully as '${settings.ownerName.ifBlank { "Sir" }}' when natural."
        } else {
            "Address the user warmly by name or naturally without forced honorifics."
        }

        val sb = StringBuilder()
        sb.appendLine(settings.personalityPreset.basePrompt)
        sb.appendLine()
        sb.appendLine("IDENTITY & RULES:")
        sb.appendLine("- Your name is XAVION. You are a male AI personal assistant.")
        sb.appendLine("- $ownerSalutation")
        sb.appendLine("- ${settings.language.promptDirective}")
        sb.appendLine("- ${settings.verbosity.directive}")
        sb.appendLine("- Current local date and time: $nowFormatted.")
        sb.appendLine("- Honesty constraint: NEVER claim to have opened an app, changed a setting, set an alarm, saved a note, or completed a device action unless you invoked the corresponding tool and received status 'ok'. If a permission is missing or the user cancelled confirmation, explain accurately.")
        sb.appendLine("- Default opening greeting when starting a fresh voice session: \"Good day, Sir. XAVION systems are online. How may I assist you?\"")

        if (settings.customSystemInstructions.isNotBlank()) {
            sb.appendLine()
            sb.appendLine("USER CUSTOM INSTRUCTIONS:")
            sb.appendLine(settings.customSystemInstructions.trim())
        }

        if (settings.memoryEnabled && memories.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("PERSISTENT USER MEMORY:")
            memories.take(20).forEach { mem ->
                sb.appendLine("- [${mem.category}] ${mem.title}: ${mem.content}")
            }
        }
        return sb.toString().trim()
    }
}
