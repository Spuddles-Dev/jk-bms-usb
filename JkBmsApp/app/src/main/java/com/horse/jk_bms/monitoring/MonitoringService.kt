package com.horse.jk_bms.monitoring

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.horse.jk_bms.MainActivity
import com.horse.jk_bms.connection.SessionState
import com.horse.jk_bms.repository.BmsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {
    @Inject lateinit var repository: BmsRepository
    @Inject lateinit var controller: BackgroundMonitorController
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val alerts = AlertDeduplicator()
    private lateinit var notifications: NotificationManager
    private var monitoring: Job? = null

    override fun onCreate() {
        super.onCreate()
        notifications = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            notifications.createNotificationChannel(NotificationChannel("monitor", "USB monitoring", NotificationManager.IMPORTANCE_LOW))
            notifications.createNotificationChannel(NotificationChannel("alerts", "BMS and connection alerts", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            scope.launch { repository.disconnect(); controller.stopped(); stopSelf() }
            return START_NOT_STICKY
        }
        if (!repository.isConnected.value) {
            controller.stopped()
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(this, 1, status("USB monitoring active"),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0)
        } catch (error: RuntimeException) {
            controller.stopped()
            stopSelf()
            return START_NOT_STICKY
        }
        if (monitoring?.isActive == true) return START_NOT_STICKY
        monitoring = scope.launch {
            combine(repository.runtimeData, repository.sessionState) { data, state -> data to state }.collect { (data, state) ->
                val conditions = buildSet {
                    if (state == SessionState.STALE) add("Telemetry is stale")
                    if (state == SessionState.RECOVERING || state == SessionState.DISCONNECTED) add("USB connection lost")
                    data?.sysAlarm?.forEachIndexed { index, alarm -> if (alarm) add("BMS alarm bit $index") }
                }
                alerts.update(conditions, SystemClock.elapsedRealtime()).forEach { text ->
                    val allowed = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                        this@MonitoringService, Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (allowed) notifications.notify(100 + text.hashCode(), NotificationCompat.Builder(this@MonitoringService, "alerts")
                        .setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("JK-BMS alert")
                        .setContentText(text).setAutoCancel(true).build())
                }
                notifications.notify(1, status("${state.name.lowercase()} • SOC ${data?.soc ?: "—"}%"))
                if (state == SessionState.DISCONNECTED) {
                    controller.stopped()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun status(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, MonitoringService::class.java).setAction("STOP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "monitor").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("JK-BMS USB").setContentText(text).setContentIntent(open).setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop session", stop).build()
    }

    override fun onDestroy() {
        if (controller.enabled.value) repository.requestDisconnect()
        scope.cancel()
        controller.stopped()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
