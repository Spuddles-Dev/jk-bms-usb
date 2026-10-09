package com.horse.jk_bms.usb

import android.app.Application
import android.app.PendingIntent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Looper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [25, 35])
class UsbPermissionRegressionTest {
    @Before fun grantReceiverPermission() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions("${application.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
    }

    @Test fun testImmutableCallbackReadsAuthoritativeGrantState() = runBlocking {
        for (grant in listOf(false, true)) {
            val device = mockk<UsbDevice>()
            every { device.deviceId } returns 42
            val manager = mockk<UsbManager>()
            var permitted = false
            every { manager.hasPermission(device) } answers { permitted }
            every { manager.requestPermission(device, any()) } answers {
                permitted = grant
                secondArg<PendingIntent>().send()
                shadowOf(Looper.getMainLooper()).idle()
            }
            val requester = UsbPermissionRequester(RuntimeEnvironment.getApplication(), manager)
            assertEquals(grant, requester.awaitPermission(device))
        }
    }

    @Test fun testExistingGrantSkipsDialog() = runBlocking {
        val device = mockk<UsbDevice>()
        val manager = mockk<UsbManager>()
        every { manager.hasPermission(device) } returns true
        assertTrue(UsbPermissionRequester(RuntimeEnvironment.getApplication(), manager).awaitPermission(device))
        verify(exactly = 0) { manager.requestPermission(any<UsbDevice>(), any<PendingIntent>()) }
    }
}
