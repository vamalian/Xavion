package com.example.commands

import org.json.JSONArray
import org.json.JSONObject

object DeviceCommandRegistry {

    val sensitiveFunctions = setOf(
        "open_website",
        "share_text",
        "open_app",
        "set_brightness",
        "create_calendar_event"
    )

    fun buildGeminiToolsJson(): JSONArray {
        val declarations = JSONArray().apply {
            put(
                fn(
                    name = "open_app",
                    description = "Launch an installed Android application by its display name or package name.",
                    properties = mapOf(
                        "app_name" to prop("string", "The human-readable name of the app to open, e.g. 'Chrome', 'Settings', 'Calendar', 'YouTube'.")
                    ),
                    required = listOf("app_name")
                )
            )
            put(
                fn(
                    name = "open_settings",
                    description = "Open a specific Android system settings screen such as Wi-Fi, Bluetooth, Display, Sound, Battery, or general Settings.",
                    properties = mapOf(
                        "setting_type" to enumProp(
                            listOf("general", "wifi", "bluetooth", "display", "sound", "apps", "battery"),
                            "Which Android settings page to open."
                        )
                    ),
                    required = listOf("setting_type")
                )
            )
            put(
                fn(
                    name = "set_volume",
                    description = "Adjust Android device audio volume percentage for a stream.",
                    properties = mapOf(
                        "stream" to enumProp(listOf("media", "ring", "alarm", "notification"), "Audio stream to adjust."),
                        "level_percent" to prop("integer", "Target volume percentage from 0 to 100.")
                    ),
                    required = listOf("stream", "level_percent")
                )
            )
            put(
                fn(
                    name = "set_brightness",
                    description = "Adjust Android screen brightness percentage (requires system write settings permission).",
                    properties = mapOf(
                        "percent" to prop("integer", "Brightness percentage from 5 to 100.")
                    ),
                    required = listOf("percent")
                )
            )
            put(
                fn(
                    name = "toggle_flashlight",
                    description = "Turn the device camera flashlight/torch on or off.",
                    properties = mapOf(
                        "enabled" to prop("boolean", "True to switch flashlight on, false to switch off.")
                    ),
                    required = listOf("enabled")
                )
            )
            put(
                fn(
                    name = "set_alarm",
                    description = "Create an alarm using Android AlarmClock.",
                    properties = mapOf(
                        "hour" to prop("integer", "Hour in 24-hour format (0-23)."),
                        "minute" to prop("integer", "Minute (0-59)."),
                        "label" to prop("string", "Short descriptive alarm label.")
                    ),
                    required = listOf("hour", "minute")
                )
            )
            put(
                fn(
                    name = "set_timer",
                    description = "Start a countdown timer using Android AlarmClock.",
                    properties = mapOf(
                        "seconds" to prop("integer", "Duration of the timer in seconds."),
                        "label" to prop("string", "Short label for the timer.")
                    ),
                    required = listOf("seconds")
                )
            )
            put(
                fn(
                    name = "create_note",
                    description = "Save a persistent note into XAVION's local Notes vault.",
                    properties = mapOf(
                        "title" to prop("string", "Note title."),
                        "content" to prop("string", "Full note content."),
                        "tag" to prop("string", "Optional category tag such as Work, Personal, Idea.")
                    ),
                    required = listOf("title", "content")
                )
            )
            put(
                fn(
                    name = "create_task",
                    description = "Create a persistent to-do task or reminder in XAVION's Tasks hub.",
                    properties = mapOf(
                        "title" to prop("string", "Task or reminder title."),
                        "due_label" to prop("string", "Optional due date/time text like 'Today 5:00 PM'."),
                        "priority" to enumProp(listOf("Low", "Normal", "High"), "Task priority.")
                    ),
                    required = listOf("title")
                )
            )
            put(
                fn(
                    name = "save_memory",
                    description = "Save a user preference, personal fact, or standing instruction into XAVION's persistent memory.",
                    properties = mapOf(
                        "category" to enumProp(listOf("Preference", "Fact", "Instruction"), "Category of memory."),
                        "title" to prop("string", "Short label for the memory."),
                        "content" to prop("string", "The fact or preference to remember.")
                    ),
                    required = listOf("category", "title", "content")
                )
            )
            put(
                fn(
                    name = "read_notifications",
                    description = "Read recent active device notifications if Notification Listener permission is granted.",
                    properties = mapOf(
                        "limit" to prop("integer", "Maximum number of notifications to return (default 5).")
                    ),
                    required = emptyList()
                )
            )
            put(
                fn(
                    name = "share_text",
                    description = "Share text via the Android system share sheet.",
                    properties = mapOf(
                        "title" to prop("string", "Share sheet title or subject."),
                        "text" to prop("string", "Text payload to share.")
                    ),
                    required = listOf("text")
                )
            )
            put(
                fn(
                    name = "open_website",
                    description = "Open an HTTPS website in the device's web browser.",
                    properties = mapOf(
                        "url" to prop("string", "Valid https:// URL to open.")
                    ),
                    required = listOf("url")
                )
            )
            put(
                fn(
                    name = "create_calendar_event",
                    description = "Create a calendar event using Android Calendar.",
                    properties = mapOf(
                        "title" to prop("string", "Event title."),
                        "description" to prop("string", "Event description or notes."),
                        "minutes_from_now" to prop("integer", "Start time offset in minutes from now (default 60).")
                    ),
                    required = listOf("title")
                )
            )
            put(
                fn(
                    name = "get_device_status",
                    description = "Inspect live battery percentage, charging state, network status, media volume, and torch state.",
                    properties = emptyMap(),
                    required = emptyList()
                )
            )
        }

        return JSONArray().apply {
            put(JSONObject().put("functionDeclarations", declarations))
        }
    }

    private fun fn(
        name: String,
        description: String,
        properties: Map<String, JSONObject>,
        required: List<String>
    ): JSONObject {
        val propsObj = JSONObject()
        properties.forEach { (k, v) -> propsObj.put(k, v) }
        val params = JSONObject()
            .put("type", "OBJECT")
            .put("properties", propsObj)
        if (required.isNotEmpty()) {
            params.put("required", JSONArray(required))
        }
        return JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", params)
    }

    private fun prop(type: String, description: String): JSONObject =
        JSONObject()
            .put("type", type.uppercase())
            .put("description", description)

    private fun enumProp(values: List<String>, description: String): JSONObject =
        JSONObject()
            .put("type", "STRING")
            .put("enum", JSONArray(values))
            .put("description", description)
}
