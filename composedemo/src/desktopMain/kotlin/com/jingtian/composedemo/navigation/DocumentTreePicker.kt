package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import java.io.File
import javax.swing.JFileChooser

class DocumentTreePicker(val title: String, private val onResult: (MultiplatformFile?) -> Unit) : IDocumentTreePicker {
    override fun launch(mimes: MultiplatformFile?) {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(MultiplatformFileImpl(chooser.selectedFile, chooser.selectedFile.extension))
        }
    }
}

@Composable
actual fun rememberDocumentTreePicker(onResult: (MultiplatformFile?) -> Unit): MutableState<IDocumentTreePicker>  {
    return remember { mutableStateOf(DocumentTreePicker("导入目录", onResult)) }
}