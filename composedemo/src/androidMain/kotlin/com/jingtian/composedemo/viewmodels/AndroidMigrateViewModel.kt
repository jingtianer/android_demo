package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel

class AndroidMigrateViewModel : ViewModel() {
    val isMigrationFinished = mutableIntStateOf(0)
}