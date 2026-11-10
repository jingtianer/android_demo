package com.jingtian.composedemo.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import com.jingtian.composedemo.base.JumpOnPurposeMarkHolder
import com.jingtian.composedemo.dao.model.Album
import com.jingtian.composedemo.demonstration.DemonstrationMain
import com.jingtian.composedemo.main.drawer.MainDrawer
import com.jingtian.composedemo.main.gallery.Gallery
import com.jingtian.composedemo.main.gallery.GalleryStateHolder
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
import com.jingtian.composedemo.viewmodels.AuthSuccess
import com.jingtian.composedemo.viewmodels.AuthViewModel
import com.jingtian.composedemo.viewmodels.albumViewModel
import com.jingtian.composedemo.viewmodels.authViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MIN_ON_STOP_DO_AUTH_TIME_GAP = 30 * 1000L

@OptIn(ExperimentalTime::class)
@Composable
fun PasswordGuardedMain(onPasswordChecked: () -> Unit = {}) {
    if (BuildKonfig.isDemo) {
        DemonstrationMain()
        return
    }
    val authViewModel: AuthViewModel = authViewModel()
    var isVisible by remember { mutableStateOf(false) }
    var isAuthed by remember { mutableStateOf(false) }
    var isLocked by authViewModel.isLocked
    var lastOnStopAuthTimeStamp by remember { mutableLongStateOf(-1) }
    val doAuth = remember {
        {
            authViewModel.doAuth { authResult ->
                when(authResult) {
                    is AuthSuccess -> {
                        lastOnStopAuthTimeStamp = Clock.System.now().toEpochMilliseconds()
                        isVisible = true
                        isAuthed = true
                        onPasswordChecked()
                    }
                }
            }
        }
    }
    fun checkShouldAuth() {
        val isJumpOnPurpose = JumpOnPurposeMarkHolder.isJumpOnPurpose.getAndSet(false)
        val isAppLockedSnapshot = isLocked
        isLocked = false
        val shouldDoAuth = isAppLockedSnapshot || !isAuthed && !isJumpOnPurpose && !(lastOnStopAuthTimeStamp != -1L && Clock.System.now().toEpochMilliseconds() - lastOnStopAuthTimeStamp <= MIN_ON_STOP_DO_AUTH_TIME_GAP)
        if (shouldDoAuth) {
            doAuth()
        } else {
            isAuthed = true
            isVisible = true
        }
    }
    val lifecycleObserver by remember {
        mutableStateOf(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                isAuthed = false
            }

            override fun onPause(owner: LifecycleOwner) {
                isVisible = false
            }

            @OptIn(ExperimentalTime::class)
            override fun onResume(owner: LifecycleOwner) {
                checkShouldAuth()
            }

            override fun onStart(owner: LifecycleOwner) {
            }
        })
    }
    Main(visible = isVisible && isAuthed && !isLocked) {
        checkShouldAuth()
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
    val snackBarMessage by viewModel.currentBackgroundTask.observeAsState()

    val galleryStateHolderMap = remember(menuItemsEntity) {
        mutableStateMapOf<Long, SoftRef<GalleryStateHolder>>()
    }

    LowMemorySideEffect(galleryStateHolderMap) {
        galleryStateHolderMap.clear()
    }

    var showSnackBar by remember { mutableStateOf(snackBarMessage != null) }
    var showSnackBarAnim by remember { mutableStateOf(true) }

    LaunchedEffect(snackBarMessage) {
        if (showSnackBar && snackBarMessage.isNullOrBlank()) {
            showSnackBarAnim = true
        } else if (!showSnackBar && !snackBarMessage.isNullOrBlank()) {
            showSnackBarAnim = true
        } else {
            showSnackBarAnim = false
        }
        showSnackBar = !snackBarMessage.isNullOrBlank()
    }
    val scope = rememberCoroutineScope()
    val modifier = if (visible) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxSize().blur(40.dp).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial).consume()
                waitForUpOrCancellation(pass = PointerEventPass.Initial)?.consume()
                onVisibleRequest()
            }
        }
    }
    CompositionLocalProvider(LocalRouterController provides routerController) {
        Scaffold(
            modifier = modifier,
            snackbarHost = {
                AnimatedVisibility(
                    visible = showSnackBar,
                    enter = if(showSnackBarAnim) fadeIn() + expandIn() else EnterTransition.None,
                    exit = if(showSnackBarAnim) shrinkOut() + fadeOut() else ExitTransition.None,
                ) {
                    Snackbar(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        AppThemeText(snackBarMessage ?: "", style = LocalTextStyle.current.copy(color = LocalAppPalette.current.dialogBg), maxLines = 1)
                    }
                }
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
                NavHost(navController = naviController, startDestination = Gallery) {
                    composable<Gallery> {
                        Gallery(galleryStateHolderMap, currentSelectedAlbum, menuItemsEntity, drawerState)
                    }
                    composable<Settings> {
                        SettingsPage()
                    }
                }
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