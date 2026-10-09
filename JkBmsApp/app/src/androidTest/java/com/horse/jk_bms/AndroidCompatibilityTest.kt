package com.horse.jk_bms

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.core.content.FileProvider
import android.os.SystemClock
import com.horse.jk_bms.data.backup.ConfigBackupStore
import com.horse.jk_bms.diagnostics.ProtocolTrace
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.viewmodel.DiagnosticsViewModel
import kotlinx.coroutines.runBlocking
import java.io.File
import com.horse.jk_bms.data.local.Converters
import com.horse.jk_bms.data.local.DatabaseMigrations
import com.horse.jk_bms.data.local.JkBmsDatabase
import com.horse.jk_bms.protocol.FrameCode
import com.horse.jk_bms.protocol.FrameEncoder
import com.horse.jk_bms.protocol.FrameStreamDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidCompatibilityTest {
    @Test fun testCaptureImportReplaysWithoutUsbConnection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, "synthetic-demo.json")
        instrumentation.context.assets.open("fixtures/demo-runtime-capture.json").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val viewModel = DiagnosticsViewModel(ProtocolTrace())
        instrumentation.runOnMainSync { viewModel.replay(context, uri) }
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (viewModel.state.value.data?.soc != 78 && viewModel.state.value.error == null &&
            SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(50)
        assertNull(viewModel.state.value.error)
        assertEquals(78, viewModel.state.value.data?.soc)
        assertEquals(16, viewModel.state.value.data?.activeCellCount)
        instrumentation.runOnMainSync { viewModel.stop() }
    }

    @Test fun testBackupRoundTripRequiresMatchingFirmware() = runBlocking {
        val store = ConfigBackupStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val device = BmsDeviceInfo(deviceSN = "instrumentation-pack", hardwareVersion = "test", softwareVersion = "test-1")
        val config = BmsConfig(volCellOV = 3.3f, cellConWireRes = FloatArray(32) { it * 0.001f })
        val file = store.save(device, config, "Before balancing change")
        assertEquals("Before balancing change", store.list(device).first().name)
        assertEquals(config, store.load(device, file.nameWithoutExtension))
        assertEquals(config, store.latest(device))
        assertNull(store.latest(device.copy(softwareVersion = "other")))
    }

    @get:Rule
    val migration = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        JkBmsDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test fun testLegacyBase64AndFragmentedFrames() {
        assertEquals("AAECA/8=", Converters.fromByteArray(byteArrayOf(0, 1, 2, 3, -1)))
        assertTrue(byteArrayOf(0, 1, 2, 3, -1).contentEquals(Converters.toByteArray("AAECA/8=")))
        val frame = FrameEncoder.buildQuery(FrameCode.RUNTIME_DATA, 7)
        val stream = FrameStreamDecoder()
        assertTrue(stream.append(frame.copyOfRange(0, 17)).isEmpty())
        assertEquals(7, stream.append(frame.copyOfRange(17, frame.size)).single().counter)
    }

    @Test fun testVersionOneDatabaseMigratesWithValidatedSchema() {
        migration.createDatabase("migration-test", 1).close()
        migration.runMigrationsAndValidate("migration-test", 2, true, DatabaseMigrations.FROM_1_TO_2).use {
            it.query("SELECT COUNT(*) FROM sessions").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test fun testApplicationStartsWithoutUsbHardware() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> assertTrue(!activity.isFinishing) }
        }
    }
}
