package com.jingtian.composedemo.main.route

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable

@Serializable
sealed class MainRouter
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

class MainRouterController(private val naviHost: NavHostController, private val drawerState: DrawerState): IRouterController {
    override suspend fun toGallery() {
        if (naviHost.currentBackStackEntry?.destination?.hasRoute(Gallery::class) == true) {
            // NOP
        } else if (naviHost.popBackStack<Gallery>(inclusive = false)) {
            // NOP
        } else {
            naviHost.navigate(Gallery) {
                launchSingleTop = true
            }
        }
        drawerState.close()
    }

    override suspend fun toSettings() {
        if (naviHost.currentBackStackEntry?.destination?.hasRoute(Settings::class) == true) {
            // NOP
        } else if (naviHost.popBackStack<Settings>(inclusive = false)) {
            // NOP
        } else {
            naviHost.navigate(Settings) {
                launchSingleTop = true
            }
        }
        drawerState.close()
    }
}