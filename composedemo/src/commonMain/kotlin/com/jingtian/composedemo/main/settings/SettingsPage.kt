package com.jingtian.composedemo.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.ui.theme.appBackground
import com.jingtian.composedemo.ui.theme.appBackgroundSlide
import com.jingtian.composedemo.ui.theme.appBackgroundV2
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.BackgroundColor
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.color
import com.jingtian.composedemo.utils.colorOf
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsPage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().appBackgroundV2()
            .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp)
    ) {
        item {
            AppBackgroundSlideBar()
        }
    }
}

private enum class AppBackgroundSlideBarSelectedType {
    Start, End, Middle;
}

@Composable
fun AppBackgroundSlideBar() {
    val viewModel: AppThemeViewModel = viewModel(factory = AppThemeViewModel.viewModelFactory)
    val currentTheme by remember { viewModel.currentAppTheme }
    val isSystemDark = isSystemInDarkTheme()
    val isDark = AppTheme.isDark(currentTheme, isSystemDark)
    val backgroundConfigState by remember(isDark) {
        if (isDark) {
            UserStorage.darkBackgroundConfigState
        } else {
            UserStorage.liteBackgroundConfigState
        }
    }
    var selectedType by remember { mutableStateOf<AppBackgroundSlideBarSelectedType?>(null) }
    val slideBarHeight: Dp = 30.dp
    val dotRadius = 28f
    var slideBarWidth by remember { mutableFloatStateOf(0F) }
    val density = LocalDensity.current
    fun dotPosition(startX: Float): Float {
        return ((slideBarWidth - slideBarHeight.value) * startX)
    }

    fun dotValue(offsetX: Float): Float {
        return ((offsetX) / (slideBarWidth - slideBarHeight.value)).coerceIn(0f, 1f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val startDotPosition by remember {
                derivedStateOf {
                    dotPosition(backgroundConfigState.startColor.startX).dp
                }
            }
            val endDotPosition by remember {
                derivedStateOf {
                    dotPosition(backgroundConfigState.endColor.startX).dp
                }
            }
            val middleDotPosition by remember(backgroundConfigState.middleColor) {
                derivedStateOf {
                    backgroundConfigState.middleColor?.let { middleColor->
                        dotPosition(middleColor.startX).dp
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.Center).pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    if (backgroundConfigState.middleColor == null) {
                        val value = dotValue(with(density) {
                            offset.x.toDp().value
                        })
                        logD("AppBackgroundSlideBar") {
                            "onTap: offset.x=${offset.x}, value=$value"
                        }
                        var backgroundConfig by if (isDark) {
                            UserStorage.darkBackgroundConfig
                        } else {
                            UserStorage.liteBackgroundConfig
                        }
                        backgroundConfig = backgroundConfig.let { backgroundConfig ->
                            backgroundConfig.copy(
                                middleColor = BackgroundColor(
                                    rawColor = backgroundConfigState.colorOf(value),
                                    startX = value
                                )
                            )
                        }
                        selectedType = AppBackgroundSlideBarSelectedType.Middle
                    }
                })
            }.onSizeChanged {
                slideBarWidth = with(density) {
                    it.width.toDp().value
                }
            }.height(slideBarHeight).clip(RoundedCornerShape(100)).appBackgroundSlide())
            Box(
                modifier = Modifier.size(dotRadius.dp).align(Alignment.CenterStart)
                    .offset(x = startDotPosition)
                    .clip(CircleShape).background(Color.White).pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            selectedType = AppBackgroundSlideBarSelectedType.Start
                        })
                    }.pointerInput(Unit) {
                        detectDragGestures(onDragStart = {
                            selectedType = AppBackgroundSlideBarSelectedType.Start
                        }) { change, dragAmount ->
                            val dotValue = dotValue(with(density) {
                                dragAmount.x.toDp().value
                            } + startDotPosition.value)
                            val middleDotPosition = middleDotPosition
                            if ((middleDotPosition != null && dotValue >= dotValue(middleDotPosition.value - dotRadius)) || dotValue >= dotValue(endDotPosition.value - dotRadius) || !(dotValue >= 0f && dotValue <= 1f)) {
                                return@detectDragGestures
                            }
                            var backgroundConfig by if (isDark) {
                                UserStorage.darkBackgroundConfig
                            } else {
                                UserStorage.liteBackgroundConfig
                            }
                            backgroundConfig = backgroundConfig.let { backgroundConfig ->
                                backgroundConfig.copy(
                                    startColor = backgroundConfig.startColor.copy(
                                        startX = dotValue
                                    )
                                )
                            }

                            logD("AppBackgroundSlideBar") {
                                "DragStart: dragAmount.x=${dragAmount.x}, dotValue=$dotValue"
                            }
                        }
                    }
            )

            Box(
                modifier = Modifier.size(dotRadius.dp).align(Alignment.CenterStart)
                    .offset(x = endDotPosition)
                    .clip(CircleShape).background(Color.White).pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            selectedType = AppBackgroundSlideBarSelectedType.End
                        })
                    }.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                selectedType = AppBackgroundSlideBarSelectedType.End
                            }
                        ) { change, dragAmount ->
                            val dotValue = dotValue(with(density) {
                                dragAmount.x.toDp().value
                            } + endDotPosition.value)
                            val middleDotPosition = middleDotPosition
                            if ((middleDotPosition != null && dotValue <= dotValue(middleDotPosition.value + dotRadius)) || dotValue <= dotValue(startDotPosition.value + dotRadius) || !(dotValue >= 0f && dotValue <= 1f)) {
                                return@detectDragGestures
                            }
                            var backgroundConfig by if (isDark) {
                                UserStorage.darkBackgroundConfig
                            } else {
                                UserStorage.liteBackgroundConfig
                            }
                            backgroundConfig = backgroundConfig.let { backgroundConfig ->
                                backgroundConfig.copy(
                                    endColor = backgroundConfig.endColor.copy(
                                        startX = dotValue
                                    )
                                )
                            }
                            logD("AppBackgroundSlideBar") {
                                "DragEnd: dragAmount.x=${dragAmount.x}, dotValue=$dotValue"
                            }
                        }
                    }
            )
            backgroundConfigState.middleColor?.let { middleColor ->
                Box(
                    modifier = Modifier.size(dotRadius.dp).align(Alignment.CenterStart)
                        .offset(x = middleDotPosition ?: 0.dp)
                        .clip(CircleShape).background(Color.White).pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                selectedType = AppBackgroundSlideBarSelectedType.Middle
                            })
                        }.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    selectedType = AppBackgroundSlideBarSelectedType.Middle
                                }
                            ) { change, dragAmount ->
                                val dotValue = dotValue(with(density) {
                                    dragAmount.x.toDp().value
                                } + (middleDotPosition?.value ?: 0f))
                                if (dotValue <= dotValue(startDotPosition.value + dotRadius) || dotValue >= dotValue(
                                        endDotPosition.value - dotRadius
                                    ) || !(dotValue >= 0f && dotValue <= 1f)
                                ) {
                                    return@detectDragGestures
                                }
                                var backgroundConfig by if (isDark) {
                                    UserStorage.darkBackgroundConfig
                                } else {
                                    UserStorage.liteBackgroundConfig
                                }
                                backgroundConfig = backgroundConfig.let { backgroundConfig ->
                                    backgroundConfig.copy(
                                        middleColor = middleColor.copy(
                                            startX = dotValue
                                        )
                                    )
                                }
                            }
                        }
                )
            }
        }
        val color by remember(selectedType) {
            derivedStateOf {
                when (selectedType) {
                    AppBackgroundSlideBarSelectedType.Middle -> {
                        backgroundConfigState.middleColor
                    }

                    AppBackgroundSlideBarSelectedType.Start -> {
                        backgroundConfigState.startColor
                    }

                    AppBackgroundSlideBarSelectedType.End -> {
                        backgroundConfigState.endColor
                    }

                    null -> {
                        null
                    }
                }?.color
            }
        }

        fun updateColor(color: Color) {
            var backgroundConfig by if (isDark) {
                UserStorage.darkBackgroundConfig
            } else {
                UserStorage.liteBackgroundConfig
            }
            when (selectedType) {
                AppBackgroundSlideBarSelectedType.Middle -> {
                    backgroundConfig = backgroundConfig.let {
                        it.copy(
                            middleColor = it.middleColor?.withColor(color)
                        )
                    }
                }

                AppBackgroundSlideBarSelectedType.Start -> {
                    backgroundConfig = backgroundConfig.let {
                        it.copy(
                            startColor = it.startColor.withColor(color)
                        )
                    }
                }

                AppBackgroundSlideBarSelectedType.End -> {
                    backgroundConfig = backgroundConfig.let {
                        it.copy(
                            endColor = it.endColor.withColor(color)
                        )
                    }
                }
                else -> {}
            }
        }
        color?.let { color->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                OutlinedTextField(value = (color.alpha * 255).roundToInt().toString(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    label = { Text("alpha") },
                    onValueChange = { value ->
                        val colorValue =
                            value.toIntOrNull()?.let { it / 255f } ?: return@OutlinedTextField
                        updateColor(color.copy(alpha = colorValue))
                    })
                OutlinedTextField(value = (color.red * 255).roundToInt().toString(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    label = { Text("red") },
                    onValueChange = { value ->
                        val colorValue =
                            value.toIntOrNull()?.let { it / 255f } ?: 0f
                        updateColor(color.copy(red = colorValue))
                    })
                OutlinedTextField(value = (color.green * 255).roundToInt().toString(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    label = { Text("green") },
                    onValueChange = { value ->
                        val colorValue =
                            value.toIntOrNull()?.let { it / 255f } ?: return@OutlinedTextField
                        updateColor(color.copy(green = colorValue))
                    })
                OutlinedTextField(value = (color.blue * 255).roundToInt().toString(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    label = { Text("blue") },
                    onValueChange = { value ->
                        val colorValue =
                            value.toIntOrNull()?.let { it / 255f } ?: return@OutlinedTextField
                        updateColor(color.copy(blue = colorValue))
                    })
            }
        }
    }
}