package com.jingtian.composedemo.dao.model

val DEFAULT_USER_NAME = "默认用户"
val DEFAULT_DESC = "这个人很懒，什么都没有留下"
class User(
    var userName: String = DEFAULT_USER_NAME,
    var userDesc: String = DEFAULT_DESC,
    var userAvatar: FileInfo = FileInfo(),
)