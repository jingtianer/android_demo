package com.jingtian.composedemo.dao.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.Date

@ProvidedTypeConverter
class DateTypeConverter : TypeAdapter<Date>() {
    @TypeConverter
    fun toDate(time: Long): Date {
        return Date(time)
    }

    @TypeConverter
    fun toString(date: Date): Long {
        return date.time
    }

    override fun write(out: JsonWriter, value: Date) {
        out.value(value.time)
    }

    override fun read(`in`: JsonReader): Date {
        return Date(`in`.nextLong())
    }
}