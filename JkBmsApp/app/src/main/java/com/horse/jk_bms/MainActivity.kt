package com.horse.jk_bms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.horse.jk_bms.ui.navigation.AppNavHost
import com.horse.jk_bms.ui.theme.JkBmsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.horse.jk_bms.repository.BmsRepository
import com.horse.jk_bms.monitoring.BackgroundMonitorController

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var repository: BmsRepository
    @Inject lateinit var background: BackgroundMonitorController

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !background.enabled.value) {
            repository.requestDisconnect()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JkBmsTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
