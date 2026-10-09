package com.horse.jk_bms.di

import android.content.Context
import android.hardware.usb.UsbManager
import com.horse.jk_bms.usb.BmsTransport
import com.horse.jk_bms.usb.UsbSerialManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UsbModule {
    @Provides
    fun provideTransport(manager: UsbSerialManager): BmsTransport = manager

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager {
        return context.getSystemService(Context.USB_SERVICE) as UsbManager
    }
}
