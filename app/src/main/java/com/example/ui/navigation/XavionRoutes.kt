package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

sealed interface XavionRoute {
    @Serializable data object Home : XavionRoute
    @Serializable data object LiveVoice : XavionRoute
    @Serializable data object TextChat : XavionRoute
    @Serializable data object Vision : XavionRoute
    @Serializable data object DeviceControl : XavionRoute
    @Serializable data object Memory : XavionRoute
    @Serializable data object Tasks : XavionRoute
    @Serializable data object AiSettings : XavionRoute
    @Serializable data object ApiConfig : XavionRoute
    @Serializable data object Permissions : XavionRoute
    @Serializable data object About : XavionRoute
}

data class NavDestinationItem(
    val route: XavionRoute,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val showInCompactBar: Boolean = false
)

val XavionNavItems = listOf(
    NavDestinationItem(XavionRoute.Home, "Home", Icons.Default.Home, "nav_home", showInCompactBar = true),
    NavDestinationItem(XavionRoute.LiveVoice, "Live", Icons.Default.GraphicEq, "nav_live", showInCompactBar = true),
    NavDestinationItem(XavionRoute.TextChat, "Chat", Icons.Default.Chat, "nav_chat", showInCompactBar = true),
    NavDestinationItem(XavionRoute.Vision, "Vision", Icons.Default.CameraAlt, "nav_vision", showInCompactBar = true),
    NavDestinationItem(XavionRoute.DeviceControl, "Device", Icons.Default.Devices, "nav_device"),
    NavDestinationItem(XavionRoute.Memory, "Memory", Icons.Default.Psychology, "nav_memory"),
    NavDestinationItem(XavionRoute.Tasks, "Tasks", Icons.Default.Checklist, "nav_tasks", showInCompactBar = true),
    NavDestinationItem(XavionRoute.AiSettings, "AI Config", Icons.Default.Tune, "nav_ai_settings"),
    NavDestinationItem(XavionRoute.ApiConfig, "API Key", Icons.Default.Key, "nav_api_config"),
    NavDestinationItem(XavionRoute.Permissions, "Perms", Icons.Default.Security, "nav_permissions"),
    NavDestinationItem(XavionRoute.About, "About", Icons.Default.Info, "nav_about")
)
