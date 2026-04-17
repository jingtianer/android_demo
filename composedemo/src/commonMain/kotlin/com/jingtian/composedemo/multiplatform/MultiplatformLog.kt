package com.jingtian.composedemo.multiplatform

import com.jingtian.composedemo.BuildKonfig


typealias LogLevel = Int

const val LOG_LEVEL_ERROR = 0
const val LOG_LEVEL_WARN = 1
const val LOG_LEVEL_DEBUG = 2
const val LOG_LEVEL_WTF = 3
const val LOG_LEVEL_INFO = 4
const val LOG_LEVEL_VERBOSE = 5

const val PREFIX_ERROR = "\uD83D\uDD34 E"
const val PREFIX_WARN = "\uD83D\uDFE0 W"
const val PREFIX_DEBUG = "\uD83D\uDD35 D"
const val PREFIX_WTF = "\uD83D\uDEBE WFT"
const val PREFIX_INFO = "\uD83D\uDFE2 I"
const val PREFIX_VERBOSE = "\uD83D\uDFE1 V"

val LOG_LEVEL: LogLevel = if (BuildKonfig.isDebug) LOG_LEVEL_WARN else LOG_LEVEL_WARN

expect fun printLog(level: LogLevel, tag: String, msg: String)
expect fun printLog(level: LogLevel, tag: String, msg: String, t: Throwable?)

fun logE(level: LogLevel, tag: String, msg: String, t: Throwable) {
    if (level <= LOG_LEVEL) {
        printLog(level, tag, msg, t)
    }
}

inline fun logD(level: LogLevel, tag: String, crossinline msg: () -> String) {
    if (level <= LOG_LEVEL) {
        printLog(level, tag, msg())
    }
}

inline fun logI(level: LogLevel, tag: String, crossinline msg: () -> String) {
    if (level <= LOG_LEVEL) {
        printLog(level, tag, msg())
    }
}