package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentInteractionController


class FileShareLauncher : IFileShareLauncher {
    override suspend fun launch(file: MultiplatformFile) {
        val filePath = withContext(Dispatchers.IO) {
            file.file ?: return@withContext null
        } ?: return
//        println("openFile: file: $filePath")

        // 1. 创建文档控制器
        val docController = UIDocumentInteractionController()

        docController.setURL(NSURL.fileURLWithPath(filePath.toString()))

        docController.delegate = OpenFileDelegate()
        docController.presentPreviewAnimated(true)
    }
}
@Composable
actual fun rememberFileShare(onResult: (Long)->Unit): MutableState<IFileShareLauncher> {
    return mutableStateOf(FileShareLauncher())
}