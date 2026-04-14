package com.horse.jk_bms.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.horse.jk_bms.ui.screen.connection.ConnectionScreen
import com.horse.jk_bms.ui.screen.dashboard.DashboardScreen
import com.horse.jk_bms.ui.screen.cells.CellsScreen
import com.horse.jk_bms.ui.screen.settings.SettingsScreen
import com.horse.jk_bms.ui.screen.device.DeviceInfoScreen
import com.horse.jk_bms.ui.screen.faults.FaultsScreen
import com.horse.jk_bms.ui.screen.logs.LogsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Connection.route,
    ) {
        composable(Screen.Connection.route) {
            ConnectionScreen(
                onConnected = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Connection.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onCellsClick = { navController.navigate(Screen.Cells.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onDeviceInfoClick = { navController.navigate(Screen.DeviceInfo.route) },
                onFaultsClick = { navController.navigate(Screen.Faults.route) },
                onLogsClick = { navController.navigate(Screen.Logs.route) },
                onDisconnect = {
                    navController.navigate(Screen.Connection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Cells.route) {
            CellsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.DeviceInfo.route) {
            DeviceInfoScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Faults.route) {
            FaultsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Logs.route) {
            LogsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
