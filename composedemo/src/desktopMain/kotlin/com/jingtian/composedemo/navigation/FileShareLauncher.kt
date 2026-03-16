package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import java.awt.FileDialog
import java.io.File


class FileShareLauncher : IFileShareLauncher {
    override fun launch(file: MultiplatformFile) {
        val file = file.file ?: return
        val window = getComposeWindow() ?: return
        FileDialog(window, "保存文件", FileDialog.SAVE).apply {
            this.file = file.absolutePath
            isVisible = true
        }
    }
}
@Composable
actual fun rememberFileShare(onResult: (Long)->Unit): MutableState<IFileShareLauncher> {
    return mutableStateOf(FileShareLauncher())
}