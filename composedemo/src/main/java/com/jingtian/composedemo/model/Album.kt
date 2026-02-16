package com.jingtian.composedemo.model

import java.util.Date

const val INVALID_ID = 0L

class Album(
    var albumId: Long = INVALID_ID,
    var createTime: Date = Date(),
    var albumName: String = "",
    var userId: Long = INVALID_ID,
)