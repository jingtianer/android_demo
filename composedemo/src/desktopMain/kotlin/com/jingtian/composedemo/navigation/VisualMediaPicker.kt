package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import java.awt.FileDialog
import java.io.File

class ImagePicker(
    private val extensions: Array<String> = MultiplatformFileImpl.imageExtensions,
    private val onResult: (MultiplatformFile?)->Unit,
): IImagePicker {
    override fun launch() {
        val window = getComposeWindow() ?: return
        val dialog = FileDialog(window, "导入图片", FileDialog.LOAD).apply {
            if (extensions.isNotEmpty()) {
                file = extensions.joinToString(",") { "*.$it" }
            }
            isVisible = true
        }
        val file = File(dialog.directory, dialog.file ?: return).takeIf {
//            print("${dialog.directory}, ${dialog.file}")
            it.extension in extensions
        } ?: return
        onResult.invoke(MultiplatformFileImpl(file))
    }

}
@Composable
actual fun rememberImagePicker(onResult: (MultiplatformFile?)->Unit): MutableState<IImagePicker> {
    return remember { mutableStateOf(ImagePicker(onResult = onResult)) }
}