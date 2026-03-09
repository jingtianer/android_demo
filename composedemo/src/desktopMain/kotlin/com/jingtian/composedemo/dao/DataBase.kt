package com.jingtian.composedemo.dao

actual fun dbImplCreator() : DataBase {
    throw NotImplementedError()
}