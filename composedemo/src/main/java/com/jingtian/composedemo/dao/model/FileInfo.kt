package com.jingtian.composedemo.dao.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.FileInfoDao
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.extension

@Entity(
    tableName = FileInfoDao.TABLE_NAME,
)
class FileInfo(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    @Ignore
    @Transient
    var uri: Uri? = null,
    var storageId: Long = DataBase.INVALID_ID,
    var fileType: FileType = FileType.RegularFile,
) {
    var extension: String? = uri?.extension()
    fun getFileUri(): Uri? {
        val uri = this.uri
        return if (uri == null) {
            FileStorageUtils.getStorage(fileType)?.get(storageId)
        } else {
            return uri
        }
    }
}

enum class FileType(val value: Int, val mimeType: String) {
    RegularFile(0, "*/*"),
    IMAGE(1, "image/*"),
    VIDEO(2, "video/*"),
    ;

    companion object {
        private val valueMap = mutableMapOf(*entries.map { it.value to it }.toTypedArray())
        fun fromValue(value: Int): FileType? {
            return valueMap.get(value)
        }
    }
}