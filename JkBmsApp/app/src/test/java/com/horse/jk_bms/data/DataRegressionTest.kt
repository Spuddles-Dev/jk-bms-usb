package com.horse.jk_bms.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.horse.jk_bms.data.export.CsvFormatter
import com.horse.jk_bms.data.export.DataExporter
import com.horse.jk_bms.data.export.ExportFormat
import com.horse.jk_bms.data.local.Converters
import com.horse.jk_bms.data.local.DatabaseMigrations
import com.horse.jk_bms.data.local.JkBmsDatabase
import com.horse.jk_bms.data.local.entity.toEntity
import com.horse.jk_bms.data.repository.DataLogRepository
import com.horse.jk_bms.model.BmsDeviceInfo
import com.horse.jk_bms.model.BmsFaultInfo
import com.horse.jk_bms.model.BmsRuntimeData
import com.horse.jk_bms.model.FaultRecord
import com.horse.jk_bms.protocol.validConfig
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [21, 25, 35])
class DataRegressionTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test fun testBase64CompatibleWithLegacyRows() {
        val bytes = ByteArray(256) { it.toByte() }
        val legacy = java.util.Base64.getEncoder().encodeToString(bytes)
        assertEquals(legacy, Converters.fromByteArray(bytes))
        assertTrue(bytes.contentEquals(Converters.toByteArray(legacy)))
        assertFalse(Converters.fromByteArray(bytes).contains("\n"))
        assertEquals("", Converters.fromByteArray(byteArrayOf()))
    }

    @Test fun testLoggingDeduplicatesWithinSessionAndSeparatesDevices() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, JkBmsDatabase::class.java).build()
        try {
            val repository = DataLogRepository(db)
            repository.startSession("one", "adapter-one")
            val faults = BmsFaultInfo(beginIndex = 20, count = 12, records = (0 until 12).map { FaultRecord(rtcCount = it.toLong(), logCode = 1) })
            repeat(100) { repository.logFaultInfo(faults) }
            repeat(10) { repository.logConfig(validConfig()) }
            repository.logDeviceInfo(BmsDeviceInfo(deviceSN = "device-one", bluetoothPwd = "secret", settingPassword = "private"))
            assertEquals(12, repository.getAllFaults().size)
            assertEquals(1, repository.getAllConfigs().size)
            assertEquals("", repository.getLatestDeviceInfo()!!.bluetoothPwd)
            assertEquals("", repository.getLatestDeviceInfo()!!.settingPassword)
            repository.startSession("two", "adapter-two")
            repository.logFaultInfo(faults)
            assertEquals(24, repository.getAllFaults().size)
            assertEquals(2, repository.getAllFaults().map { it.sessionId }.distinct().size)
        } finally { db.close() }
    }

    @Test fun testRepeatedStreamingExportsAndCorrectCellCount() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, JkBmsDatabase::class.java).build()
        val directory = File(context.cacheDir, "test-exports")
        try {
            val repository = DataLogRepository(db)
            repository.startSession("export", "serial,quoted")
            repeat(1001) { index ->
                repository.logRuntimeData(BmsRuntimeData(cellVoltages = FloatArray(32) { if (it < 16) 3.3f else 0f }, celMaxVol = 3), 1000L + index)
            }
            val exporter = DataExporter(repository)
            val first = exporter.exportRuntimeData(0, ExportFormat.CSV, directory)
            val second = exporter.exportRuntimeData(0, ExportFormat.CSV, directory)
            assertFalse(first.name == second.name)
            val lines = first.readLines()
            assertEquals(1002, lines.size)
            val json = exporter.exportRuntimeData(0, ExportFormat.JSON, directory)
            val parsed = JSONObject(json.readText())
            assertEquals(2, parsed.getInt("schema_version"))
            assertEquals(1001, parsed.getJSONArray("rows").length())
            assertEquals(16, parsed.getJSONArray("rows").getJSONObject(0).getInt("active_cells"))
            val row = BmsRuntimeData(cellVoltages = FloatArray(32) { if (it < 16) 3.3f else 0f }, celMaxVol = 3).toEntity()
            assertEquals("16", CsvFormatter.runtimeRow(row).split(',')[10])
        } finally { db.close() }
    }

    @Test fun testRetentionCoversEveryTable() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, JkBmsDatabase::class.java).build()
        try {
            val repository = DataLogRepository(db)
            repository.startSession("retention", "adapter")
            repository.logRuntimeData(BmsRuntimeData(), 1)
            repository.logConfig(validConfig())
            repository.logDeviceInfo(BmsDeviceInfo(deviceSN = "test"))
            repository.logFaultInfo(BmsFaultInfo(records = listOf(FaultRecord())))
            repository.logSystemLog(com.horse.jk_bms.model.BmsSystemLog())
            listOf("runtime_data", "config", "device_info", "fault_record", "system_log").forEach {
                db.openHelper.writableDatabase.execSQL("UPDATE $it SET timestamp = 1")
            }
            repository.cleanup()
            assertEquals(0, repository.getRuntimeDataCount())
            assertTrue(repository.getAllConfigs().isEmpty())
            assertTrue(repository.getAllDeviceInfo().isEmpty())
            assertTrue(repository.getAllFaults().isEmpty())
            assertTrue(repository.getAllSystemLogs().isEmpty())
        } finally { db.close() }
    }

    @Test fun testMigrationPreservesLegacyMeasurementsAndRemovesCredentials() = runBlocking {
        val name = "legacy-migration.db"
        context.deleteDatabase(name)
        val schema = JSONObject(javaClass.getResourceAsStream("/com.horse.jk_bms.data.local.JkBmsDatabase/1.json")!!
            .bufferedReader().readText()).getJSONObject("database")
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                    for (table in listOf("runtime_data", "device_info")) {
                        val entity = (0 until entities.length()).map { entities.getJSONObject(it) }.first { it.getString("tableName") == table }
                        val fields = entity.getJSONArray("fields")
                        val columns = (0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
                        val values: List<Any> = (0 until fields.length()).map {
                            val field = fields.getJSONObject(it)
                            when (field.getString("columnName")) {
                                "id" -> 1
                                "timestamp" -> 1234L
                                "cellVoltages" -> "[3.3,3.4]"
                                "bluetoothPwd", "settingPassword" -> "legacy-secret"
                                else -> if (field.getString("affinity") == "TEXT") "" else 0
                            }
                        }
                        db.execSQL("INSERT INTO $table (${columns.joinToString()}) VALUES (${columns.joinToString { "?" }})", values.toTypedArray())
                    }
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        helper.writableDatabase
        helper.close()
        val db = Room.databaseBuilder(context, JkBmsDatabase::class.java, name).addMigrations(DatabaseMigrations.FROM_1_TO_2).build()
        try {
            assertEquals(1234L, db.runtimeDataDao().getLatest()!!.timestamp)
            assertEquals("[3.3,3.4]", db.runtimeDataDao().getLatest()!!.cellVoltages)
            assertEquals("", db.deviceInfoDao().getLatest()!!.settingPassword)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
