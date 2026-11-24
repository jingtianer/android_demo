package com.jingtian.composedemo.main

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.launch.LaunchTasks
import com.jingtian.composedemo.ui.theme.DemoAppTheme
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.viewmodels.albumViewModel
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGFloat


// px
class ScreenRoundedCornerInsets(val topStart: Float = 0f, val topEnd: Float = 0f, val bottomStart: Float = 0f, val bottomEnd: Float = 0f)

private val _currentScreenRoundedCornerInsets = mutableStateOf(ScreenRoundedCornerInsets())

val currentScreenRoundedCornerInsets by _currentScreenRoundedCornerInsets

fun screenRoundedCornerInsets(topStart: CGFloat, topEnd: CGFloat, bottomStart: CGFloat, bottomEnd: CGFloat) {
    _currentScreenRoundedCornerInsets.value = ScreenRoundedCornerInsets(topStart.toFloat(), topEnd.toFloat(), bottomStart.toFloat(), bottomEnd.toFloat())
}

fun MainViewController() = ComposeUIViewController {
    if (BuildKonfig.isDemo) {
        Main()
        return@ComposeUIViewController
    }
    DemoAppTheme {
        PasswordGuardedMain()
    }
    LaunchedEffect(Unit) {
        LaunchTasks.onLaunch()
        delay(2000)
        migrate()
    }
}

fun migrate() {
    CoroutineUtils.runIOTask({
        DataBase.dbImpl.getAlbumDao().getAllAlbum().forEach outerLoop@{ album->
            val albumId = album.albumId
            if (albumId == null || albumId.takeIf { it != -1L } == null) {
                return@outerLoop
            }
            albumViewModel().sendMessage("开始迁移")
            DataBase.dbImpl.getAlbumItemDao().getAllAlbumItemWithExtra(albumId).forEach innerLoop@{ item->
                FileStorageUtils.getStorage(item.fileInfo.fileType).migrate(item.fileInfo.getFileUri() ?: return@innerLoop)
            }
        }
    })
}