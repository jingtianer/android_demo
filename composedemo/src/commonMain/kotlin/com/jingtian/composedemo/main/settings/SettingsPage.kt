package com.jingtian.composedemo.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.main.labels.CheckableLabelView
import com.jingtian.composedemo.main.settings.SettingsViewModel.Companion.settingsViewModel
import com.jingtian.composedemo.ui.theme.LocalAppColorScheme
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.appBackgroundSlide
import com.jingtian.composedemo.ui.theme.appBackgroundV2
import com.jingtian.composedemo.utils.BackgroundColor
import com.jingtian.composedemo.utils.BackgroundConfig
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.color
import com.jingtian.composedemo.utils.colorOf
import kotlin.math.roundToInt

@Composable
fun SettingsPage() {
    val viewModel = settingsViewModel()

    val snackBarMessage by viewModel.snackBarMessage
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
    Scaffold(
        modifier = Modifier.fillMaxSize().appBackgroundV2()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentColor = Color.Transparent,
        containerColor = Color.Transparent,
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 18.dp,
                alignment = Alignment.Top
            )
        ) {
            item {
                AppClearButtons()
            }
            item {
                AppBackgroundSlideBar()
            }
            item {
                MainColorSlideBar()
            }
            item {
                ResetAllColor()
            }
        }
    }

}

@Composable
fun AppClearButtons() {
    val viewModel = settingsViewModel()
    Row(Modifier.fillMaxWidth()) {
        Button(onClick = {
            viewModel.clearCache()
        }, modifier = Modifier.weight(1f).height(38.dp)) {
            Text("清空缓存", modifier = Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.width(16.dp))
        Button(onClick = {
            viewModel.clearStorage()
        }, modifier = Modifier.weight(1f).height(38.dp)) {
            Text("清空存储", modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

@Composable
fun ResetAllColor() {
    val isDark = LocalAppPalette.current.isDark
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = {
            UserStorage.resetColors(isDark)
        }, modifier = Modifier.width(160.dp).height(36.dp).align(Alignment.CenterEnd)) {
            Text("恢复所有颜色", modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}

private enum class AppBackgroundSlideBarSelectedType {
    Start, End, Middle;
}

@Composable
fun MainColorSlideBar() {
    val isDark = LocalAppPalette.current.isDark
    val mainColorRaw by if (isDark) {
        UserStorage.darkMainColorState
    } else {
        UserStorage.liteMainColorState
    }

    fun updateColor(color: Color) {
        var mainColorStore by if (isDark) {
            UserStorage.darkMainColor
        } else {
            UserStorage.liteMainColor
        }
        mainColorStore = color.toArgb().toLong()
    }

    val mainColor = remember(mainColorRaw) {
        Color(mainColorRaw)
    }

    ColorSlideBar(mainColor, true, ::updateColor)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorSlideBar(mainColor: Color, showDemo: Boolean = true, updateColor: (Color)->Unit) {
    val redState = rememberSliderState(value = mainColor.red, valueRange = 0f..1f)
    val greenState = rememberSliderState(value = mainColor.green, valueRange = 0f..1f)
    val blueState = rememberSliderState(value = mainColor.blue, valueRange = 0f..1f)
    val alphaState = rememberSliderState(value = mainColor.alpha, valueRange = 0f..1f)
    LaunchedEffect(mainColor) {
        redState.value = mainColor.red
        greenState.value = mainColor.green
        blueState.value = mainColor.blue
        alphaState.value = mainColor.alpha
    }
    LaunchedEffect(redState.value, greenState.value, blueState.value, alphaState.value) {
        updateColor(
            Color(red = redState.value, green = greenState.value, blue = blueState.value, alpha = alphaState.value)
        )
    }
    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        if (showDemo) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {}, modifier = Modifier.weight(1f).height(38.dp)) {
                    Text("示例按钮", modifier = Modifier.align(Alignment.CenterVertically))
                }
                CheckableLabelView("示例标签", true) {}
            }
            Spacer(Modifier.height(12.dp))
        }
        Text("alpha")
        Slider(
            state = alphaState,
            modifier = Modifier.fillMaxWidth()
        )
        Text("red")
        Slider(
            state = redState,
            modifier = Modifier.fillMaxWidth()
        )
        Text("green")
        Slider(
            state = greenState,
            modifier = Modifier.fillMaxWidth()
        )
        Text("blue")
        Slider(
            state = blueState,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            ColorEditText(mainColor, ColorType.A) {
                updateColor(mainColor.copy(alpha = it))
            }
            ColorEditText(mainColor, ColorType.R) {
                updateColor(mainColor.copy(red = it))
            }
            ColorEditText(mainColor, ColorType.G) {
                updateColor(mainColor.copy(green = it))
            }
            ColorEditText(mainColor, ColorType.B) {
                updateColor(mainColor.copy(blue = it))
            }
        }
    }
}

@Composable
fun AppBackgroundSlideBar() {
    val isDark = LocalAppPalette.current.isDark
    val backgroundConfigState by if (isDark) {
        UserStorage.darkBackgroundConfigState
    } else {
        UserStorage.liteBackgroundConfigState
    }
    var selectedType by remember { mutableStateOf<AppBackgroundSlideBarSelectedType?>(null) }
    val slideBarHeight: Dp = 36.dp
    val dotSize = 34f
    var slideBarWidth by remember { mutableFloatStateOf(0F) }
    val density = LocalDensity.current
    val dotSizeGap = (slideBarHeight.value - dotSize) / 2f
    fun dotPosition(startX: Float): Float {
        return ((slideBarWidth - slideBarHeight.value) * startX + dotSizeGap)
    }

    fun dotValue(offsetX: Float): Float {
        return ((offsetX - dotSizeGap) / (slideBarWidth - slideBarHeight.value)).coerceIn(0f, 1f)
    }

    fun updateBackgroundConfig(onUpdate: BackgroundConfig.() -> BackgroundConfig) {
        var backgroundConfig by if (isDark) {
            UserStorage.darkBackgroundConfig
        } else {
            UserStorage.liteBackgroundConfig
        }
//        logD("AppBackgroundSlideBar") {
//            "updateBackgroundConfig: isDark=$isDark"
//        }
        backgroundConfig = backgroundConfig.onUpdate()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val startDotPosition by remember(backgroundConfigState, backgroundConfigState.startColor.startX, slideBarWidth) {
                derivedStateOf {
                    dotPosition(backgroundConfigState.startColor.startX).dp
                }
            }
            val endDotPosition by remember(backgroundConfigState, backgroundConfigState.endColor.startX, slideBarWidth) {
                derivedStateOf {
                    dotPosition(backgroundConfigState.endColor.startX).dp
                }
            }
            val middleDotPosition by remember(backgroundConfigState, backgroundConfigState.middleColor?.startX, slideBarWidth) {
                derivedStateOf {
                    backgroundConfigState.middleColor?.let { middleColor ->
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
                        updateBackgroundConfig {
                            copy(
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
            BackgroundSlideDot(
                dotSize,
                AppBackgroundSlideBarSelectedType.Start,
                selectedType,
                startDotPosition,
                onUpdate = {
                    selectedType = it
                }) {
                val dotValue = dotValue(with(density) {
                    it.toDp().value
                } + startDotPosition.value)
                val middleDotPosition = middleDotPosition
                if ((middleDotPosition != null && dotValue >= dotValue(middleDotPosition.value - dotSize)) || dotValue >= dotValue(
                        endDotPosition.value - dotSize
                    ) || !(dotValue >= 0f && dotValue <= 1f)
                ) {
                    return@BackgroundSlideDot
                }
                updateBackgroundConfig {
                    copy(
                        startColor = startColor.copy(
                            startX = dotValue
                        )
                    )
                }
            }
            BackgroundSlideDot(
                dotSize,
                AppBackgroundSlideBarSelectedType.End,
                selectedType,
                endDotPosition,
                onUpdate = {
                    selectedType = it
                }) {
                val dotValue = dotValue(with(density) {
                    it.toDp().value
                } + endDotPosition.value)
                val middleDotPosition = middleDotPosition
                if ((middleDotPosition != null && dotValue <= dotValue(middleDotPosition.value + dotSize)) || dotValue <= dotValue(
                        startDotPosition.value + dotSize
                    ) || !(dotValue >= 0f && dotValue <= 1f)
                ) {
                    return@BackgroundSlideDot
                }
                updateBackgroundConfig {
                    copy(
                        endColor = endColor.copy(
                            startX = dotValue
                        )
                    )
                }
            }

            if (backgroundConfigState.middleColor != null) {
                BackgroundSlideDot(
                    dotSize,
                    AppBackgroundSlideBarSelectedType.Middle,
                    selectedType,
                    middleDotPosition ?: 0.dp,
                    onUpdate = {
                        selectedType = it
                    }) {
                    val dotValue = dotValue(with(density) {
                        it.toDp().value
                    } + (middleDotPosition?.value ?: 0f))
                    if (dotValue <= dotValue(startDotPosition.value + dotSize) || dotValue >= dotValue(
                            endDotPosition.value - dotSize
                        ) || !(dotValue >= 0f && dotValue <= 1f)
                    ) {
                        return@BackgroundSlideDot
                    }
                    updateBackgroundConfig {
                        copy(
                            middleColor = middleColor?.copy(
                                startX = dotValue
                            )
                        )
                    }
                }
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
        color?.let { color ->
            ColorSlideBar(color, false, ::updateColor)
        }
    }
}

enum class ColorType(val label: String) {
    A("alpha"), R("red"), G("green"), B("blue");
}

@Composable
fun RowScope.ColorEditText(color: Color, colorType: ColorType, onUpdate: (value: Float) -> Unit) {
    val colorValue = (when (colorType) {
        ColorType.A -> {
            color.alpha
        }

        ColorType.R -> {
            color.red
        }

        ColorType.G -> {
            color.green
        }

        ColorType.B -> {
            color.blue
        }
    } * 255).roundToInt()
    val label = colorType.label
    OutlinedTextField(value = if (colorValue >= 0) colorValue.toString() else "",
        modifier = Modifier.padding(horizontal = 3.dp).weight(1f),
        maxLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        label = { Text(label) },
        onValueChange = { value ->
            val colorValue = value.toIntOrNull()?.let { it / 255f } ?: 0f
            onUpdate(colorValue)
        })
}

@Composable
private fun BoxScope.BackgroundSlideDot(
    dotSize: Float,
    type: AppBackgroundSlideBarSelectedType,
    selectedType: AppBackgroundSlideBarSelectedType?,
    offset: Dp,
    onUpdate: (selectedType: AppBackgroundSlideBarSelectedType) -> Unit,
    onDrag: (Float) -> Unit,
) {
    // !!! 保证获取最新回调
    val onUpdate by rememberUpdatedState(onUpdate)
    val onDrag by rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier.size(dotSize.dp).align(Alignment.CenterStart)
            .offset(x = offset)
            .clip(CircleShape).background(LocalAppColorScheme.current.primary).pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onUpdate(type)
                })
            }.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onUpdate(type)
                    }
                ) { change, dragAmount ->
                    onDrag(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (type == selectedType) {
            Box(
                modifier = Modifier.size(6.dp).clip(CircleShape)
                    .background(LocalTextStyle.current.color)
            )
        }
    }
}