package com.jingtian.composedemo.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

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