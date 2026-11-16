package com.example

import com.example.ai.SystemPromptBuilder
import com.example.commands.DeviceCommandRegistry
import com.example.data.local.MemoryRecordEntity
import com.example.data.model.AssistantLanguage
import com.example.data.model.XavionSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class XavionCoreUnitTest {

    @Test
    fun systemPromptBuilder_includesPersonalitySirAndMemoriesWhenEnabled() {
        val settings = XavionSettings(
            addressAsSir = true,
            ownerName = "Sir",
            language = AssistantLanguage.BILINGUAL,
            memoryEnabled = true,
            customSystemInstructions = "Prioritize battery conservation."
        )
        val memories = listOf(
            MemoryRecordEntity(id = 1, category = "Preference", title = "Coffee", content = "Prefers black espresso")
        )
        val prompt = SystemPromptBuilder.build(settings, memories)
        assertTrue(prompt.contains("XAVION"))
        assertTrue(prompt.contains("'Sir'"))
        assertTrue(prompt.contains("Urdu"))
        assertTrue(prompt.contains("Prefers black espresso"))
        assertTrue(prompt.contains("Prioritize battery conservation."))
    }

    @Test
    fun systemPromptBuilder_omitsMemoriesWhenDisabled() {
        val settings = XavionSettings(memoryEnabled = false, addressAsSir = false)
        val memories = listOf(
            MemoryRecordEntity(id = 1, category = "Fact", title = "Secret", content = "Hidden detail")
        )
        val prompt = SystemPromptBuilder.build(settings, memories)
        assertFalse(prompt.contains("Hidden detail"))
    }

    @Test
    fun deviceCommandRegistry_buildsValidFunctionDeclarations() {
        val tools = DeviceCommandRegistry.buildGeminiToolsJson()
        assertTrue(tools.length() == 1)
        val declarations = tools.getJSONObject(0).getJSONArray("functionDeclarations")
        val names = (0 until declarations.length()).map {
            declarations.getJSONObject(it).getString("name")
        }
        assertTrue("open_app" in names)
        assertTrue("toggle_flashlight" in names)
        assertTrue("set_alarm" in names)
        assertTrue("create_note" in names)
        assertTrue("read_notifications" in names)
        assertTrue("get_device_status" in names)
    }
}
