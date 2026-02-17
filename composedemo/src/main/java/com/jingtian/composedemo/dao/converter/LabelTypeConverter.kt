package com.jingtian.composedemo.dao.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.Label

@ProvidedTypeConverter
class LabelTypeConverter :  TypeAdapter<Label>() {
    override fun write(out: JsonWriter?, value: Label?) {
        out?.value((value ?: Label.DEFAULT).value)
    }

    override fun read(`in`: JsonReader?): Label {
        val value = `in`?.nextInt() ?: return Label.DEFAULT
        return Label.fromValue(value) ?: Label.DEFAULT
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