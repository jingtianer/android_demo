package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile

class DocumentPicker : IDocumentPicker {
    override fun launch(mimes: Array<String>) {

    }

}

@Composable
actual fun rememberDocumentPicker(onResult: (MultiplatformFile?)->Unit): MutableState<IDocumentPicker> {
    return remember {
        mutableStateOf(DocumentPicker())
    }
}