package com.jingtian.composedemo.multiplatform

import android.util.Log


actual fun printLog(level: LogLevel, tag: String, msg: String) {
    when(level) {
        LOG_LEVEL_ERROR -> Log.e(tag, msg)
        LOG_LEVEL_WARN -> Log.w(tag, msg)
        LOG_LEVEL_DEBUG -> Log.d(tag, msg)
        LOG_LEVEL_VERBOSE -> Log.v(tag, msg)
        LOG_LEVEL_WTF -> Log.wtf(tag, msg)
        LOG_LEVEL_INFO -> Log.i(tag, msg)
    }
}

actual fun printLog(level: LogLevel, tag: String, msg: String, t: Throwable?) {
    when(level) {
        LOG_LEVEL_ERROR -> Log.e(tag, msg, t)
        LOG_LEVEL_WARN -> Log.w(tag, msg, t)
        LOG_LEVEL_DEBUG -> Log.d(tag, msg, t)
        LOG_LEVEL_VERBOSE -> Log.v(tag, msg, t)
        LOG_LEVEL_WTF -> Log.wtf(tag, msg, t)
        LOG_LEVEL_INFO -> Log.i(tag, msg, t)
    }
}