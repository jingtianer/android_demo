package com.jingtian.composedemo.dao.model

private val DEFAULT_USER_NAME = "默认用户"
private val DEFAULT_DESC = "这个人很懒，什么都没有留下"
class User(
    private var userName: String? = null,
    private var userDesc: String? = null,
    private var userAvatar: FileInfo? = null,
) {

    fun getUserName(): String {
        return userName ?: DEFAULT_USER_NAME
    }

    fun getUserDesc(): String {
        return userDesc ?: DEFAULT_DESC
    }

    fun getUserImage() {

    }
}