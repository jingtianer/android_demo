package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.jingtian.composedemo.multiplatform.MultiplatformFile


interface IImagePicker {
    fun launch()
}

@Composable
expect fun rememberImagePicker(onResult: (MultiplatformFile?)->Unit): MutableState<IImagePicker>