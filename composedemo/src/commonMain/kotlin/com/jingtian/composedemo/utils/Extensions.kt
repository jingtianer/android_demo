package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.lang.ref.SoftReference
import java.util.function.Predicate


fun CharSequence.splitBy(predicate: Predicate<Char>): List<String> {
    val sb = StringBuilder()
    val resultList = mutableListOf<String>()
    for (char in this) {
        if (predicate.test(char)) {
            if (sb.isNotEmpty()) {
                resultList.add(sb.toString())
                sb.clear()
            }
            continue
        } else {
            sb.append(char)
        }
    }
    if (sb.isNotEmpty()) {
        resultList.add(sb.toString())
    }
    return resultList
}

fun CharSequence.splitByWhiteSpace() = splitBy(Char::isWhitespace)

fun <K, V> SnapshotStateMap<K, SoftReference<V>>.getOrPutRef(key: K, default: ()->V): V {
    return get(key)?.get() ?: default().also {
        put(key, SoftReference(it))
    }
}

@Composable
fun <T> MutableState<T>.observeAsState() = remember { this }