package com.jingtian.composedemo.launch

import com.jingtian.composedemo.utils.FileStorageUtils

object LaunchTasks {
    fun onLaunch() {
        FileStorageUtils.checkRootDir()
    }
}