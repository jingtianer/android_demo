package com.jingtian.composedemo.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.R
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType.*
import com.jingtian.composedemo.main.drawer.MainDrawer
import com.jingtian.composedemo.main.gallery.Gallery
import com.jingtian.composedemo.main.gallery.GalleryStateHolder
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.observeAsState
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AlbumViewModel.Companion.observeAsState
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import com.jingtian.composedemo.web.WebViewActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.SoftReference

class MainActivity : BaseActivity() {
    @Composable
    override fun Content() = Main()

    override fun shouldFitSystemBars(): Boolean = false
}

fun systemFallbackIntent(context: Context, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType.mimeType
    val originFileUri = fileInfo.getFileUri()
    val originFile = originFileUri?.file
    val mediaUri: Uri = if (originFile != null) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            originFile
        )
    } else {
        (originFileUri as MultiplatformFileImpl)?.uri
    } ?: return null
    return Intent(Intent.ACTION_VIEW).apply {
        // 设置Uri和媒体类型
        setDataAndType(mediaUri, mediaType)
        // 关键：授予系统应用访问该Uri的权限
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // 适配Android 12+的前台服务权限（可选，避免部分播放器启动失败）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

fun webIntent(context: Context, fileInfo: FileInfo) : Intent {
    return Intent(context, WebViewActivity::class.java).apply {
        putExtra(WebViewActivity.KEY_WEB_URI, (fileInfo.getFileUri() as? MultiplatformFileImpl)?.uri)
        putExtra(WebViewActivity.KEY_STORAGE_ID, fileInfo.storageId)
    }
}

fun playIntent(context: Context, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType
    return when (mediaType) {
        HTML -> {
            webIntent(context, fileInfo)
        }

        IMAGE, VIDEO, AUDIO, RegularFile -> systemFallbackIntent(context, fileInfo)
    }
}
