package com.horse.jk_bms.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class UsbPermissionRequester @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: UsbManager,
) {
    suspend fun awaitPermission(device: UsbDevice): Boolean {
        if (manager.hasPermission(device)) return true
        val action = "${context.packageName}.USB_PERMISSION.${device.deviceId}"
        var registered: BroadcastReceiver? = null
        try {
            return withTimeout(30_000) {
                suspendCancellableCoroutine { continuation ->
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (!continuation.isActive) return
                            if (intent.action == action) {
                                continuation.resume(manager.hasPermission(device))
                                return
                            }
                            val target = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                            if (target?.deviceId != device.deviceId) return
                            if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                                continuation.resume(false)
                            }
                        }
                    }
                    val filter = IntentFilter(action).apply { addAction(UsbManager.ACTION_USB_DEVICE_DETACHED) }
                    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                    registered = receiver
                    val intent = PendingIntent.getBroadcast(
                        context, device.deviceId, Intent(action).setPackage(context.packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    manager.requestPermission(device, intent)
                }
            }
        } finally {
            registered?.let { context.unregisterReceiver(it) }
        }
    }
}
