package com.jingtian.composedemo.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType.*
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.FileLinkProvider
import com.jingtian.composedemo.web.WebViewActivity

class MainActivity : BaseActivity() {
    @Composable
    override fun Content() = Main()

    override fun shouldFitSystemBars(): Boolean = false
}

fun systemFallbackIntent(context: Context, fileName: String, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType.mimeType
    val originFileUri = fileInfo.getFileUri()
    val originFile = originFileUri?.file
    val mediaUri: Uri = if (originFile != null) {
        val tmpLinkFile = FileLinkProvider.get(fileName, fileInfo)?.toFile()
        if (tmpLinkFile != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tmpLinkFile
            )
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                originFile
            )
        }
    } else {
        (originFileUri as MultiplatformFileImpl).uri
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

fun webIntent(context: Context, fileName: String, fileInfo: FileInfo) : Intent {
    return Intent(context, WebViewActivity::class.java).apply {
        putExtra(WebViewActivity.KEY_WEB_URI, (fileInfo.getFileUri() as? MultiplatformFileImpl)?.uri)
        putExtra(WebViewActivity.KEY_STORAGE_ID, fileInfo.storageId)
        putExtra(WebViewActivity.KEY_WEB_URI_EXTENSION, fileInfo.extension ?: "html")
    }
}

fun playIntent(context: Context, fileName: String, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType
    return when (mediaType) {
        HTML -> {
            webIntent(context, fileName, fileInfo)
        }

        IMAGE, VIDEO, AUDIO, RegularFile -> systemFallbackIntent(context, fileName, fileInfo)
    }
}
