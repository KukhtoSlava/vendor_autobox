package com.autobox.autoboxlauncher

import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.autobox.autoboxlauncher.call.CallRepository
import com.autobox.autoboxlauncher.call.CallState
import com.autobox.autoboxlauncher.ui.*
import com.autobox.autoboxlauncher.ui.theme.AutoBoxLauncherTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // AppCompat 1.7.0 on Android 16 wraps the base context with ContextThemeWrapper(base,
    // Theme_AppCompat_Empty) and then calls ResourcesCompat.ThemeCompat.rebase() on it.
    // On API 36 this breaks the theme chain and causes hasValue(windowActionBar) to fail.
    // Wrapping here first makes AppCompatDelegateImpl.attachBaseContext2() take its early
    // ContextThemeWrapper return path, bypassing the empty-theme wrapping entirely.
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(ContextThemeWrapper(newBase, R.style.Theme_AutoBoxLauncher))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        detectMediaApp()

        setContent {
            val brightness = viewModel.brightness.value
            SideEffect {
                val attrs = window.attributes
                attrs.screenBrightness = brightness
                window.attributes = attrs
            }

            val appTheme = viewModel.theme.value
            AutoBoxLauncherTheme(appTheme = appTheme) {
                val isDark = when (appTheme) {
                    AppTheme.LIGHT -> false
                    AppTheme.DARK -> true
                    AppTheme.AUTO -> isSystemInDarkTheme()
                }
                BackHandler(enabled = true) {}

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val callState by CallRepository.callState.collectAsState()

                // Retain last known states for exit animations
                var lastIncoming by remember { mutableStateOf<CallState.Incoming?>(null) }
                if (callState is CallState.Incoming) lastIncoming = callState as CallState.Incoming

                var lastActive by remember { mutableStateOf<CallState.Active?>(null) }
                if (callState is CallState.Active) lastActive = callState as CallState.Active

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0),
                        bottomBar = { BottomNavigationBar(navController) }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route,
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None },
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) { HomeScreen(viewModel, isDark) }
                            composable(Screen.Map.route) { MapScreen(viewModel, isDark) }
                            composable(Screen.Multimedia.route) { MultimediaScreen(viewModel) }
                            composable(Screen.Calls.route) { CallScreen() }
                            composable(Screen.Settings.route) {
                                SettingsScreen(viewModel, onExit = ::exitLauncher)
                            }
                        }
                    }

                    // Incoming call overlay — appears over any screen
                    AnimatedVisibility(
                        visible = callState is CallState.Incoming,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { -it / 2 },
                        exit  = fadeOut(tween(200)) + slideOutVertically(tween(250)) { -it / 2 },
                    ) {
                        lastIncoming?.let { IncomingCallOverlay(it) }
                    }

                    // Active call banner — top strip when not on Calls screen
                    AnimatedVisibility(
                        visible = callState is CallState.Active &&
                                  currentRoute != Screen.Calls.route,
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                        enter = fadeIn(tween(200)) + slideInVertically(tween(300)) { -it },
                        exit  = fadeOut(tween(150)) + slideOutVertically(tween(200)) { -it },
                    ) {
                        lastActive?.let {
                            ActiveCallBanner(
                                state   = it,
                                onClick = {
                                    navController.navigate(Screen.Calls.route) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.attachMedia()
    }

    override fun onPause() {
        super.onPause()
        viewModel.detachMedia()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun exitLauncher() {
        viewModel.stopNavigation()
        viewModel.clearNavigationUi()
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
        finish()
    }

    private fun detectMediaApp() {
        if (viewModel.selectedMediaApp.value != null) return
        // Auto-select the first installed MediaBrowserService app.
        val first = viewModel.installedMediaApps.value.firstOrNull() ?: return
        viewModel.setMediaApp(first)
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Home,
        Screen.Map,
        Screen.Multimedia,
        Screen.Calls,
        Screen.Settings
    )
    NavigationBar(
        modifier = Modifier.height(52.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0.dp)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label,
                        modifier = Modifier.size(34.dp)
                    )
                },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    indicatorColor = Color.Transparent
                ),
                alwaysShowLabel = false
            )
        }
    }
}
