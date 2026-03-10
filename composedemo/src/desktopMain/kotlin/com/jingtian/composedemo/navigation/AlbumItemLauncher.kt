package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import java.awt.Desktop
import java.io.File
import java.net.URI

class AlbumItemLauncher : IAlbumItemLauncher {
    private fun openUrl(file: File) {
        runCatching {
            Desktop.getDesktop().browse(file.toURI())
        }
    }

    private fun openFile(file: File) {
        runCatching {
            Desktop.getDesktop().open(file)
        }
    }

    override fun launch(fileInfo: FileInfo) {
        val file = fileInfo.getFileUri()?.file ?: return
        when(fileInfo.fileType) {
            FileType.HTML -> openUrl(file)
            else -> openFile(file)
        }
    }
}

@Composable
actual fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher> {
    return remember {
        mutableStateOf(AlbumItemLauncher())
    }
}