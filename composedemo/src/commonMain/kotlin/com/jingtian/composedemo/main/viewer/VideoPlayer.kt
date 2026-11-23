package com.jingtian.composedemo.main.viewer

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jingtian.composedemo.base.resources.DrawableIcon
import com.jingtian.composedemo.base.resources.getPainter
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.widget.AppScrim
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.ui.theme.DarkWrapper
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay

class UIVideoPlayerState(val initialPlaying: Boolean, internal val playerState: VideoPlayerState) {
    init {
        playerState.loop = true
    }

    var showMask by mutableStateOf(false)
    var isLoading by mutableStateOf(true)
    var maskVisible by mutableStateOf(false)
    var showMaskAnim = Animatable(initialValue = 0f)
    var isSpeedingUp by mutableStateOf(false)
    var isPressed by mutableStateOf(false)
    var thumbnailPressed by mutableStateOf(false)

    fun toggleMask() {
        showMask = !showMask
    }

    fun togglePlay() {
        if (playerState.isPlaying) {
            playerState.pause()
        } else {
            playerState.play()
        }
    }
}

@Composable
fun rememberUIVideoPlayerState(initialPlaying: Boolean = true): UIVideoPlayerState {
    val videoPlayerState = rememberVideoPlayerState()
    return remember { UIVideoPlayerState(initialPlaying, videoPlayerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    uiVideoPlayerState: UIVideoPlayerState = rememberUIVideoPlayerState(),
    modifier: Modifier = Modifier,
    uri: AlbumItemRelation,
    onEditRequest: () -> Unit = {},
) {
    var fileUri: MultiplatformFile? by remember { mutableStateOf(null) }
    LaunchedEffect(uri) {
        fileUri = uri.fileInfo.getFileUri()
    }
    val finalFileUri = fileUri?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                uiVideoPlayerState.playerState.pause()
            }

            override fun onResume(owner: LifecycleOwner) {
                uiVideoPlayerState.playerState.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    VideoPlayer(uiVideoPlayerState, modifier, finalFileUri, onEditRequest) {
        DarkWrapper {
            FileExtraInfoView(albumItemRelation = uri)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayer(
    uiVideoPlayerState: UIVideoPlayerState = rememberUIVideoPlayerState(),
    modifier: Modifier = Modifier,
    uri: MultiplatformFile,
    onEditRequest: () -> Unit = {},
    maskContent: @Composable () -> Unit = {},
) {
    uiVideoPlayerState.apply {
        val sliderState = rememberSliderState(
            value = playerState.sliderPos,
            valueRange = 0f..1000f,
        )
        val sliderInteractionSource = remember { MutableInteractionSource() }
        LaunchedEffect(Unit) {
            sliderInteractionSource.interactions.collect { action ->
                when (action) {
                    is DragInteraction.Start -> {
                        thumbnailPressed = true
                        playerState.pause()
                    }

                    else -> {
                        thumbnailPressed = false
                        playerState.play()
                    }
                }
            }
        }
        LaunchedEffect(thumbnailPressed, playerState.sliderPos) {
            if (!thumbnailPressed) {
                sliderState.value = playerState.sliderPos
            }
        }
        LaunchedEffect(thumbnailPressed, sliderState.value) {
            if (thumbnailPressed) {
                playerState.seekTo(sliderState.value)
                playerState.pause()
            }
        }
        LaunchedEffect(uri) {
            withContext(Dispatchers.Main) {
                isLoading = true
            }
            val platformFile = withContext(Dispatchers.IO) {
                uri.platformFile()
            }
//            logD("jingtian") {
//                "VideoPlayer: platformFile=$platformFile"
//            }
            if (platformFile != null) {
                withContext(Dispatchers.Main) {
                    playerState.openFile(
                        platformFile, initializeplayerState = if (initialPlaying) {
                            InitialPlayerState.PLAY
                        } else {
                            InitialPlayerState.PAUSE
                        }
                    )
                    withFrameMillis { }
                    isLoading = false
                }
            }
        }
        LaunchedEffect(showMask, thumbnailPressed) {
            if (showMask) {
                maskVisible = true
                showMaskAnim.animateTo(1f)
                if (!thumbnailPressed) {
                    delay(2000)
                    showMask = false
                }
            } else {
                maskVisible = true
                showMaskAnim.animateTo(0f)
                maskVisible = false
            }
        }
        LaunchedEffect(isPressed) {
            if (isPressed) {
                delay(200)
                isSpeedingUp = true
            } else {
                if (!isSpeedingUp) {
                    toggleMask()
                }
                isSpeedingUp = false
            }
        }
        LaunchedEffect(isSpeedingUp) {
            if (isSpeedingUp) {
                playerState.playbackSpeed = 2f
            } else {
                playerState.playbackSpeed = 1f
            }
        }
        Box(modifier = modifier.background(color = Color.Black)) {
            VideoPlayerSurface(
                playerState = playerState,
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        isPressed = true
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            isPressed = false
                        }
                    }
                })
            }

            if (maskVisible) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = showMaskAnim.value }
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            onEditRequest()
                        },
                            onTap = {
                                toggleMask()
                            })
                    }) {
                    AppScrim(alpha = 0.8f)
                    Icon(
                        painter = if (playerState.isPlaying) getPainter(DrawableIcon.DrawablePause) else getPainter(DrawableIcon.DrawablePlaying),
                        modifier = Modifier.fillMaxSize(0.35f).align(
                            Alignment.Center
                        ).clickable(interactionSource = null, indication = null) {
                            togglePlay()
                        },
                        contentDescription = "播放按钮",
                        tint = Color.White.copy(alpha = 0.7f)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                            .windowInsetsPadding(
                                WindowInsets.systemGestures
                            ).padding(horizontal = 16.dp)
                    ) {
                        maskContent()
                        Text(
                            text = "${playerState.positionText} / ${playerState.durationText}",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color.White
                        )
                        Slider(
                            state = sliderState,
                            interactionSource = sliderInteractionSource,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}