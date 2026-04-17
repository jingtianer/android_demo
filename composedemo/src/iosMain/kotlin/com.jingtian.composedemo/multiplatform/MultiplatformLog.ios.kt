package com.jingtian.composedemo.multiplatform

import platform.Foundation.NSLog


actual fun printLog(level: LogLevel, tag: String, msg: String) {
    printLog(level, tag, msg, null)
}

private fun logToString(level: String, tag: String, msg: String, t: Throwable?): String {
    return "[$level] $tag: $msg" + (if (t != null) "error: ${t.message} stacktrace:\n${t.stackTraceToString()}" else "")
}

actual fun printLog(level: LogLevel, tag: String, msg: String, t: Throwable?) {
    when(level) {
        LOG_LEVEL_ERROR -> NSLog(logToString(PREFIX_ERROR, tag, msg, t))
        LOG_LEVEL_WARN -> NSLog(logToString(PREFIX_WARN, tag, msg, t))
        LOG_LEVEL_DEBUG -> NSLog(logToString(PREFIX_DEBUG, tag, msg, t))
        LOG_LEVEL_VERBOSE -> NSLog(logToString(PREFIX_VERBOSE, tag, msg, t))
        LOG_LEVEL_WTF -> NSLog(logToString(PREFIX_WTF, tag, msg, t))
        LOG_LEVEL_INFO -> NSLog(logToString(PREFIX_INFO, tag, msg, t))
    }
}