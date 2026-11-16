package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveNotificationItem(
    val key: String,
    val appName: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class XavionNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _notifications = MutableStateFlow<List<ActiveNotificationItem>>(emptyList())
        val notifications: StateFlow<List<ActiveNotificationItem>> = _notifications.asStateFlow()

        @Volatile
        private var instance: XavionNotificationListenerService? = null

        fun refreshNow() {
            instance?.syncActiveNotifications()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isConnected.value = true
        syncActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isConnected.value = false
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        syncActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        syncActiveNotifications()
    }

    private fun syncActiveNotifications() {
        try {
            val active = activeNotifications ?: emptyArray()
            val pm = packageManager
            val mapped = active
                .filter { it.packageName != packageName }
                .mapNotNull { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
                    if (title.isBlank() && text.isBlank()) return@mapNotNull null
                    val appLabel = runCatching {
                        val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    }.getOrDefault(sbn.packageName)
                    ActiveNotificationItem(
                        key = sbn.key,
                        appName = appLabel,
                        packageName = sbn.packageName,
                        title = title,
                        text = text,
                        postTime = sbn.postTime
                    )
                }
                .sortedByDescending { it.postTime }
            _notifications.value = mapped
        } catch (_: Exception) {
            // Permission may have been revoked mid-session
        }
    }
}
