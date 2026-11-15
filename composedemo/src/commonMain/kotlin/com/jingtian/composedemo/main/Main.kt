package com.jingtian.composedemo.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.jingtian.composedemo.main.widget.BackEventCompat
import com.jingtian.composedemo.main.widget.EnterTransitionCreator
import com.jingtian.composedemo.main.widget.ExitTransitionCreator
import com.jingtian.composedemo.main.widget.SimpleNavHost
import com.jingtian.composedemo.main.widget.SimpleNavHostController
import com.jingtian.composedemo.main.widget.rememberSimpleNavHostController
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class BackProgressState(backProgressState: MutableFloatState, isLeftState: MutableState<Boolean>) {
    val backProgress: FloatState = backProgressState
    val isLeft: State<Boolean> = isLeftState
}

@Composable
expect fun rememberNativeBackInvoker(): (suspend () -> Unit)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberBackProgress(
    isEnable: Boolean,
    onPredicableBackFinish: suspend () -> Unit
): BackProgressState {
    val backProgress = remember { mutableFloatStateOf(0f) }
    val isLeft = remember { mutableStateOf(true) }
    var enable by remember { mutableStateOf(true) }
    val externalScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember {
        object : DefaultLifecycleObserver {
            private fun resetState() {
                enable = true
                backProgress.value = 0f
            }

            override fun onStart(owner: LifecycleOwner) {
                resetState()
            }

            override fun onResume(owner: LifecycleOwner) {
                resetState()
            }
        }
    }
    DisposableEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(observer = lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer = lifecycleObserver)
        }
    }
    PredictiveBackHandler(enabled = enable && isEnable) { backFlow ->
        try {
            backProgress.value = 0f
            backFlow.collect { event ->
                backProgress.value = event.progress
                when (event.swipeEdge) {
                    BackEventCompat.EDGE_LEFT -> {
                        isLeft.value = true
                    }

                    BackEventCompat.EDGE_RIGHT -> {
                        isLeft.value = false
                    }
                }
            }
            backProgress.value = 1f
            enable = false
            withFrameNanos { }
            externalScope.launch(Dispatchers.Main) {
                onPredicableBackFinish.invoke()
            }
        } catch (e: CancellationException) {
            backProgress.value = 0f
            enable = true
            throw e
        } finally {

        }
    }
    return remember { BackProgressState(backProgress, isLeft) }
}

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

@OptIn(ExperimentalComposeUiApi::class)
private class MainState(
    val naviController: SimpleNavHostController,
) {
    companion object {
        const val EXIT_FATEOUT_DURATION = 100
        const val TRANSITION_ANIM_DURATION = 300
        const val EXIT_SCALE_SLIDE_DURATION = TRANSITION_ANIM_DURATION - EXIT_FATEOUT_DURATION
        const val EXIT_TARTGET_SCALE = 0.8f
        const val EXIT_TARGET_OFFSET: Float =
            EXIT_SCALE_SLIDE_DURATION * 1f / TRANSITION_ANIM_DURATION
        const val ENTER_INITIAL_SCALE: Float = 1 - ((1 - EXIT_TARTGET_SCALE) / EXIT_TARGET_OFFSET)
    }

    var menuItemsEntity by mutableStateOf(emptyList<Album>())
    var menuItemsEntityInitialized by mutableStateOf(false)
    val viewModel: AlbumViewModel = albumViewModel()
    var currentSelectedAlbum by mutableStateOf<IndexedValue<Album>?>(null)
    val albumListChange by viewModel.albumListChange
    val galleryStateHolderMap = mutableStateMapOf<Long, SoftRef<GalleryStateHolder>>()

    val navBackStackEntry get() = naviController.currentBackStackEntry

    val galleryStateHolder by derivedStateOf {
        currentSelectedAlbum?.value?.albumId?.let {
            galleryStateHolderMap[it]?.get()
        }
    }
    val isOnTop by derivedStateOf {
        navBackStackEntry == Gallery
    }

    val isBackEnable by derivedStateOf {
        val canClose = menuItemsEntity.isNotEmpty() || !isOnTop
        (drawerState.isOpen) && canClose
    }

    val drawerState = DrawerState(
        initialValue = DrawerValue.Closed,
        confirmStateChange = this::confirmStateChange
    )

    val routerController = MainRouterController(naviController, drawerState)

    val enterTransition: EnterTransitionCreator = { swipeEdge ->
        val isLeft = when (swipeEdge) {
            BackEventCompat.EDGE_LEFT -> {
                true
            }
            else -> {
                false
            }
        }
        val scale = scaleIn(
            initialScale = ENTER_INITIAL_SCALE,
            animationSpec = tween(
                durationMillis = TRANSITION_ANIM_DURATION,
                easing = LinearEasing
            )
        )
        val slide = slideIn(
            animationSpec = tween(
                durationMillis = TRANSITION_ANIM_DURATION,
                easing = LinearEasing
            )
        ) { IntOffset((if (isLeft) 1 else -1) * it.width, 0) }
        scale + slide
    }

    val exitTransitionCreator: ExitTransitionCreator = { swipeEdge ->
        val isLeft = when (swipeEdge) {
            BackEventCompat.EDGE_LEFT -> {
                true
            }
            else -> {
                false
            }
        }
        val slide = slideOut(
            animationSpec = tween(
                durationMillis = EXIT_SCALE_SLIDE_DURATION,
                easing = LinearEasing
            ),
            targetOffset = {
                IntOffset(
                    ((if (isLeft) 1 else -1) * it.width * EXIT_TARGET_OFFSET).roundToInt(),
                    0
                )
            }
        )
        val scale = scaleOut(
            animationSpec = tween(
                durationMillis = EXIT_SCALE_SLIDE_DURATION,
                easing = LinearEasing
            ),
            targetScale = EXIT_TARTGET_SCALE
        )
        val fade = fadeOut(
            animationSpec = keyframes {
                durationMillis = TRANSITION_ANIM_DURATION
                1f at EXIT_SCALE_SLIDE_DURATION
                0f at TRANSITION_ANIM_DURATION
            }
        )
        slide + scale + fade
    }

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
                    menuItemsEntity.isNotEmpty() || !isOnTop
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
        LaunchedEffect(navBackStackEntry, drawerState.isClosed, menuItemsEntityInitialized) {
            val canClose = menuItemsEntity.isNotEmpty() || !isOnTop
            if (!canClose && drawerState.isClosed && menuItemsEntityInitialized) {
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
            val canClose = menuItemsEntity.isNotEmpty() || !isOnTop
            if (drawerState.isOpen && canClose) {
                drawerState.close()
                true
            } else if (galleryStateHolder?.onBackPressed() == true) {
                true
            } else {
                false
            }
        }
        val nativeBackInvoker = rememberNativeBackInvoker()
        return rememberBackProgress(isOnTop && !backEnable) {
            nativeBackInvoker.invoke()
            authViewModel().lock()
        }
    }
}

@Composable
private fun rememberMainState(): MainState {
    val naviController = rememberSimpleNavHostController()
    return remember {
        MainState(
            naviController = naviController,
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
private fun MainState.Main(
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
                    SimpleNavHost(
                        simpleNavHostController = naviController,
                        initialDestination = Gallery,
                        enterTransition = enterTransition,
                        exitTransition = exitTransitionCreator,
                        mapBuilder = {
                            composable(Gallery) {
                                Gallery(
                                    galleryStateHolderMap,
                                    currentSelectedAlbum,
                                    menuItemsEntity,
                                    drawerState,
                                    naviController
                                )
                            }

                            composable(Settings) {
                                SettingsPage()
                            }
                        }
                    )
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