package com.horse.jk_bms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.horse.jk_bms.ui.navigation.AppNavHost
import com.horse.jk_bms.ui.theme.JkBmsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
