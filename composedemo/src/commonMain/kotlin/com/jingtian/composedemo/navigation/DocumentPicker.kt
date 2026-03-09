package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.jingtian.composedemo.multiplatform.MultiplatformFile


interface IDocumentPicker {
    fun launch(mimes : Array<String>)
}

@Composable
expect fun rememberDocumentPicker(onResult: (MultiplatformFile?)->Unit): MutableState<IDocumentPicker>