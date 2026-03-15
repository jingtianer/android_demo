package com.jingtian.composedemo.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DocumentTreePicker(val launcher: ManagedActivityResultLauncher<Uri?, Uri?>, val scope: CoroutineScope) :
    IDocumentTreePicker {
    override fun launch(mimes: MultiplatformFile?) {
        scope.launch(Dispatchers.Main) {
            launcher.launch(withContext(Dispatchers.IO) {
                (mimes as? MultiplatformFileImpl)?.uri
            })
        }
    }
}

@Composable
actual fun rememberDocumentTreePicker(onResult: (MultiplatformFile?) -> Unit): MutableState<IDocumentTreePicker> {
    val scope = rememberCoroutineScope()
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onResult(MultiplatformFileImpl(uri))
        }
    )
    return remember {
        mutableStateOf(DocumentTreePicker(multipleImagePickerLauncher, scope))
    }
}