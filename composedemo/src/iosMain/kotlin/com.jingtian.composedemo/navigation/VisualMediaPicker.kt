package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl

class ImagePicker(
    private val extensions: Array<String> = MultiplatformFileImpl.imageExtensions,
    private val onResult: (MultiplatformFile?)->Unit,
): IImagePicker {
    override fun launch() {
    }

}
@Composable
actual fun rememberImagePicker(onResult: (MultiplatformFile?)->Unit): MutableState<IImagePicker> {
    return remember { mutableStateOf(ImagePicker(onResult = onResult)) }
}