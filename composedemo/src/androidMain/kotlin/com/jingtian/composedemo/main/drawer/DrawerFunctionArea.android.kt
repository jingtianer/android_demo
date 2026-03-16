package com.jingtian.composedemo.main.drawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.R
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.navigation.rememberDocumentPicker
import com.jingtian.composedemo.utils.share.ShareUtils
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import kotlinx.coroutines.launch

@Composable
actual fun PlatformDrawerFunctions() {
    val viewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.viewModelFactory)
    val scope = rememberCoroutineScope()
    val documentPicker by rememberDocumentPicker { uri->
        uri ?: return@rememberDocumentPicker
        scope.launch {
            ShareUtils.importSharedDb(viewModel, (uri as MultiplatformFileImpl).uri)
        }
    }
    val context = LocalContext.current
    DrawerFunctionView({
        scope.launch {
            ShareUtils.shareDataBase(context)
        }
    }, painterResource(R.drawable.upload_to_cloud), "导出数据")
    DrawerFunctionView({
        scope.launch {
            documentPicker.launch(arrayOf("application/json"))
        }
    }, painterResource(R.drawable.import_icon), "导入数据")
}