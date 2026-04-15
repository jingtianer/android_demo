package com.jingtian.composedemo.dao.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.jingtian.composedemo.dao.model.FileType

@ProvidedTypeConverter
class FileTypeConverter {
    @TypeConverter
    fun toFileType(value: Int): FileType {
        return FileType.fromValue(value) ?: FileType.RegularFile
    }

    @TypeConverter
    fun fromFileType(fileType: FileType): Int {
        return fileType.value
    }
}
