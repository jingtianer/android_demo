package com.jingtian.demoapp.main

import android.app.Application
import android.util.TypedValue

lateinit var app: Application

val Float.dp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, app.resources.displayMetrics)