package com.jingtian.composedemo.dao.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.FileInfoDao
import com.jingtian.composedemo.utils.FileStorageUtils

@Entity(
    tableName = FileInfoDao.TABLE_NAME,
    primaryKeys = [
        "id", "fileType"
    ]
)
class FileInfo(
    var id: Long = DataBase.INVALID_ID,
    @Ignore
    var uri: Uri? = null,
    var fileType: FileType = FileType.RegularFile
) {
    fun getUri(): Uri? {
        val uri = this.uri
        return if (uri == null) {
            FileStorageUtils.getStorage(fileType)?.get(id)
        } else {
            return uri
        }
    }
}

enum class FileType(val value: Int) {
    RegularFile(0),
    IMAGE(1)
    ;

    companion object {
        fun fromValue(value: Int): FileType? {
            return when(value) {
                0 -> RegularFile
                1 -> IMAGE
                else -> null
            }
        }
    }
}