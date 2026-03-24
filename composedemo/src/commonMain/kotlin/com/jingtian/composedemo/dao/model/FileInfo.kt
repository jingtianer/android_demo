package com.jingtian.composedemo.dao.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.FileInfoDao
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.utils.FileStorageUtils

@Entity(
    tableName = FileInfoDao.TABLE_NAME,
)
class FileInfo(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    var storageId: Long = DataBase.INVALID_ID,
    var filePath: String = "/",
    var fileType: FileType = FileType.RegularFile,
    var intrinsicWidth: Int = -1,
    var intrinsicHeight: Int = -1,
    var extension: String? = null,
) {
    fun getFileUri(): MultiplatformFile? {
        return FileStorageUtils.getStorage(fileType)?.get(storageId, this)
    }
}

enum class FileType(val value: Int, val mimeType: String, val typeName: String) {
    IMAGE(1, "image/*", "图片"),
    VIDEO(2, "video/*", "视频"),
    AUDIO(3, "audio/*", "音频"),
    HTML(4, "text/html", "html"),
    RegularFile(0, "*/*", "其他"),
    ;

    companion object {
        private val valueMap = mutableMapOf(*entries.map { it.value to it }.toTypedArray())
        val mimes = entries.map { it.mimeType }.toTypedArray()
        fun fromValue(value: Int): FileType? {
            return valueMap.get(value)
        }
    }
}