package com.jingtian.composedemo.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl

class ImagePicker(val launcher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>): IImagePicker {
    override fun launch() {
        launcher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }
}

@Composable
actual fun rememberImagePicker(onResult: (MultiplatformFile?)->Unit): MutableState<IImagePicker> {
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri->
            uri ?: return@rememberLauncherForActivityResult
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            app.contentResolver.takePersistableUriPermission(uri, takeFlags)
            onResult(MultiplatformFileImpl(uri))
        }
    )
    return remember {
        mutableStateOf(ImagePicker(multipleImagePickerLauncher))
    }
}