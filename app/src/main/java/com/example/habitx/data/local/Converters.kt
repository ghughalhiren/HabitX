package com.example.habitx.data.local

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.fromEpochMilliseconds(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilliseconds()
    }

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        return value?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }
    }

    @TypeConverter
    fun intListToString(list: List<Int>?): String? {
        return list?.joinToString(",")
    }
}
