package com.horse.jk_bms.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class UsbEvent {
    data object DeviceAttached : UsbEvent()
    data object DeviceDetached : UsbEvent()
}

@Singleton
class UsbEventReceiver @Inject constructor() {

    private val _events = MutableSharedFlow<UsbEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UsbEvent> = _events.asSharedFlow()

    private var receiver: BroadcastReceiver? = null

    fun register(context: Context) {
        if (receiver != null) return
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        _events.tryEmit(UsbEvent.DeviceAttached)
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        _events.tryEmit(UsbEvent.DeviceDetached)
                    }
                }
            }
        }
        context.registerReceiver(receiver, filter)
    }

    fun unregister(context: Context) {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {
            }
        }
        receiver = null
    }
}
