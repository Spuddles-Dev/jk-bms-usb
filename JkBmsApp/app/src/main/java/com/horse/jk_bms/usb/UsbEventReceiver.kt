package com.horse.jk_bms.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class UsbEvent {
    data class DeviceAttached(val device: UsbDevice) : UsbEvent()
    data class DeviceDetached(val device: UsbDevice) : UsbEvent()
}

@Singleton
class UsbEventReceiver @Inject constructor() {
    private val mutableEvents = MutableSharedFlow<UsbEvent>(extraBufferCapacity = 16)
    val events = mutableEvents.asSharedFlow()
    private var receiver: BroadcastReceiver? = null

    fun register(context: Context) {
        if (receiver != null) return
        val created = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device = IntentCompat.getParcelableExtra(intent, UsbManager.EXTRA_DEVICE, UsbDevice::class.java) ?: return
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> mutableEvents.tryEmit(UsbEvent.DeviceAttached(device))
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> mutableEvents.tryEmit(UsbEvent.DeviceDetached(device))
                }
            }
        }
        ContextCompat.registerReceiver(
            context, created, IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiver = created
    }
}
