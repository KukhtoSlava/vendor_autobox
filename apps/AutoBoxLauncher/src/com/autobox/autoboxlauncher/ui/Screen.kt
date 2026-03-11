package com.autobox.autoboxlauncher.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.autobox.autoboxlauncher.ui.components.LauncherIcons

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", LauncherIcons.Home, "Home")
    object Map : Screen("map", LauncherIcons.Navigation, "Map")
    object Multimedia : Screen("multimedia", LauncherIcons.Music, "Multimedia")
    object Calls : Screen("calls", LauncherIcons.Phone, "Calls")
    object Settings : Screen("settings", LauncherIcons.Settings, "Settings")
}
