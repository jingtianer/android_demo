package com.jingtian.composedemo.model

import android.net.Uri

class FileInfo(
    var id: Long = INVALID_ID,
    var uri: Uri = Uri.EMPTY,
    var fileType: FileType = FileType.RegularFile
)

enum class FileType {
    RegularFile,
    IMAGE
}