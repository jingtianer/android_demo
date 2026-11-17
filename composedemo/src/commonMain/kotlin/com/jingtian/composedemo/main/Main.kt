package com.jingtian.composedemo.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.jingtian.composedemo.main.PredictiveBackVisualState.Companion.STATE_CANCEL
import com.jingtian.composedemo.main.PredictiveBackVisualState.Companion.STATE_END
import com.jingtian.composedemo.main.PredictiveBackVisualState.Companion.STATE_IDLE
import com.jingtian.composedemo.main.PredictiveBackVisualState.Companion.STATE_START
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
import com.jingtian.composedemo.main.widget.SizeTransitionCreator
import com.jingtian.composedemo.main.widget.rememberSimpleNavHostController
import com.jingtian.composedemo.multiplatform.LowMemorySideEffect
import com.jingtian.composedemo.multiplatform.SoftRef
import com.jingtian.composedemo.multiplatform.logD
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
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
expect fun rememberNativeBackInvoker(): (suspend () -> Unit)

private class PredictiveBackVisualState {
    val anim = Animatable(initialValue = 1f)
    val progress = mutableFloatStateOf(0f)
    val swipeEdge = mutableIntStateOf(BackEventCompat.EDGE_LEFT)
    val isEnable = mutableStateOf(false)
    val backState = mutableIntStateOf(STATE_IDLE)

    companion object {
        const val STATE_IDLE = 0
        const val STATE_START = 1
        const val STATE_END = 2
        const val STATE_CANCEL = 3
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberPredictiveBackVisualState(
    isEnable: Boolean,
    animationSpec: (Float, Float) -> AnimationSpec<Float> = { targetValue: Float, currentValue: Float ->
        val leftAnimDuration = (abs(targetValue - currentValue) * 380).roundToInt()
        tween(
            durationMillis = leftAnimDuration,
            easing = FastOutSlowInEasing
        )
    },
    onPredicableBackFinish: suspend () -> Unit = {},
): State<Float> {
    val state = remember { PredictiveBackVisualState() }
    val authViewModel = authViewModel()
    val isVisible by authViewModel.isVisible
    val isLocked by authViewModel.isLocked
    val anim = state.anim
    var backState by state.backState
    var progress by state.progress
    var swipeEdge by state.swipeEdge
    var enable by state.isEnable
    val externalScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember {
        object : DefaultLifecycleObserver {
            private fun resetState() {
                enable = true
                backState = STATE_IDLE
                progress = 0f
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
            progress = 0f
            backState = STATE_START
            backFlow.collect { event ->
                progress = event.progress
                swipeEdge = event.swipeEdge
            }
            progress = 1f
            backState = STATE_END
            enable = false
        } catch (e: CancellationException) {
            progress = 0f
            backState = STATE_CANCEL
            enable = true
            throw e
        } finally {

        }
    }
    LaunchedEffect(progress, isVisible, isLocked, backState) {
        if ((backState == STATE_CANCEL || backState == STATE_IDLE) && !isLocked && isVisible) {
            if (anim.value <= 0 || (anim.isRunning && anim.targetValue <= 0f)) {
                return@LaunchedEffect
            }
            if (backState == STATE_IDLE) {
                anim.snapTo(1f)
            }
            anim.animateTo(targetValue = 0f, animationSpec(0f, anim.value))
        } else if (isLocked || !isVisible || backState == STATE_END) {
//            if (anim.value >= 1f || (anim.isRunning && anim.targetValue >= 1f)) {
//                return@LaunchedEffect
//            }
            if (!isVisible) {
                anim.snapTo(1f)
            } else {
                anim.animateTo(targetValue = 1f, animationSpec = animationSpec(1f, anim.value))
            }
            if (backState == STATE_END) {
                withFrameNanos { }
                externalScope.launch(Dispatchers.Main) {
                    onPredicableBackFinish.invoke()
                }
            }
        } else {
            anim.snapTo(progress)
        }
    }
    return anim.asState()
}

@Composable
fun PasswordGuardedMain(onPasswordChecked: () -> Unit = {}) {
    if (BuildKonfig.isDemo) {
        DemonstrationMain()
        return
    }
    val authViewModel: AuthViewModel = authViewModel()
    val isVisible by authViewModel.isVisible
    val isLocked by authViewModel.isLocked

    fun doAuth() {
        authViewModel.login { authResult ->
            when (authResult) {
                is AuthSuccess -> {
                    onPasswordChecked()
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
    Main(visible = isVisible && !isLocked) {
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
        const val TRANSITION_ANIM_DURATION = 360
        const val ENTER_INITIAL_SCALE: Float = 0.8f
    }

    var menuItemsEntity by mutableStateOf(emptyList<Album>())
    var menuItemsEntityInitialized by mutableStateOf(false)
    val viewModel: AlbumViewModel = albumViewModel()
    var currentSelectedAlbum by mutableStateOf<IndexedValue<Album>?>(null)
    val albumListChange by viewModel.albumListChange
    val galleryStateHolderMap = mutableStateMapOf<Long, SoftRef<GalleryStateHolder>>()

    val navBackStackEntry get() = naviController.currentBackStackEntry

    val galleryStateHolder get() = currentSelectedAlbum?.value?.albumId?.let {
        galleryStateHolderMap[it]?.get()
    }

    val isOnTop get() = navBackStackEntry == Gallery

    val isBackEnable: Boolean get() {
        val canClose = menuItemsEntity.isNotEmpty() || !isOnTop
        return (drawerState.isOpen) && canClose
    }

    val drawerState = DrawerState(
        initialValue = DrawerValue.Closed,
    )

    val routerController = MainRouterController(naviController, drawerState)

    val enterTransition: EnterTransitionCreator = { swipeEdge ->
        val orientation = when (swipeEdge) {
            BackEventCompat.EDGE_LEFT -> 1
            else -> -1
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
        ) { IntOffset(orientation * it.width, 0) }
        scale + slide
    }

    val sizeTransition : SizeTransitionCreator = { _ ->
        null
    }

    val exitTransitionCreator: ExitTransitionCreator = { swipeEdge ->
        val orientation = when (swipeEdge) {
            BackEventCompat.EDGE_LEFT -> 1
            else -> -1
        }
        val slide = slideOut(
            animationSpec = tween(
                durationMillis = TRANSITION_ANIM_DURATION,
                easing = LinearEasing
            ),
            targetOffset = {
                IntOffset(
                    orientation * it.width,
                    0
                )
            }
        )
        val scale = scaleOut(
            animationSpec = keyframes { // ios要用keyframe 大小才不会乱
                durationMillis = TRANSITION_ANIM_DURATION
                1f at 0
                ENTER_INITIAL_SCALE at TRANSITION_ANIM_DURATION
            },
            targetScale = ENTER_INITIAL_SCALE
        )
        slide + scale
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
            }
        }

        // 内存紧张信号
        LowMemorySideEffect(galleryStateHolderMap) {
            galleryStateHolderMap.clear()
        }

        // 导航栈变化 初始化完成
        LaunchedEffect(drawerState.isClosed, menuItemsEntityInitialized, isOnTop, drawerState.isAnimationRunning) {
            val canClose = menuItemsEntity.isNotEmpty() || !isOnTop
            if (!canClose && drawerState.isClosed && menuItemsEntityInitialized && !drawerState.isAnimationRunning) {
                val noAlbumToast = "没有任何合集，先创建合集吧"
                if (viewModel.currentBackgroundTask.value?.equals(noAlbumToast) != true) {
                    viewModel.sendMessage(noAlbumToast)
                }
                drawerState.open()
            }
        }
    }

    @Composable
    fun InstallBackHandlers(): State<Float> {
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
        return rememberPredictiveBackVisualState(isOnTop && !backEnable) {
            nativeBackInvoker.invoke()
            authViewModel().lock()
        }
    }
}

@Composable
private fun rememberMainState(): MainState {
    val naviController = rememberSimpleNavHostController()
    return remember {
        MainState(naviController = naviController,)
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
    val predictiveBackVisualState = InstallBackHandlers()
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
            modifier = Modifier.fillMaxSize().predictiveBackVisual(predictiveBackVisualState = predictiveBackVisualState)
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
                        sizeTransition = sizeTransition,
                        mapBuilder = remember(menuItemsEntity) {
                            {
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