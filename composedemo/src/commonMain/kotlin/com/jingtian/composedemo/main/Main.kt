package com.jingtian.composedemo.main

import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import com.jingtian.composedemo.multiplatform.WeakRef
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jingtian.composedemo.BuildKonfig
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.BackPressHandler
import com.jingtian.composedemo.base.JumpOnPurposeMarkHolder
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
import com.jingtian.composedemo.utils.observeAsState
import com.jingtian.composedemo.viewmodels.AlbumViewModel
import com.jingtian.composedemo.viewmodels.AuthSkip
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
expect fun rememberBackProgress(isEnable: Boolean): MutableFloatState

@OptIn(ExperimentalTime::class, ExperimentalComposeUiApi::class)
@Composable
fun PasswordGuardedMain(onPasswordChecked: () -> Unit = {}) {
    if (BuildKonfig.isDemo) {
        DemonstrationMain()
        return
    }
    val authViewModel: AuthViewModel = authViewModel()
    var isVisible by authViewModel.isVisible
    var isAuthed by authViewModel.isAuthed
    val isLocked by authViewModel.isLocked
    val doAuth = remember {
        {
            authViewModel.login { authResult ->
                when(authResult) {
                    is AuthSuccess -> {
                        onPasswordChecked()
                    }
                }
            }
        }
    }
    val lifecycleObserver by remember {
        mutableStateOf(object : DefaultLifecycleObserver {
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
        })
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

@Composable
fun Main(
    visible: Boolean = true,
    onVisibleRequest: ()->Unit = {}
) {
    if (BuildKonfig.isDemo) {
        DemonstrationMain()
        return
    }
    val naviController = rememberNavController()
    val navBackStackEntry by naviController.currentBackStackEntryAsState()
    val viewModel: AlbumViewModel = albumViewModel()
    var menuItemsEntity by remember { mutableStateOf(emptyList<Album>()) }
    var menuItemsEntityInitialized by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed, confirmStateChange = { drawerValue: DrawerValue ->
        when(drawerValue) {
            DrawerValue.Closed -> {
                val canClose = menuItemsEntity.isNotEmpty() || (navBackStackEntry?.destination?.hasRoute(Gallery::class)?.not() ?: true)
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
    })
    rememberCoroutineScope()
    val routerController = MainRouterController(naviController, drawerState)
    var currentSelectedAlbum by remember { mutableStateOf<IndexedValue<Album>?>(null) }
    val albumListChange by viewModel.albumListChange.observeAsState()

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
    if (BuildKonfig.isRemote) {
        LaunchedEffect(Unit) {
            viewModel.refreshRemote()
        }
    }
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
    val galleryStateHolderMap = remember(menuItemsEntity) {
        mutableStateMapOf<Long, SoftRef<GalleryStateHolder>>()
    }

    LowMemorySideEffect(galleryStateHolderMap) {
        galleryStateHolderMap.clear()
    }
    val scope = rememberCoroutineScope()

    val galleryStateHolder by remember(currentSelectedAlbum) {
        derivedStateOf {
            currentSelectedAlbum?.value?.albumId?.let {
                galleryStateHolderMap[it]?.get()
            }
        }
    }

    val isOnTop by remember {
        derivedStateOf {
            navBackStackEntry?.destination?.hasRoute(Gallery::class) ?: false
        }
    }
    galleryStateHolder?.BackPressHandler(isOnTop)
    val backProgress = rememberBackProgress(isOnTop && (galleryStateHolder?.let { !it.backEnable } ?: true))
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
        Box(modifier = Modifier.fillMaxSize().predictiveBackVisual(backProgress = backProgress, visible = visible)) {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                snackbarHost = {
                    GlobalSnapBar()
                }
            ) { _->
                ModalNavigationDrawer(
                    drawerContent = {
                        Box(Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(0.dp, 20.dp, 20.dp, 0.dp))
                            .background(LocalAppPalette.current.drawerBg)
                            .windowInsetsPadding(WindowInsets.navigationBars.only(
                                WindowInsetsSides.Vertical + WindowInsetsSides.Start)
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
                            slideIn { IntOffset(it.width, 0) }
                        },
                        exitTransition = {
                            slideOut { IntOffset(-it.width, 0) }
                        },
                        popEnterTransition = {
                            slideIn { IntOffset(-it.width, 0) }
                        },
                        popExitTransition = {
                            slideOut { IntOffset(it.width, 0) }
                        },
                    ) {
                        composable<Gallery> {
                            Gallery(galleryStateHolderMap, currentSelectedAlbum, menuItemsEntity, drawerState, naviController)
                        }
                        composable<Settings> {
                            SettingsPage()
                        }
                    }
                }
            }
            if (!visible) {
                Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemGestures).pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        onVisibleRequest()
                    })
                })
            }
        }
    }

    LaunchedEffect(navBackStackEntry, menuItemsEntityInitialized) {
        val canClose = menuItemsEntity.isNotEmpty() || (navBackStackEntry?.destination?.hasRoute(Gallery::class)?.not() ?: true)
        if (!canClose && menuItemsEntityInitialized) {
            val noAlbumToast = "没有任何合集，先创建合集吧"
            if (viewModel.currentBackgroundTask.value?.equals(noAlbumToast) != true) {
                viewModel.sendMessage(noAlbumToast)
            }
            drawerState.open()
        }
    }
}