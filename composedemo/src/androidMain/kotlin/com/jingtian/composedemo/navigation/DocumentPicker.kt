package com.jingtian.composedemo.navigation

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl


interface IDocumentPicker {
    fun launch(mimes : Array<String>)
}

class DocumentPicker(val launcher: ManagedActivityResultLauncher<Array<String>, Uri?>): IDocumentPicker {
    override fun launch(mimes : Array<String>) {
        launcher.launch(mimes)
    }
}

@Composable
fun rememberDocumentPicker(onResult: (MultiplatformFile?)->Unit): MutableState<IDocumentPicker> {
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri->
            onResult(uri?.let { MultiplatformFileImpl(uri) })
        }
    )
    return remember {
        mutableStateOf(DocumentPicker(multipleImagePickerLauncher))
    }
}