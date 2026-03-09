package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile

class DocumentTreePicker : IDocumentTreePicker {
    override fun launch(mimes: MultiplatformFile?) {

    }

}

@Composable
actual fun rememberDocumentTreePicker(onResult: (MultiplatformFile?) -> Unit): MutableState<IDocumentTreePicker>  {
    return remember { mutableStateOf(DocumentTreePicker()) }
}