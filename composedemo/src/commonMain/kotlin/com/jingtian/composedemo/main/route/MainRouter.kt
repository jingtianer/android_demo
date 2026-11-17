package com.jingtian.composedemo.main.route

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import com.jingtian.composedemo.main.widget.NavRouter
import com.jingtian.composedemo.main.widget.SimpleNavHostController
import kotlinx.serialization.Serializable

@Serializable
sealed class MainRouter : NavRouter()
@Serializable
object Gallery : MainRouter()
@Serializable
object Settings : MainRouter()
@Serializable
object Drawer : MainRouter()

interface IRouterController {
    suspend fun toGallery() = Unit

    suspend fun toSettings() = Unit

    suspend fun toDrawer() = Unit
}

object EmptyRouterController: IRouterController {}

val LocalRouterController = compositionLocalOf<IRouterController>(structuralEqualityPolicy()) { EmptyRouterController }

class MainRouterController(private val naviHost: SimpleNavHostController): IRouterController {
    override suspend fun toGallery() {
        naviHost.navigateTo(Gallery)
    }

    override suspend fun toSettings() {
        naviHost.navigateTo(Settings)
    }

    override suspend fun toDrawer() {
        naviHost.navigateTo(Drawer)
    }
}