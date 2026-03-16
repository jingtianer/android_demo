package com.jingtian.composedemo.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl

class FileShareLauncher(val context: Context) : IFileShareLauncher {
    override fun launch(file: MultiplatformFile) {
        val sharedFile = file.file
        val uri = if (sharedFile != null) {
            FileProvider.getUriForFile(
                app,
                app.packageName + ".fileprovider",
                sharedFile
            )
        } else {
            (file as MultiplatformFileImpl).uri
        }
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "*/*"
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享文件"))
    }
}
@Composable
actual fun rememberFileShare(onResult: (Long)->Unit): MutableState<IFileShareLauncher> {
    val context: Context = LocalContext.current
    return remember {
        mutableStateOf(FileShareLauncher(context))
    }
}