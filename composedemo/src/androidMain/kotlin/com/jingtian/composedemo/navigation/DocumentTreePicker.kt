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


class DocumentTreePicker(val launcher: ManagedActivityResultLauncher<Uri?, Uri?>) :
    IDocumentTreePicker {
    override fun launch(mimes: MultiplatformFile?) {
        launcher.launch((mimes as? MultiplatformFileImpl)?.uri)
    }
}

@Composable
actual fun rememberDocumentTreePicker(onResult: (MultiplatformFile?) -> Unit): MutableState<IDocumentTreePicker> {
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            onResult(uri?.let { MultiplatformFileImpl(uri) })
        }
    )
    return remember {
        mutableStateOf(DocumentTreePicker(multipleImagePickerLauncher))
    }
}