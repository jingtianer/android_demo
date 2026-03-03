package com.jingtian.composedemo.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.lang.ref.SoftReference
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

fun Context.getScreenWidth(): Int {
    val displayMetrics = DisplayMetrics()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds.width()
    } else {
        this.getSystemService(WindowManager::class.java).defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}

fun Context.getScreenHeight(): Int {
    val displayMetrics = DisplayMetrics()
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds.height()
    } else {
        this.getSystemService(WindowManager::class.java).defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.heightPixels
    }
}

fun <K, V> SnapshotStateMap<K, SoftReference<V>>.getOrPutRef(key: K, default: ()->V): V {
    return get(key)?.get() ?: default().also {
        put(key, SoftReference(it))
    }
}