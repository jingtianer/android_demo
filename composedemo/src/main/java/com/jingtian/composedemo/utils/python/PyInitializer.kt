package com.jingtian.composedemo.utils.python

import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.jingtian.composedemo.base.app

object PyInitializer {
    fun init() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(app))
        }
        if (Python.isStarted()) {
            val module = Python.getInstance().getModule("hello_world.hello_world")
            module.callAttr("hello_world")
        }
    }
}