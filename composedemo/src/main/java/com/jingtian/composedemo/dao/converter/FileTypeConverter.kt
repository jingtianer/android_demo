package com.jingtian.composedemo.dao.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.jingtian.composedemo.dao.model.FileType
import java.util.Date

@ProvidedTypeConverter
class FileTypeConverter :  TypeAdapter<FileType>() {
    override fun write(out: JsonWriter?, value: FileType?) {
        out?.value((value ?: FileType.RegularFile).value)
    }

    override fun read(`in`: JsonReader?): FileType {
        val value = `in`?.nextInt() ?: return FileType.RegularFile
        return FileType.fromValue(value) ?: FileType.RegularFile
    }

    @TypeConverter
    fun toFileType(value: Int): FileType {
        return FileType.fromValue(value) ?: FileType.RegularFile
    }

    @TypeConverter
    fun toString(fileType: FileType): Int {
        return fileType.value
    }
}