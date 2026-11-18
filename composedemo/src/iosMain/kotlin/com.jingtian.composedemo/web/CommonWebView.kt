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
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.multiplatform.toImageBitmap
import kotlinx.io.files.Path
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.numberWithInt
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.darwin.NSInteger

var snapshotWebViewFactory: SnapshotWebViewFactory? = null

interface SnapshotWebViewFactory {
    fun newInstance(): ISnapshotWebView
}

interface ISnapshotWebView {
    suspend fun tackSnapShot(): UIImage
    fun getView(): UIView
    fun initForSnapShot(width: NSNumber?, height: NSNumber?, enabled: Boolean)
    fun loadFile(filePath: NSString)
}


@Composable
actual fun CommonWebView(
    modifier: Modifier,
    file: MultiplatformFile?,
    enabled: Boolean,
    width: Dp?,
    height: Dp?,
    viewScope: ICommonWebViewScope.() -> Unit
) {
    logD("CommonWebView") {
        "factory=${snapshotWebViewFactory}"
    }
    val factory = snapshotWebViewFactory ?: return
    var contentFilePath by remember { mutableStateOf<Path?>(null) }
    LaunchedEffect(file) {
        contentFilePath = file?.file()
    }
    UIKitView(modifier = modifier, factory = {
        val webView = factory.newInstance()
        val webViewScope = object : ICommonWebViewScope {
            override suspend fun tackSnapShot(): ImageBitmap {
                return webView.tackSnapShot().toImageBitmap()
            }

            override fun initForSnapShot(width: Int?, height: Int?, enabled: Boolean) {
                webView.initForSnapShot(width?.let { NSNumber.numberWithInt(it) }, height?.let { NSNumber.numberWithInt(it) }, enabled)
            }
        }
        webViewScope.viewScope()
        webView.loadFile((contentFilePath?.toString() ?: "/") as NSString)
        webView.getView()
    }, update = { webView->
        (webView as ISnapshotWebView).loadFile((contentFilePath?.toString() ?: "/") as NSString)
    })
}