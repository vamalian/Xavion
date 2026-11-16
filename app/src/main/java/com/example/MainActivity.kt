package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.CommandConfirmationDialog
import com.example.ui.components.ConnectionStatusPill
import com.example.ui.components.HolographicBackground
import com.example.ui.navigation.XavionNavItems
import com.example.ui.navigation.XavionRoute
import com.example.ui.screens.AboutSecurityScreen
import com.example.ui.screens.AiSettingsScreen
import com.example.ui.screens.ApiConfigScreen
import com.example.ui.screens.DeviceControlScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveVoiceScreen
import com.example.ui.screens.LockGateScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TextChatScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.theme.XavionBorderSubtle
import com.example.ui.theme.XavionCyan
import com.example.ui.theme.XavionObsidian
import com.example.ui.theme.XavionPanelDark
import com.example.ui.theme.XavionTextMuted
import com.example.ui.theme.XavionTextPrimary
import com.example.ui.theme.XavionTheme
import com.example.ui.viewmodel.XavionViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_OPEN_LIVE_VOICE = "extra_open_live_voice"
        const val EXTRA_AUTO_CONNECT_LIVE = "extra_auto_connect_live"
    }

    private val viewModel: XavionViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openLiveOnStart = intent?.getBooleanExtra(EXTRA_OPEN_LIVE_VOICE, false) == true
        val autoConnectLive = intent?.getBooleanExtra(EXTRA_AUTO_CONNECT_LIVE, false) == true

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
            val lockError by viewModel.lockError.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)

            LaunchedEffect(settings.screenshotProtectionEnabled) {
                if (settings.screenshotProtectionEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LaunchedEffect(autoConnectLive, isLocked) {
                if (autoConnectLive && !isLocked) {
                    viewModel.connectLiveSession()
                }
            }

            XavionTheme {
                if (isLocked) {
                    LockGateScreen(
                        biometricEnabled = settings.biometricUnlockEnabled,
                        errorText = lockError,
                        onUnlockPin = { viewModel.verifyAndUnlockPin(it) },
                        onUnlockBiometric = { viewModel.unlockWithBiometric() }
                    )
                } else {
                    XavionRootShell(
                        viewModel = viewModel,
                        widthSizeClass = windowSizeClass.widthSizeClass,
                        initialOpenLive = openLiveOnStart
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_AUTO_CONNECT_LIVE, false)) {
            viewModel.connectLiveSession()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun XavionRootShell(
    viewModel: XavionViewModel,
    widthSizeClass: WindowWidthSizeClass,
    initialOpenLive: Boolean
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    val liveState by viewModel.liveState.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialOpenLive) {
        if (initialOpenLive) {
            navController.navigate(XavionRoute.LiveVoice) {
                launchSingleTop = true
            }
        }
    }

    val navigateTo: (XavionRoute) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val useNavigationRail = widthSizeClass != WindowWidthSizeClass.Compact

    pendingConfirmation?.let { conf ->
        CommandConfirmationDialog(
            confirmation = conf,
            onApprove = { viewModel.resolveConfirmation(true) },
            onDeny = { viewModel.resolveConfirmation(false) }
        )
    }

    HolographicBackground {
        if (useNavigationRail) {
            // Tablet Landscape & Portrait Medium/Expanded Rail Layout
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Surface(
                        color = XavionObsidian.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, XavionBorderSubtle),
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp)
                    ) {
                        NavigationRail(
                            containerColor = Color.Transparent,
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            header = {
                                Text(
                                    text = "XAVION",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = XavionCyan,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        ) {
                            XavionNavItems.forEach { item ->
                                val selected = currentDest?.hasRoute(item.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    modifier = Modifier.testTag(item.testTag),
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = XavionCyan,
                                        selectedTextColor = XavionCyan,
                                        indicatorColor = XavionCyan.copy(alpha = 0.16f),
                                        unselectedIconColor = XavionTextMuted,
                                        unselectedTextColor = XavionTextMuted
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        XavionNavHost(navController = navController, viewModel = viewModel, navigateTo = navigateTo)
                    }
                }
            }
        } else {
            // Compact screen with Bottom Bar + Drawer for all 11 screens
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = XavionPanelDark,
                        drawerContentColor = XavionTextPrimary
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "XAVION MODULES",
                                style = MaterialTheme.typography.titleLarge,
                                color = XavionCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                            )
                            XavionNavItems.forEach { item ->
                                val selected = currentDest?.hasRoute(item.route::class) == true
                                NavigationDrawerItem(
                                    label = { Text(item.label) },
                                    selected = selected,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigateTo(item.route)
                                    },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = XavionCyan.copy(alpha = 0.16f),
                                        selectedIconColor = XavionCyan,
                                        selectedTextColor = XavionCyan,
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedIconColor = XavionTextMuted,
                                        unselectedTextColor = XavionTextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("XAVION AI", style = MaterialTheme.typography.titleLarge, color = XavionCyan)
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("compact_menu_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open modules menu", tint = XavionCyan)
                                }
                            },
                            actions = {
                                ConnectionStatusPill(state = liveState, modifier = Modifier.padding(end = 10.dp))
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = XavionObsidian.copy(alpha = 0.88f))
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = XavionObsidian.copy(alpha = 0.94f),
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            XavionNavItems.filter { it.showInCompactBar }.forEach { item ->
                                val selected = currentDest?.hasRoute(item.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(item.route) },
                                    modifier = Modifier.testTag(item.testTag),
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = XavionCyan,
                                        selectedTextColor = XavionCyan,
                                        indicatorColor = XavionCyan.copy(alpha = 0.16f),
                                        unselectedIconColor = XavionTextMuted,
                                        unselectedTextColor = XavionTextMuted
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        XavionNavHost(navController = navController, viewModel = viewModel, navigateTo = navigateTo)
                    }
                }
            }
        }
    }
}

@Composable
private fun XavionNavHost(
    navController: androidx.navigation.NavHostController,
    viewModel: XavionViewModel,
    navigateTo: (XavionRoute) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = XavionRoute.Home
    ) {
        composable<XavionRoute.Home> {
            HomeScreen(viewModel = viewModel, onNavigate = navigateTo)
        }
        composable<XavionRoute.LiveVoice> {
            LiveVoiceScreen(
                viewModel = viewModel,
                onOpenApiConfig = { navigateTo(XavionRoute.ApiConfig) }
            )
        }
        composable<XavionRoute.TextChat> {
            TextChatScreen(viewModel = viewModel)
        }
        composable<XavionRoute.Vision> {
            VisionScreen(viewModel = viewModel)
        }
        composable<XavionRoute.DeviceControl> {
            DeviceControlScreen(
                viewModel = viewModel,
                onOpenPermissions = { navigateTo(XavionRoute.Permissions) }
            )
        }
        composable<XavionRoute.Memory> {
            MemoryScreen(viewModel = viewModel)
        }
        composable<XavionRoute.Tasks> {
            TasksScreen(viewModel = viewModel)
        }
        composable<XavionRoute.AiSettings> {
            AiSettingsScreen(viewModel = viewModel)
        }
        composable<XavionRoute.ApiConfig> {
            ApiConfigScreen(viewModel = viewModel)
        }
        composable<XavionRoute.Permissions> {
            PermissionsScreen(viewModel = viewModel)
        }
        composable<XavionRoute.About> {
            AboutSecurityScreen(viewModel = viewModel)
        }
    }
}
