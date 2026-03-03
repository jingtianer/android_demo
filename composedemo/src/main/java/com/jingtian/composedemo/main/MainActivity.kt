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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
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
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.FileStorageUtils.safeToFile
import com.jingtian.composedemo.viewmodels.AlbumViewModel
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

@Preview
@Composable
fun Main() {
    val viewModel: AlbumViewModel = viewModel()
    var menuItemsEntity by remember { mutableStateOf(emptyList<Album>()) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed, confirmStateChange = { drawerValue: DrawerValue ->
        when(drawerValue) {
            DrawerValue.Closed -> {
                val canClose = menuItemsEntity.isNotEmpty()
                if (!canClose) {
                    val noAlbumToast = "没有任何合集，先创建合集吧"
                    if (viewModel.currentBackgroundTask.value?.equals(noAlbumToast) != true) {
                        viewModel.sendMessage(noAlbumToast)
                    }
                }
                canClose
            }
            DrawerValue.Open -> true
        }
    })
    var currentSelectedAlbum by remember { mutableStateOf<IndexedValue<Album>?>(null) }
    val albumListChange by viewModel.albumListChange.observeAsState()

    fun updateSelectedValue(list: List<Album>): IndexedValue<Album>? {
        val currentSelectedAlbum = currentSelectedAlbum
        if (currentSelectedAlbum != null) {
            val lastSelectedIndex = currentSelectedAlbum.index
            val lastSelectedAlbumId = currentSelectedAlbum.value.albumId
            val findId = list.withIndex().find { it.value.albumId == lastSelectedAlbumId }
            if (findId != null) {
                return findId
            }
            if (lastSelectedIndex < list.size) {
                return IndexedValue(lastSelectedIndex, list.get(lastSelectedIndex))
            }
            return list.asReversed().withIndex().firstOrNull()
        } else {
            return list.withIndex().firstOrNull()
        }
    }

    LaunchedEffect(albumListChange) {
        withContext(Dispatchers.IO) {
            viewModel.menuItemsFlow.collect { value ->
                val nextSelectedAlbum = updateSelectedValue(value)
                withContext(Dispatchers.Main) {
                    menuItemsEntity = value
                    currentSelectedAlbum = nextSelectedAlbum
                }
                if (value.isEmpty()) {
                    drawerState.snapTo(DrawerValue.Open)
                }
            }
        }
    }
    val snackBarMessage by viewModel.currentBackgroundTask.observeAsState()

    val galleryStateHolderMap = remember(menuItemsEntity) {
        mutableStateMapOf<Long, SoftReference<GalleryStateHolder>>()
    }
    var showSnackBar by remember { mutableStateOf(snackBarMessage != null) }
    var showSnackBarAnim by remember { mutableStateOf(true) }

    LaunchedEffect(snackBarMessage) {
        if (showSnackBar && snackBarMessage.isNullOrBlank()) {
            showSnackBarAnim = true
        } else if (!showSnackBar && !snackBarMessage.isNullOrBlank()) {
            showSnackBarAnim = true
        } else {
            showSnackBarAnim = false
        }
        showSnackBar = !snackBarMessage.isNullOrBlank()
    }
    val scope = rememberCoroutineScope()
    Scaffold(
        snackbarHost = {
            AnimatedVisibility(
                visible = showSnackBar,
                enter = if(showSnackBarAnim) fadeIn() + expandIn() else EnterTransition.None,
                exit = if(showSnackBarAnim) shrinkOut() + fadeOut() else ExitTransition.None,
            ) {
                Snackbar(
                    modifier = Modifier.padding(8.dp)
                ) {
                    AppThemeText(snackBarMessage ?: "", style = LocalTextStyle.current.copy(color = LocalAppPalette.current.dialogBg), maxLines = 1)
                }
            }
        }
    ) { _->
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = LocalAppPalette.current.drawerBg, windowInsets = WindowInsets.navigationBars) {
                    MainDrawer(menuItemsEntity) { index, album ->
                        currentSelectedAlbum = IndexedValue(index, album)
                        scope.launch {
                            drawerState.close()
                        }
                    }
                }
            },
            Modifier.fillMaxSize(),
            drawerState = drawerState,
            gesturesEnabled = true
        ) {
            Gallery(galleryStateHolderMap, currentSelectedAlbum, menuItemsEntity, drawerState)
        }
    }
}

class LabelCheckInfo<T>(val label: T, val name: String, var isChecked: MutableLiveData<Boolean> = MutableLiveData(false))

fun systemFallbackIntent(context: Context, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType.mimeType
    val originFileUri = fileInfo.getFileUri()
    val originFile = originFileUri?.safeToFile()
    val mediaUri: Uri = if (originFile != null) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            originFile
        )
    } else {
        originFileUri
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
        putExtra(WebViewActivity.KEY_WEB_URI, fileInfo.getFileUri())
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

@Composable
fun AppThemeSwitcher() {
    val viewModel: AppThemeViewModel = viewModel()
    val currentTheme by viewModel.currentAppTheme.observeAsState()
    @Composable
    fun Modifier.modifier(appTheme: AppTheme):Modifier {
        val modifier = this
        return if (appTheme == (currentTheme ?: AppTheme.AUTO)) {
            modifier.background(
                color = LocalAppPalette.current.labelChecked,
                shape = RoundedCornerShape(100)
            )
        } else {
            modifier.background(
                color = LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(100)
            )
        }
            .clip(RoundedCornerShape(100))
            .clickable {
                viewModel.currentAppTheme.value = appTheme
            }
            .padding(8.dp)
    }

    val size = 42.dp
    val autoWidth = size * 1.6464f

    Box(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .wrapContentHeight()) {
//        AppThemeText(
//            "主题颜色",
//            Modifier
//                .wrapContentSize()
//                .align(Alignment.CenterStart),
//            style = LocalTextStyle.current.copy(fontSize = 16.sp)
//        )
        Row(
            Modifier
                .wrapContentSize()
                .align(Alignment.CenterEnd)
        ) {
            Image(
                painter = painterResource(R.drawable.app_theme_auto),
                contentDescription = "自动",
                Modifier
                    .size(width = autoWidth, size)
                    .modifier(AppTheme.AUTO)
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.app_theme_light),
                contentDescription = "浅色",
                Modifier
                    .size(size)
                    .modifier(AppTheme.Lite)
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = painterResource(R.drawable.app_theme_night),
                contentDescription = "深色",
                Modifier
                    .size(size)
                    .modifier(AppTheme.Dark)
            )
        }
    }
}

