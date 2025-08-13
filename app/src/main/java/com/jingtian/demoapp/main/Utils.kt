package com.jingtian.demoapp.main

import android.app.Application
import android.util.TypedValue
import java.lang.reflect.Field
import java.lang.reflect.Modifier

lateinit var app: Application

val Float.dp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, app.resources.displayMetrics)

fun <T> Field.getStaticValue(cl: Class<out Any>, clazz: Class<T>): T? {
    if (type == clazz && Modifier.isStatic(modifiers)) {
        return get(cl) as? T
    }
    return null
}

fun <T> Field.getStaticValueString(cl: Class<out Any>, clazz: Class<T>): String? {
    return getStaticValue(cl, clazz)?.let {
         when(it) {
            is Array<*> -> {
                it.reflectContentDeepToString()
            }
            is String -> {
                it.reflectToString()
            }
            else -> {
                it.toString()
            }
        }
    } ?: "null"
}

fun String.reflectToString(): String {
    return "\"$this\""
}

fun <T> Array<T>.reflectContentDeepToString(): String {
    val sb = StringBuilder("[")
    for (item in this) {
        when(item) {
            is Array<*> -> {
                sb.append(item.reflectContentDeepToString())
            }
            is String -> {
                sb.append(item.reflectToString())
            }
            else -> {
                sb.append(item)
            }
        }
        sb.append(", ")
    }
    sb[sb.length - 2] = ']'
    sb.deleteAt(sb.length - 1)
    return sb.toString()
}