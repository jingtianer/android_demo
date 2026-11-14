package com.jingtian.composedemo.main

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.base.BackPressHandler
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.demonstration.DemonstrationMain
import com.jingtian.composedemo.main.drawer.MainDrawer
import com.jingtian.composedemo.main.gallery.Gallery
import com.jingtian.composedemo.main.gallery.GalleryStateHolder
import com.jingtian.composedemo.main.gallery.predictiveBackVisual
import com.jingtian.composedemo.main.route.Gallery
import com.jingtian.composedemo.main.route.LocalRouterController
import com.jingtian.composedemo.main.route.MainRouterController
import com.jingtian.composedemo.main.route.Settings
import com.jingtian.composedemo.main.settings.SettingsPage
import com.jingtian.composedemo.multiplatform.LowMemorySideEffect
import com.jingtian.composedemo.multiplatform.SoftRef
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.clipDeviceRoundCorner
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthSuccess
import com.jingtian.composedemo.viewmodels.AuthViewModel
import com.jingtian.composedemo.viewmodels.GlobalSnapBar
import com.jingtian.composedemo.viewmodels.albumViewModel
import com.jingtian.composedemo.viewmodels.authViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class BackProgressState(backProgressState: MutableFloatState, isLeftState: MutableState<Boolean>) {
    val backProgress: FloatState = backProgressState
    val isLeft: State<Boolean> = isLeftState
}

@Composable
expect fun rememberBackProgress(
    isEnable: Boolean,
    onPredicableBackFinish: () -> Unit = {}
): BackProgressState

@Composable
fun PasswordGuardedMain(onPasswordChecked: () -> Unit = {}) {
    if (BuildKonfig.isDemo) {
        DemonstrationMain()
        return
    }
    val authViewModel: AuthViewModel = authViewModel()
    val isVisible by authViewModel.isVisible
    val isAuthed by authViewModel.isAuthed
    val isLocked by authViewModel.isLocked
    val doAuth = remember {
        {
            authViewModel.login { authResult ->
                when (authResult) {
                    is AuthSuccess -> {
                        onPasswordChecked()
                    }
                }
            }
        }
    }
    val lifecycleObserver = remember {
        object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                authViewModel.onStop(owner)
            }

            override fun onPause(owner: LifecycleOwner) {
                authViewModel.onPause(owner)
            }

            override fun onResume(owner: LifecycleOwner) {
                authViewModel.onResume(owner)
                doAuth()
            }

            override fun onStart(owner: LifecycleOwner) {
                authViewModel.onStart(owner)
            }
        }
    }
    Main(visible = isVisible && isAuthed && !isLocked) {
        doAuth()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(observer = lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer = lifecycleObserver)
        }
    }
}

class MainState(
    val naviController: NavHostController,
    currentBackStackEntryState: State<NavBackStackEntry?>,
) {
    var menuItemsEntity by mutableStateOf(emptyList<Album>())
    var menuItemsEntityInitialized by mutableStateOf(false)
    val viewModel: AlbumViewModel = albumViewModel()
    var currentSelectedAlbum by mutableStateOf<IndexedValue<Album>?>(null)
    val albumListChange by viewModel.albumListChange
    val galleryStateHolderMap = mutableStateMapOf<Long, SoftRef<GalleryStateHolder>>()

    val navBackStackEntry by currentBackStackEntryState

    val galleryStateHolder by derivedStateOf {
        currentSelectedAlbum?.value?.albumId?.let {
            galleryStateHolderMap[it]?.get()
        }
    }
    val isOnTop by derivedStateOf {
        navBackStackEntry?.destination?.hasRoute(Gallery::class) ?: false
    }

    val isBackEnable by derivedStateOf {
        drawerState.isOpen || drawerState.isAnimationRunning
    }

    val drawerState = DrawerState(
        initialValue = DrawerValue.Closed,
        confirmStateChange = this::confirmStateChange
    )

    val routerController = MainRouterController(naviController, drawerState)

    fun updateSelectedValue(list: List<Album>): IndexedValue<Album>? {
        val currentSelectedAlbum = currentSelectedAlbum
        if (currentSelectedAlbum != null) {
            val lastSelectedIndex = currentSelectedAlbum.index
            val lastSelectedAlbumId = currentSelectedAlbum.value.albumId
            val findId = list.withIndex().find { it.value.albumId == lastSelectedAlbumId }
            if (findId != null) {
                return findId
            }
            if (lastSelectedIndex < list.size) {
                return IndexedValue(lastSelectedIndex, list.get(lastSelectedIndex))
            }
            return list.asReversed().withIndex().firstOrNull()
        } else {
            return list.withIndex().firstOrNull()
        }
    }

    private fun confirmStateChange(drawerValue: DrawerValue): Boolean {
        return when (drawerValue) {
            DrawerValue.Closed -> {
                val canClose =
                    menuItemsEntity.isNotEmpty() || (navBackStackEntry?.destination?.hasRoute(
                        Gallery::class
                    )?.not() ?: true)
                if (!canClose) {
                    val noAlbumToast = "没有任何合集，先创建合集吧"
                    if (viewModel.currentBackgroundTask.value?.equals(noAlbumToast) != true) {
                        viewModel.sendMessage(noAlbumToast)
                    }
                }
                canClose
            }

            DrawerValue.Open -> true
        }
    }

    @Composable
    fun InstallSideEffects() {
        // 登录后拉取远端数据
        if (BuildKonfig.isRemote) {
            LaunchedEffect(Unit) {
                viewModel.refreshRemote()
            }
        }

        // 收到albumListChange通知 更新数据
        LaunchedEffect(albumListChange) {
            withContext(Dispatchers.IO) {
                val value = viewModel.menuItems()
                val nextSelectedAlbum = updateSelectedValue(value)
                withContext(Dispatchers.Main) {
                    menuItemsEntity = value
                    currentSelectedAlbum = nextSelectedAlbum
                    menuItemsEntityInitialized = true
                }
                if (value.isEmpty()) {
                    drawerState.snapTo(DrawerValue.Open)
                }
            }
        }

        // 内存紧张信号
        LowMemorySideEffect(galleryStateHolderMap) {
            galleryStateHolderMap.clear()
        }

        // 导航栈变化 初始化完成
        LaunchedEffect(navBackStackEntry, menuItemsEntityInitialized) {
            val canClose =
                menuItemsEntity.isNotEmpty() || (navBackStackEntry?.destination?.hasRoute(Gallery::class)
                    ?.not() ?: true)
            if (!canClose && menuItemsEntityInitialized) {
                val noAlbumToast = "没有任何合集，先创建合集吧"
                if (viewModel.currentBackgroundTask.value?.equals(noAlbumToast) != true) {
                    viewModel.sendMessage(noAlbumToast)
                }
                drawerState.open()
            }
        }
    }

    @Composable
    fun InstallBackHandlers(): BackProgressState {
        val galleryBackEnable = galleryStateHolder?.backEnable ?: false
        val backEnable = isBackEnable || galleryBackEnable
        BackPressHandler(backEnable) {
            if (drawerState.isOpen) {
                drawerState.close()
                true
            } else if (drawerState.isAnimationRunning) {
                true
            } else if (galleryStateHolder?.onBackPressed() == true) {
                true
            } else {
                false
            }
        }
        return rememberBackProgress(isOnTop && !backEnable) {
            authViewModel().lock()
        }
    }
}

@Composable
private fun rememberMainState(): MainState {
    val naviController = rememberNavController()
    val currentBackStackEntryState = naviController.currentBackStackEntryAsState()
    return remember {
        MainState(
            naviController = naviController,
            currentBackStackEntryState = currentBackStackEntryState,
        )
    }
}

@Composable
fun Main(
    visible: Boolean = true,
    onVisibleRequest: () -> Unit = {}
) {
    val mainState = rememberMainState()
    mainState.Main(visible, onVisibleRequest)
}

@Composable
fun MainState.Main(
    visible: Boolean = true,
    onVisibleRequest: () -> Unit = {}
) {
    if (BuildKonfig.isDemo) {
        DemonstrationMain()
        return
    }

    val scope = rememberCoroutineScope()
    val backProgressState = InstallBackHandlers()
    val backProgress = backProgressState.backProgress
    val modifier = if (visible) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                while (true) {
                    awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
        }
    }
    CompositionLocalProvider(LocalRouterController provides routerController) {
        Box(
            modifier = Modifier.fillMaxSize()
                .predictiveBackVisual(backProgress = backProgress, visible = visible)
        ) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = {
                    GlobalSnapBar()
                }
            ) { _ ->
                ModalNavigationDrawer(
                    drawerContent = {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.92f)
                                .clipDeviceRoundCorner()
                                .background(LocalAppPalette.current.drawerBg)
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars.only(
                                        WindowInsetsSides.Vertical + WindowInsetsSides.Start
                                    )
                                )
                        ) {
                            MainDrawer(menuItemsEntity) { index, album ->
                                currentSelectedAlbum = IndexedValue(index, album)
                                scope.launch {
                                    routerController.toGallery()
                                }
                            }
                        }
                    },
                    Modifier.fillMaxSize(),
                    drawerState = drawerState,
                    gesturesEnabled = true
                ) {
                    NavHost(
                        navController = naviController,
                        startDestination = Gallery,
                        enterTransition = {
                            slideIn { IntOffset((it.width * 0.2f).roundToInt(), 0) } + scaleIn(initialScale = 0.8f)
                        },
                        exitTransition = {
                            slideOut { IntOffset((it.width * 0.2f).roundToInt(), 0) } + scaleOut(targetScale = 0.8f)
                        },
                        popEnterTransition = {
                            slideIn { IntOffset((it.width * 0.2f).roundToInt(), 0) } + scaleIn(initialScale = 0.8f)
                        },
                        popExitTransition = {
                            slideOut { IntOffset((it.width * 0.2f).roundToInt(), 0) } + scaleOut(targetScale = 0.8f)
                        },
                    ) {
                        composable<Gallery> {
                            Gallery(
                                galleryStateHolderMap,
                                currentSelectedAlbum,
                                menuItemsEntity,
                                drawerState,
                                naviController
                            )
                        }
                        composable<Settings> {
                            SettingsPage()
                        }
                    }
                }
            }
            if (!visible) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemGestures).pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                onVisibleRequest()
                            })
                        })
            }
        }
    }

    InstallSideEffects()
}