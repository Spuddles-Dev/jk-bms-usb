package com.horse.jk_bms.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val FROM_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf("runtime_data", "config", "device_info", "fault_record", "system_log").forEach { table ->
                db.execSQL("ALTER TABLE $table ADD COLUMN sessionId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE $table ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX index_${table}_timestamp ON $table(timestamp)")
                db.execSQL("CREATE INDEX index_${table}_sessionId_timestamp ON $table(sessionId, timestamp)")
            }
            db.execSQL("ALTER TABLE runtime_data ADD COLUMN cellWireResStat TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("ALTER TABLE runtime_data ADD COLUMN enableFlags TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE fault_record ADD COLUMN eventKey TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE TABLE sessions (id TEXT NOT NULL PRIMARY KEY, deviceId TEXT NOT NULL, name TEXT NOT NULL, startedAt INTEGER NOT NULL, endedAt INTEGER, firmware TEXT NOT NULL)")
            db.execSQL("CREATE INDEX index_sessions_startedAt ON sessions(startedAt)")
            db.execSQL("CREATE TABLE write_audit (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, timestamp INTEGER NOT NULL, sessionId TEXT NOT NULL, deviceId TEXT NOT NULL, beforeJson TEXT NOT NULL, afterJson TEXT NOT NULL, outcome TEXT NOT NULL)")
            db.execSQL("CREATE INDEX index_write_audit_timestamp ON write_audit(timestamp)")
            db.execSQL("INSERT INTO sessions SELECT '', '', 'Imported history', startedAt, endedAt, '' FROM (SELECT MIN(timestamp) AS startedAt, MAX(timestamp) AS endedAt, COUNT(*) AS count FROM runtime_data) WHERE count > 0")
            db.execSQL("UPDATE device_info SET bluetoothPwd = '', settingPassword = ''")
        }
    }
}
