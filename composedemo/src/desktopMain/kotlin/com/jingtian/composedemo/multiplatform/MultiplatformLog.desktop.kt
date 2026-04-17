package com.jingtian.composedemo.multiplatform

actual fun printLog(level: LogLevel, tag: String, msg: String) {
    printLog(level, tag, msg, null)
}

private fun logToString(level: String, tag: String, msg: String, t: Throwable?): String {
    return "[$level] $tag: $msg" + if (t != null) "error: ${t.message} stacktrace:\n${t.stackTraceToString()}" else ""
}

actual fun printLog(level: LogLevel, tag: String, msg: String, t: Throwable?) {
    when(level) {
        LOG_LEVEL_ERROR -> System.err.println(logToString(PREFIX_ERROR, tag, msg, t))
        LOG_LEVEL_WARN -> System.err.println(logToString(PREFIX_WARN, tag, msg, t))
        LOG_LEVEL_DEBUG -> System.out.println(logToString(PREFIX_DEBUG, tag, msg, t))
        LOG_LEVEL_VERBOSE -> System.out.println(logToString(PREFIX_VERBOSE, tag, msg, t))
        LOG_LEVEL_WTF -> System.err.println(logToString(PREFIX_WTF, tag, msg, t))
        LOG_LEVEL_INFO -> System.out.println(logToString(PREFIX_INFO, tag, msg, t))
    }
}