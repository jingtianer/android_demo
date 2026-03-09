package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.jingtian.composedemo.multiplatform.MultiplatformFile


interface IDocumentTreePicker {
    fun launch(mimes: MultiplatformFile?)
}

@Composable
expect fun rememberDocumentTreePicker(onResult: (MultiplatformFile?) -> Unit): MutableState<IDocumentTreePicker>