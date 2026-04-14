package com.horse.jk_bms.ui.navigation

sealed class Screen(val route: String) {
    data object Connection : Screen("connection")
    data object Dashboard : Screen("dashboard")
    data object Cells : Screen("cells")
    data object Settings : Screen("settings")
    data object DeviceInfo : Screen("device_info")
    data object Faults : Screen("faults")
    data object Logs : Screen("logs")
}
