package com.horse.jk_bms.monitoring

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundMonitorController @Inject constructor(@ApplicationContext private val context: Context) {
    private val mutableEnabled = MutableStateFlow(false)
    val enabled = mutableEnabled.asStateFlow()

    fun start(): Result<Unit> = runCatching {
        mutableEnabled.value = true
        try { ContextCompat.startForegroundService(context, Intent(context, MonitoringService::class.java)) }
        catch (error: Exception) { mutableEnabled.value = false; throw error }
    }

    fun stopBackground() {
        mutableEnabled.value = false
        context.stopService(Intent(context, MonitoringService::class.java))
    }

    fun stopped() { mutableEnabled.value = false }
}
