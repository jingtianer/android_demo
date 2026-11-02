package com.jingtian.composedemo.main.route

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.navigation.NavHostController

enum class MainRouter {
    Gallery,
    Settings,
}


interface IRouterController {
    suspend fun toGallery() = Unit

    suspend fun toSettings() = Unit
}

object EmptyRouterController: IRouterController {}

val LocalRouterController = compositionLocalOf<IRouterController>(structuralEqualityPolicy()) { EmptyRouterController }

class MainRouterController(private val naviHost: NavHostController, private val drawerState: DrawerState): IRouterController {
    override suspend fun toGallery() {
        naviHost.navigate(MainRouter.Gallery.name) {
            launchSingleTop = true
        }
        drawerState.close()
    }

    override suspend fun toSettings() {
        naviHost.navigate(MainRouter.Settings.name) {
            launchSingleTop = true
        }
        drawerState.close()
    }
}