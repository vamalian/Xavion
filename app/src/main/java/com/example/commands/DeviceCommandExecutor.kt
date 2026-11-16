package com.example.commands

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.local.MemoryRepository
import com.example.data.local.ProductivityRepository
import com.example.data.model.DeviceTelemetry
import com.example.data.model.PendingCommandConfirmation
import com.example.data.preferences.SettingsRepository
import com.example.service.XavionNotificationListenerService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class InstalledAppInfo(
    val label: String,
    val packageName: String
)

data class CalendarAgendaItem(
    val title: String,
    val startTimeMillis: Long,
    val location: String
)

class DeviceCommandExecutor(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val memoryRepository: MemoryRepository,
    private val productivityRepository: ProductivityRepository
) {

    private val _flashlightOn = MutableStateFlow(false)
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingCommandConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingCommandConfirmation?> = _pendingConfirmation.asStateFlow()

    private var confirmationDeferred: CompletableDeferred<Boolean>? = null

    fun resolveConfirmation(approved: Boolean) {
        confirmationDeferred?.complete(approved)
        confirmationDeferred = null
        _pendingConfirmation.value = null
    }

    suspend fun executeFromGemini(
        callId: String,
        functionName: String,
        args: JSONObject
    ): JSONObject {
        val settings = settingsRepository.current()
        if (settings.requireConfirmationForSensitiveActions &&
            functionName in DeviceCommandRegistry.sensitiveFunctions
        ) {
            val summary = summarizeAction(functionName, args)
            val deferred = CompletableDeferred<Boolean>()
            confirmationDeferred = deferred
            _pendingConfirmation.value = PendingCommandConfirmation(
                callId = callId,
                functionName = functionName,
                summary = summary,
                details = args.toString(2),
                argumentsJson = args.toString()
            )
            val approved = deferred.await()
            if (!approved) {
                productivityRepository.logToolExecution(
                    functionName = functionName,
                    argumentsSummary = summary,
                    status = "DENIED",
                    resultMessage = "User declined action confirmation."
                )
                return JSONObject()
                    .put("status", "cancelled_by_user")
                    .put("message", "The user declined to execute $functionName.")
            }
        }

        val result = executeInternal(functionName, args)
        productivityRepository.logToolExecution(
            functionName = functionName,
            argumentsSummary = summarizeAction(functionName, args),
            status = if (result.optString("status") == "error") "FAILED" else "EXECUTED",
            resultMessage = result.optString("message", result.toString())
        )
        return result
    }

    suspend fun executeDirect(functionName: String, args: JSONObject = JSONObject()): JSONObject {
        val result = executeInternal(functionName, args)
        productivityRepository.logToolExecution(
            functionName = functionName,
            argumentsSummary = summarizeAction(functionName, args),
            status = if (result.optString("status") == "error") "FAILED" else "EXECUTED",
            resultMessage = result.optString("message", result.toString())
        )
        return result
    }

    private suspend fun executeInternal(functionName: String, args: JSONObject): JSONObject {
        return when (functionName) {
            "open_app" -> openApp(args.optString("app_name"))
            "open_settings" -> openSettings(args.optString("setting_type", "general"))
            "set_volume" -> setVolume(
                stream = args.optString("stream", "media"),
                percent = args.optInt("level_percent", 50)
            )
            "set_brightness" -> setBrightness(args.optInt("percent", 70))
            "toggle_flashlight" -> toggleFlashlight(args.optBoolean("enabled", !_flashlightOn.value))
            "set_alarm" -> setAlarm(
                hour = args.optInt("hour", 7),
                minute = args.optInt("minute", 0),
                label = args.optString("label", "Xavion Alarm")
            )
            "set_timer" -> setTimer(
                seconds = args.optInt("seconds", 60),
                label = args.optString("label", "Xavion Timer")
            )
            "create_note" -> {
                val title = args.optString("title").ifBlank { "Xavion Note" }
                val content = args.optString("content")
                val tag = args.optString("tag", "General")
                val id = productivityRepository.addNote(title, content, tag)
                JSONObject()
                    .put("status", "ok")
                    .put("note_id", id)
                    .put("message", "Saved note '$title'.")
            }
            "create_task" -> {
                val title = args.optString("title").ifBlank { "Untitled Task" }
                val due = args.optString("due_label", "")
                val priority = args.optString("priority", "Normal")
                val id = productivityRepository.addTask(title = title, dueLabel = due, priority = priority)
                JSONObject()
                    .put("status", "ok")
                    .put("task_id", id)
                    .put("message", "Added task '$title' ($priority priority).")
            }
            "save_memory" -> {
                if (!settingsRepository.current().memoryEnabled) {
                    JSONObject()
                        .put("status", "disabled")
                        .put("message", "Persistent memory is currently disabled in AI Settings.")
                } else {
                    val category = args.optString("category", "Fact")
                    val title = args.optString("title").ifBlank { "Saved Detail" }
                    val content = args.optString("content")
                    memoryRepository.addOrSave(category, title, content)
                    JSONObject()
                        .put("status", "ok")
                        .put("message", "Stored '$title' in persistent memory.")
                }
            }
            "read_notifications" -> readNotifications(args.optInt("limit", 5))
            "share_text" -> shareText(args.optString("title", "Shared from Xavion AI"), args.optString("text"))
            "open_website" -> openWebsite(args.optString("url"))
            "create_calendar_event" -> createCalendarEvent(
                title = args.optString("title", "Xavion Event"),
                description = args.optString("description", ""),
                minutesFromNow = args.optInt("minutes_from_now", 60)
            )
            "get_device_status" -> {
                val t = readDeviceTelemetry()
                JSONObject()
                    .put("status", "ok")
                    .put("battery_percent", t.batteryPercent)
                    .put("is_charging", t.isCharging)
                    .put("network_available", t.networkAvailable)
                    .put("network_type", t.networkType)
                    .put("media_volume_percent", t.mediaVolumePercent)
                    .put("flashlight_on", t.flashlightOn)
                    .put("message", "Battery ${t.batteryPercent}%, ${t.networkType}, media volume ${t.mediaVolumePercent}%.")
            }
            else -> JSONObject()
                .put("status", "error")
                .put("message", "Unsupported command: $functionName")
        }
    }

    fun getInstalledLaunchableApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolved
            .map { info ->
                InstalledAppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun launchAppByPackage(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }

    private fun openApp(query: String): JSONObject {
        if (query.isBlank()) {
            return JSONObject().put("status", "error").put("message", "No application name specified.")
        }
        val apps = getInstalledLaunchableApps()
        val match = apps.firstOrNull { it.label.equals(query, ignoreCase = true) }
            ?: apps.firstOrNull { it.label.contains(query, ignoreCase = true) }
            ?: apps.firstOrNull { it.packageName.contains(query, ignoreCase = true) }

        return if (match != null && launchAppByPackage(match.packageName)) {
            JSONObject()
                .put("status", "ok")
                .put("app", match.label)
                .put("message", "Launched ${match.label}.")
        } else {
            JSONObject()
                .put("status", "error")
                .put("message", "Application '$query' is not installed or cannot be launched.")
        }
    }

    fun openSettings(type: String): JSONObject {
        val action = when (type.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "battery" -> Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            JSONObject().put("status", "ok").put("message", "Opened Android $type settings.")
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "Failed to open $type settings: ${e.message}")
        }
    }

    fun setVolume(stream: String, percent: Int): JSONObject {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val streamType = when (stream.lowercase()) {
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            else -> AudioManager.STREAM_MUSIC
        }
        return try {
            val max = audioManager.getStreamMaxVolume(streamType)
            val clamped = percent.coerceIn(0, 100)
            val target = ((clamped / 100f) * max).roundToInt().coerceIn(0, max)
            audioManager.setStreamVolume(streamType, target, AudioManager.FLAG_SHOW_UI)
            JSONObject()
                .put("status", "ok")
                .put("stream", stream)
                .put("level_percent", clamped)
                .put("message", "Set $stream volume to $clamped%.")
        } catch (e: SecurityException) {
            JSONObject()
                .put("status", "error")
                .put("message", "Do Not Disturb permission is required to adjust $stream volume.")
        }
    }

    fun setBrightness(percent: Int): JSONObject {
        if (!Settings.System.canWrite(context)) {
            return JSONObject()
                .put("status", "permission_missing")
                .put("message", "Modify System Settings permission is not granted. Please enable it in Permissions.")
        }
        val clamped = percent.coerceIn(5, 100)
        val value = ((clamped / 100f) * 255f).roundToInt().coerceIn(10, 255)
        return try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            JSONObject()
                .put("status", "ok")
                .put("brightness_percent", clamped)
                .put("message", "Screen brightness adjusted to $clamped%.")
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "Failed to set brightness: ${e.message}")
        }
    }

    fun toggleFlashlight(enabled: Boolean): JSONObject {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val torchCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (torchCameraId == null) {
                JSONObject()
                    .put("status", "error")
                    .put("message", "This device does not report a controllable camera flashlight.")
            } else {
                cameraManager.setTorchMode(torchCameraId, enabled)
                _flashlightOn.value = enabled
                JSONObject()
                    .put("status", "ok")
                    .put("flashlight_on", enabled)
                    .put("message", if (enabled) "Flashlight turned on." else "Flashlight turned off.")
            }
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "Flashlight control failed: ${e.message}")
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String): JSONObject {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, h)
                putExtra(AlarmClock.EXTRA_MINUTES, m)
                putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "Xavion Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JSONObject()
                .put("status", "ok")
                .put("message", "Opened alarm creation for %02d:%02d (%s).".format(h, m, label))
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "No compatible Clock app found: ${e.message}")
        }
    }

    fun setTimer(seconds: Int, label: String): JSONObject {
        val s = seconds.coerceAtLeast(1)
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, s)
                putExtra(AlarmClock.EXTRA_MESSAGE, label.ifBlank { "Xavion Timer" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JSONObject()
                .put("status", "ok")
                .put("message", "Started timer request for $s seconds ($label).")
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "No compatible Timer app found: ${e.message}")
        }
    }

    private fun readNotifications(limit: Int): JSONObject {
        if (!XavionNotificationListenerService.isConnected.value) {
            return JSONObject()
                .put("status", "permission_missing")
                .put("message", "Notification Access is not granted for Xavion AI. Enable it in Permissions.")
        }
        XavionNotificationListenerService.refreshNow()
        val items = XavionNotificationListenerService.notifications.value.take(limit.coerceIn(1, 15))
        val array = JSONArray()
        items.forEach { n ->
            array.put(
                JSONObject()
                    .put("app", n.appName)
                    .put("title", n.title)
                    .put("text", n.text)
            )
        }
        return JSONObject()
            .put("status", "ok")
            .put("count", items.size)
            .put("notifications", array)
            .put("message", if (items.isEmpty()) "No active notifications." else "Retrieved ${items.size} notifications.")
    }

    fun shareText(title: String, text: String): JSONObject {
        if (text.isBlank()) {
            return JSONObject().put("status", "error").put("message", "Nothing to share.")
        }
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            JSONObject().put("status", "ok").put("message", "Opened Android Share sheet.")
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "Share failed: ${e.message}")
        }
    }

    fun openWebsite(rawUrl: String): JSONObject {
        val normalized = when {
            rawUrl.startsWith("https://", ignoreCase = true) -> rawUrl
            rawUrl.startsWith("http://", ignoreCase = true) -> rawUrl
            rawUrl.contains(".") -> "https://$rawUrl"
            else -> return JSONObject().put("status", "error").put("message", "Invalid URL: $rawUrl")
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            JSONObject().put("status", "ok").put("message", "Opened $normalized in browser.")
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "Cannot open website: ${e.message}")
        }
    }

    fun createCalendarEvent(title: String, description: String, minutesFromNow: Int): JSONObject {
        val startMillis = System.currentTimeMillis() + minutesFromNow.coerceAtLeast(1) * 60_000L
        val endMillis = startMillis + 3_600_000L
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            JSONObject().put("status", "ok").put("message", "Opened Calendar event composer for '$title'.")
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", "Unable to open Calendar: ${e.message}")
        }
    }

    fun readUpcomingCalendarEvents(limit: Int = 5): List<CalendarAgendaItem> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val items = mutableListOf<CalendarAgendaItem>()
        val now = System.currentTimeMillis()
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.EVENT_LOCATION
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ?"
        val args = arrayOf(now.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                args,
                sortOrder
            )?.use { cursor ->
                while (cursor.moveToNext() && items.size < limit) {
                    val title = cursor.getString(0) ?: "Untitled Event"
                    val start = cursor.getLong(1)
                    val location = cursor.getString(2).orEmpty()
                    items.add(CalendarAgendaItem(title, start, location))
                }
            }
        }
        return items
    }

    fun readDeviceTelemetry(): DeviceTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else 100
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNet = cm.activeNetwork
        val caps = activeNet?.let { cm.getNetworkCapabilities(it) }
        val netAvailable = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val netType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Connected"
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val volPct = ((curVol.toFloat() / maxVol) * 100f).roundToInt()

        return DeviceTelemetry(
            batteryPercent = pct,
            isCharging = charging,
            networkAvailable = netAvailable,
            networkType = netType,
            mediaVolumePercent = volPct,
            flashlightOn = _flashlightOn.value
        )
    }

    private fun summarizeAction(functionName: String, args: JSONObject): String =
        when (functionName) {
            "open_app" -> "Open app '${args.optString("app_name")}'"
            "open_settings" -> "Open ${args.optString("setting_type")} settings"
            "set_volume" -> "Set ${args.optString("stream")} volume to ${args.optInt("level_percent")}%"
            "set_brightness" -> "Set brightness to ${args.optInt("percent")}%"
            "toggle_flashlight" -> if (args.optBoolean("enabled")) "Enable flashlight" else "Disable flashlight"
            "set_alarm" -> "Set alarm ${args.optInt("hour")}:${args.optInt("minute")}"
            "set_timer" -> "Set timer for ${args.optInt("seconds")}s"
            "create_note" -> "Create note '${args.optString("title")}'"
            "create_task" -> "Create task '${args.optString("title")}'"
            "save_memory" -> "Save memory '${args.optString("title")}'"
            "open_website" -> "Open website ${args.optString("url")}"
            "share_text" -> "Share text via Android chooser"
            "create_calendar_event" -> "Schedule '${args.optString("title")}'"
            else -> functionName
        }
}
