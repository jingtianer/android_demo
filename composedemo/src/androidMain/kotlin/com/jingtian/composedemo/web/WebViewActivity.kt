package com.jingtian.composedemo.web

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.ui.theme.LocalAppColorScheme
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.ui.widget.CommonWebView
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import kotlinx.coroutines.launch

class WebViewActivity: BaseActivity() {

    private lateinit var uri: Uri
    private var storageId: Long = -1
    private var webView: CommonWebView? = null

    companion object {
        private const val TAG = "WebViewActivity"
        const val KEY_WEB_URI = "web_uri"
        const val KEY_STORAGE_ID = "web_storage_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getParcelableExtra<Uri>(KEY_WEB_URI)
        if (!intent.hasExtra(KEY_WEB_URI)){
            finish()
            return
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(KEY_WEB_URI, Uri::class.java)
        } else {
            intent.getParcelableExtra(KEY_WEB_URI)
        }
        storageId = intent.getLongExtra(KEY_STORAGE_ID, -1)
        if (uri == null) {
            finish()
            return
        }
        this.uri = uri
    }

    @Composable
    override fun Content() {
        AndroidWebView(Modifier.fillMaxSize().background(LocalAppColorScheme.current.background).appBackground().windowInsetsPadding(WindowInsets.systemBars), MultiplatformFileImpl(uri)) {
            webView = this
        }
    }

    override fun onBackPressed() {
        CoroutineUtils.runIOTask({
            val bitmap = webView?.takeSnapShot(this.window.decorView)
            if (bitmap != null) {
                BitMapCachePool.invalid(storageId, FileType.HTML)
                BitMapCachePool.loadImage(FileInfo(
                    storageId = storageId,
                    fileType = FileType.HTML,
                )) {
                    bitmap.asImageBitmap()
                }
            }
        }, onFailure = {
            setResult(2)
            Log.e(TAG, "WebViewActivity: onBackPressed: $it")
            super.onBackPressed()
        }) {
            setResult(Activity.RESULT_OK)
            super.onBackPressed()
        }
    }

    override fun shouldFitSystemBars(): Boolean {
        return false
    }
}

@Composable
fun AndroidWebView(modifier: Modifier = Modifier, file: MultiplatformFile?, enabled: Boolean = true, width: Dp? = null, height: Dp? = null, viewScope: CommonWebView.()->Unit = {}) {
    val uri = (file as? MultiplatformFileImpl)?.uri
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun CommonWebView.init() {
        scope.launch {
            suspendLoadUri(uri)
        }
        isEnabled = enabled
        viewScope()
    }
    AndroidView(factory = {
        CommonWebView(context).apply {
//            layoutParams = ViewGroup.LayoutParams(width?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?:ViewGroup.LayoutParams.MATCH_PARENT)
            init()
            initForSnapShot(width?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, height?.dpValue?.toInt()?: ViewGroup.LayoutParams.MATCH_PARENT, enable = enabled)
        }
    }, modifier = modifier)
}