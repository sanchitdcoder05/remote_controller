package com.san.productivity.tvremotewifibluetooth.ui.navigation

sealed class NavRoutes(val route: String, val title: String) {
    object Home : NavRoutes("home", "Home")
    object Remote : NavRoutes("remote", "Remote")
    object Discovery : NavRoutes("discovery", "TV Discovery")
    object Bluetooth : NavRoutes("bluetooth", "Bluetooth")
    object Settings : NavRoutes("settings", "Settings")
    object Troubleshooting : NavRoutes("troubleshooting", "Troubleshooting")
}
