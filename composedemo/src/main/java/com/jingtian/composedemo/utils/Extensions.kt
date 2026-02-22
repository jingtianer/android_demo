package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.function.Predicate

@Composable
inline fun <T> MutableLiveData<T>.composeObserve(crossinline onUpdate: (T?)->Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = Observer<T> {
            onUpdate(it)
        }
        this@composeObserve.observe(lifecycleOwner, observer)
        onDispose {
            this@composeObserve.removeObserver(observer)
        }
    }
}


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