package com.jingtian.composedemo.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.playIntent

class AlbumItemLauncher(
    val context: Context,
    val launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) : IAlbumItemLauncher{
    override fun launch(fileName: String, fileInfo: FileInfo) {
        val playIntent = playIntent(context, fileName, fileInfo)
        if (playIntent != null) {
            if (fileInfo.fileType == FileType.HTML) {
                launcher.launch(playIntent)
            } else {
                context.startActivity(playIntent)
            }
        }
    }

    override fun launch(albumItemRelation: AlbumItemRelation) {
        val fileName = albumItemRelation.albumItem.itemName
        val fileInfo = albumItemRelation.fileInfo
        launch(fileName, fileInfo)
    }
}

@Composable
actual fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher> {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        onResult.invoke(result.resultCode.toLong())
    }
    return remember {
        mutableStateOf(AlbumItemLauncher(context, launcher))
    }
}