package com.jingtian.composedemo.main.route

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.jingtian.composedemo.main.widget.NavRouter
import com.jingtian.composedemo.main.widget.SimpleNavHostController
import kotlinx.serialization.Serializable

@Serializable
sealed class MainRouter : NavRouter()
@Serializable
object Gallery : MainRouter()
@Serializable
object Settings : MainRouter()


interface IRouterController {
    suspend fun toGallery() = Unit

    suspend fun toSettings() = Unit
}

object EmptyRouterController: IRouterController {}

val LocalRouterController = compositionLocalOf<IRouterController>(structuralEqualityPolicy()) { EmptyRouterController }

class MainRouterController(private val naviHost: SimpleNavHostController, private val drawerState: DrawerState): IRouterController {
    override suspend fun toGallery() {
        drawerState.close()
        naviHost.navigateTo(Gallery)
    }

    override suspend fun toSettings() {
        drawerState.close()
        naviHost.navigateTo(Settings)
    }
}