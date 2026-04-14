package com.horse.jk_bms.data.local

import androidx.room.TypeConverter
import org.json.JSONArray
import java.util.Base64

object Converters {

    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        val arr = JSONArray()
        for (v in value) arr.put(v.toDouble())
        return arr.toString()
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val arr = JSONArray(value)
        return FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
    }

    @TypeConverter
    fun fromIntArray(value: IntArray): String {
        val arr = JSONArray()
        for (v in value) arr.put(v)
        return arr.toString()
    }

    @TypeConverter
    fun toIntArray(value: String): IntArray {
        val arr = JSONArray(value)
        return IntArray(arr.length()) { arr.getInt(it) }
    }

    @TypeConverter
    fun fromBooleanArray(value: BooleanArray): String {
        val arr = JSONArray()
        for (v in value) arr.put(v)
        return arr.toString()
    }

    @TypeConverter
    fun toBooleanArray(value: String): BooleanArray {
        val arr = JSONArray(value)
        return BooleanArray(arr.length()) { arr.getBoolean(it) }
    }

    @TypeConverter
    fun fromByteArray(value: ByteArray): String {
        return Base64.getEncoder().encodeToString(value)
    }

    @TypeConverter
    fun toByteArray(value: String): ByteArray {
        return Base64.getDecoder().decode(value)
    }
}
