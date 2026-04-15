package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController

class DocumentTreePicker(val title: String, private val onResult: (MultiplatformFile?) -> Unit) : IDocumentTreePicker {
    override fun launch(mimes: MultiplatformFile?) {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return

        val delegate = DocumentPickerDelegate(onResult)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = utTypeDir, asCopy = false).apply {
            this.delegate = delegate
            allowsMultipleSelection = false
        }
        rootVC.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberDocumentTreePicker(onResult: (MultiplatformFile?) -> Unit): MutableState<IDocumentTreePicker>  {
    return remember { mutableStateOf(DocumentTreePicker("导入目录", onResult)) }
}