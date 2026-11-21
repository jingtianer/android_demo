package com.jingtian.composedemo.main.route

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.main.MainState
import com.jingtian.composedemo.main.widget.NavRouter
import com.jingtian.composedemo.main.widget.SimpleNavHostController
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import kotlinx.serialization.Serializable

@Serializable
sealed class MainRouter : NavRouter()
@Serializable
object Gallery : MainRouter()
@Serializable
object Settings : MainRouter()
@Serializable
object Drawer : MainRouter()
@Serializable
object WebView : MainRouter()
@Serializable
object ImageViewer : MainRouter()
@Serializable
object VideoPlayer : MainRouter()
@Serializable
object Feed : MainRouter()

interface IRouterController {
    suspend fun toGallery() = Unit

    suspend fun toSettings() = Unit

    suspend fun toDrawer() = Unit

    suspend fun toWebView(file: Pair<FileInfo?, MultiplatformFile?>) = Unit

    suspend fun toImageViewer(file: MultiplatformFile?) = Unit

    suspend fun toVideoPlayer(file: MultiplatformFile?) = Unit
}

object EmptyRouterController: IRouterController {}

val LocalRouterController = compositionLocalOf<IRouterController>(structuralEqualityPolicy()) { EmptyRouterController }

class MainRouterController(private val naviHost: SimpleNavHostController, val mainState: MainState): IRouterController {
    override suspend fun toGallery() {
        naviHost.navigateTo(Gallery)
    }

    override suspend fun toSettings() {
        naviHost.navigateTo(Settings)
    }

    override suspend fun toDrawer() {
        naviHost.navigateTo(Drawer)
    }

    override suspend fun toWebView(file: Pair<FileInfo?, MultiplatformFile?>) {
        mainState.currentWebViewFile = file
        naviHost.navigateTo(WebView)
    }

    override suspend fun toImageViewer(file: MultiplatformFile?) {
        mainState.currentImageViewer = file
        if (file == null) {
            return
        }
        naviHost.navigateTo(ImageViewer)
    }

    override suspend fun toVideoPlayer(file: MultiplatformFile?) {
        mainState.currentVideoPlayer = file
        if (file == null) {
            return
        }
        naviHost.navigateTo(VideoPlayer)
    }
}