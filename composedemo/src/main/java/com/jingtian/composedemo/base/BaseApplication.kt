package com.jingtian.composedemo.base

import android.app.Application

lateinit var app: BaseApplication

class BaseApplication : Application() {
    init {
        app = this
    }
}