package com.jingtian.composedemo.web

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.ui.widget.CommonWebView
import kotlinx.coroutines.launch

class WebViewActivity: BaseActivity() {

    private lateinit var uri: Uri

    companion object {
        const val KEY_WEB_URI = "web_uri"
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
        if (uri == null) {
            finish()
            return
        }
        this.uri = uri
    }

    @Composable
    override fun Content() {
        CommonWebView(Modifier.fillMaxSize(), uri)
    }
}

@Composable
fun CommonWebView(modifier: Modifier = Modifier, uri: Uri?, enabled: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    fun CommonWebView.init() {
        scope.launch {
            suspendLoadUri(uri)
        }
        isEnabled = enabled
    }
    AndroidView(factory = {
        CommonWebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            init()
        }
    }, modifier = modifier)
}