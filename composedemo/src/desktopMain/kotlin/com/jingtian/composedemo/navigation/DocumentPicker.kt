package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import java.awt.FileDialog
import java.io.File
import java.io.FilenameFilter

fun getComposeWindow(): ComposeWindow? =
    java.awt.Window.getWindows().firstOrNull { it is ComposeWindow } as? ComposeWindow


class DocumentPicker(
    private val extensions: Array<String> = arrayOf(),
    private val onResult: (MultiplatformFile?)->Unit,
) : IDocumentPicker {
    override fun launch(mimes: Array<String>) {
        val window = getComposeWindow() ?: return
        val dialog = FileDialog(window, "导入文件", FileDialog.LOAD).apply {
            if (extensions.isNotEmpty()) {
                filenameFilter = FilenameFilter { dir, name ->
                    val file = File(dir, name)
                    file.isDirectory || file.extension in extensions
                }
            }
            isVisible = true
        }
        val file = File(dialog.directory, dialog.file ?: return)
        onResult.invoke(MultiplatformFileImpl(file, file.extension))
    }
}

@Composable
actual fun rememberDocumentPicker(onResult: (MultiplatformFile?)->Unit): MutableState<IDocumentPicker> {
    return remember {
        mutableStateOf(DocumentPicker(onResult = onResult))
    }
}