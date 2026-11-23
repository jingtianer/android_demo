package com.jingtian.composedemo.main.route

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.MainState
import com.jingtian.composedemo.main.http.dao.HttpServerEntity
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
@Serializable
object HttpFeed : MainRouter()

interface IRouterController {
    suspend fun toGallery() = Unit

    suspend fun toSettings() = Unit

    suspend fun toDrawer() = Unit

    suspend fun toWebView(file: Pair<FileInfo?, MultiplatformFile?>) = Unit

    suspend fun toImageViewer(file: AlbumItemRelation) = Unit

    suspend fun toVideoPlayer(file: AlbumItemRelation) = Unit

    suspend fun toHttpServerFeed(serverEntity: HttpServerEntity, album: Album) = Unit

    fun isAnimating(): State<Boolean> = mutableStateOf(false)
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

    override suspend fun toImageViewer(file: AlbumItemRelation) {
        mainState.currentImageViewer = file
        naviHost.navigateTo(ImageViewer)
    }

    override suspend fun toVideoPlayer(file: AlbumItemRelation) {
        mainState.currentVideoPlayer = file
        naviHost.navigateTo(VideoPlayer)
    }

    override suspend fun toHttpServerFeed(serverEntity: HttpServerEntity, album: Album) {
        mainState.currentHttpFeed = serverEntity to album
        naviHost.navigateTo(HttpFeed)
    }

    override fun isAnimating(): State<Boolean> {
        return naviHost.isAnimating
    }
}