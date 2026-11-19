package com.jingtian.composedemo.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitView
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.toImageBitmap
import kotlinx.io.files.Path
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.numberWithInt
import platform.UIKit.UIImage
import platform.UIKit.UIView

var snapshotWebViewFactory: SnapshotWebViewFactory? = null

interface SnapshotWebViewFactory {
    fun newInstance(): ISnapshotWebView
}

interface ISnapshotWebView {
    suspend fun takeSnapshot(): UIImage
    fun getView(): UIView
    fun initForSnapshot(width: NSNumber?, height: NSNumber?, enabled: Boolean)
    fun loadFile(filePath: NSString)
}


@Composable
actual fun CommonWebView(
    modifier: Modifier,
    file: MultiplatformFile?,
    enabled: Boolean,
    width: Int?,
    height: Int?,
    viewScope: ICommonWebViewScope.() -> Unit
) {
//    logD("CommonWebView") {
//        "factory=${snapshotWebViewFactory}"
//    }
    val factory = snapshotWebViewFactory ?: return
    var contentFilePath by remember { mutableStateOf<Path?>(null) }
    LaunchedEffect(file) {
        contentFilePath = file?.file()
    }
    UIKitView(modifier = modifier, factory = {
        val webView = factory.newInstance()
        val webViewScope = object : ICommonWebViewScope {
            override suspend fun takeSnapshot(): ImageBitmap {
                return webView.takeSnapshot().toImageBitmap()
            }

            override fun initForSnapshot(width: Int?, height: Int?, enabled: Boolean) {
                webView.initForSnapshot(width?.let { NSNumber.numberWithInt(it) }, height?.let { NSNumber.numberWithInt(it) }, enabled)
            }
        }
        webViewScope.viewScope()
        webView.initForSnapshot(width?.let { NSNumber.numberWithInt(it) }, height?.let { NSNumber.numberWithInt(it) }, enabled)
        webView.loadFile((contentFilePath?.toString() ?: "/") as NSString)
        webView.getView()
    }, update = { webView->
        (webView as ISnapshotWebView).apply {
            webView.initForSnapshot(width?.let { NSNumber.numberWithInt(it) }, height?.let { NSNumber.numberWithInt(it) }, enabled)
            loadFile((contentFilePath?.toString() ?: "/") as NSString)
        }
    })
}