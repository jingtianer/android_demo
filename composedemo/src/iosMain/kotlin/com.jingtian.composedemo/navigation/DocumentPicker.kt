package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl


class DocumentPicker(
    private val extensions: Array<String> = arrayOf(),
    private val onResult: (MultiplatformFile?)->Unit,
) : IDocumentPicker {
    override fun launch(mimes: Array<String>) {
    }
}

@Composable
actual fun rememberDocumentPicker(onResult: (MultiplatformFile?)->Unit): MutableState<IDocumentPicker> {
    return remember {
        mutableStateOf(DocumentPicker(onResult = onResult))
    }
}