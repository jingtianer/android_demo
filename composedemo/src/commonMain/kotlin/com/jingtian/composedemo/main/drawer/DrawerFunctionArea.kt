package com.jingtian.composedemo.main.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.main.dialog.AddOrEditAlbumDialog
import com.jingtian.composedemo.base.resources.getPainter
import demoapp.composedemo.generated.resources.Res

import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.main.gallery.GalleryFunctionView
import com.jingtian.composedemo.main.gallery.GalleryFunctions
import com.jingtian.composedemo.navigation.rememberDocumentPicker
import com.jingtian.composedemo.navigation.rememberFileShare
import com.jingtian.composedemo.utils.share.ShareUtils
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthSuccess
import com.jingtian.composedemo.viewmodels.albumViewModel
import com.jingtian.composedemo.viewmodels.authViewModel
import kotlinx.coroutines.launch

@Composable
fun DrawerFunctionArea() {
    var dialogState by remember { mutableStateOf(false) }
    DrawerFunctionView(
        onClick = {
            dialogState = true
        },
        painter = getPainter(DrawableIcon.DrawableAdd),
        text = "添加合集",
    )

    ShareDrawerFunctions()

    if (BuildKonfig.isRemote) {
        val authViewModel = authViewModel()
        val viewModel: AlbumViewModel = albumViewModel()
        DrawerFunctionView(GalleryFunctions.REFRESH_CIFS_FILE) {
            authViewModel.doAuth { authResult->
                when (authResult) {
                    is AuthSuccess -> {
                        viewModel.refreshRemote()
                    }
                }
            }
        }
    }

    if (dialogState) {
        AddOrEditAlbumDialog {
            dialogState = false
        }
    }
}



@Composable
fun ShareDrawerFunctions() {
    val viewModel: AlbumViewModel = albumViewModel()
    val scope = rememberCoroutineScope()
    val documentPicker by rememberDocumentPicker { uri->
        uri ?: return@rememberDocumentPicker
        scope.launch {
            ShareUtils.importSharedDb(viewModel, uri)
        }
    }
    val rememberShareLauncher by rememberFileShare({})
    DrawerFunctionView({
        scope.launch {
            rememberShareLauncher.launch(ShareUtils.shareDataBase())
        }
    }, getPainter(DrawableIcon.DrawableUploadToCloud), "导出数据")
    DrawerFunctionView({
        scope.launch {
            documentPicker.launch(arrayOf("application/json"))
        }
    }, getPainter(DrawableIcon.DrawableImportIcon), "导入数据")
}