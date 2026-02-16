package com.jingtian.composedemo.model

import java.util.*

class AlbumItem(
    var itemId: Long = INVALID_ID,
    var createTime: Date = Date(),
    var itemName: String = "",

    var albumId: Long = INVALID_ID,

    var fileId: Long = INVALID_ID,
    var userId: Long = INVALID_ID,
)