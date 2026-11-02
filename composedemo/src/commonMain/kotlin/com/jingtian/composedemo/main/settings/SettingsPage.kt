package com.jingtian.composedemo.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.main.labels.CheckableLabelView
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.ui.theme.LocalAppColorScheme
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.appBackgroundSlide
import com.jingtian.composedemo.ui.theme.appBackgroundV2
import com.jingtian.composedemo.ui.theme.toHsl
import com.jingtian.composedemo.utils.AppTheme
import com.jingtian.composedemo.utils.BackgroundColor
import com.jingtian.composedemo.utils.BackgroundConfig
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.color
import com.jingtian.composedemo.utils.colorOf
import com.jingtian.composedemo.viewmodels.AppThemeViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsPage() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().appBackgroundV2()
            .windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.Top
        )
    ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainColorSlideBar() {
    val isDark = LocalAppPalette.current.isDark
    val mainColorRaw by if (isDark) {
        UserStorage.darkMainColorState
    } else {
        UserStorage.liteMainColorState
    }
    val mainColor = remember(mainColorRaw) {
        Color(mainColorRaw)
    }

    fun updateColor(color: Color) {
        var mainColorStore by if (isDark) {
            UserStorage.darkMainColor
        } else {
            UserStorage.liteMainColor
        }
        mainColorStore = color.toArgb().toLong()
    }

    val ahslColor = remember(mainColor) { mutableListOf(*mainColor.toHsl().toTypedArray()) }
    val hState = rememberSliderState(value = ahslColor[1] / 360f, valueRange = 0f..1f)
    val sState = rememberSliderState(value = ahslColor[2], valueRange = 0f..1f)
    val lState = rememberSliderState(value = ahslColor[3], valueRange = 0f..1f)
    val alphaState = rememberSliderState(value = ahslColor[0], valueRange = 0f..1f)
    LaunchedEffect(ahslColor) {
        hState.value = ahslColor[1] / 360f
        sState.value = ahslColor[2]
        lState.value = ahslColor[3]
        alphaState.value = ahslColor[0]
    }
    LaunchedEffect(hState.value, sState.value, lState.value, alphaState.value) {
        updateColor(
            Color.hsl(hState.value * 360, sState.value, lState.value).copy(alpha = alphaState.value)
        )
    }
    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {}, modifier = Modifier.weight(1f).height(38.dp)) {
                Text("示例按钮", modifier = Modifier.align(Alignment.CenterVertically))
            }
            CheckableLabelView("示例标签", true) {}
        }
        Spacer(Modifier.height(12.dp))
        Text("alpha")
        Slider(
            state = alphaState,
            modifier = Modifier.fillMaxWidth()
        )
        Text("h")
        Slider(
            state = hState,
            modifier = Modifier.fillMaxWidth()
        )
        Text("s")
        Slider(
            state = sState,
            modifier = Modifier.fillMaxWidth()
        )
        Text("l")
        Slider(
            state = lState,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            OutlinedTextField(value = (mainColor.alpha * 255).roundToInt().toString(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                label = { Text("alpha") },
                onValueChange = { value ->
                    val colorValue = value.toIntOrNull()?.let { it / 255f } ?: 0f
                    updateColor(mainColor.copy(alpha = colorValue))
                })
            OutlinedTextField(value = (mainColor.red * 255).roundToInt().toString(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                label = { Text("red") },
                onValueChange = { value ->
                    val colorValue = value.toIntOrNull()?.let { it / 255f } ?: 0f
                    updateColor(mainColor.copy(red = colorValue))
                })
            OutlinedTextField(value = (mainColor.green * 255).roundToInt().toString(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                label = { Text("green") },
                onValueChange = { value ->
                    val colorValue = value.toIntOrNull()?.let { it / 255f } ?: 0f
                    updateColor(mainColor.copy(green = colorValue))
                })
            OutlinedTextField(value = (mainColor.blue * 255).roundToInt().toString(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                label = { Text("blue") },
                onValueChange = { value ->
                    val colorValue = value.toIntOrNull()?.let { it / 255f } ?: 0f
                    updateColor(mainColor.copy(blue = colorValue))
                })
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
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                ColorEditText(color, ColorType.A) {
                    updateColor(color.copy(alpha = it))
                }
                ColorEditText(color, ColorType.R) {
                    updateColor(color.copy(red = it))
                }
                ColorEditText(color, ColorType.G) {
                    updateColor(color.copy(green = it))
                }
                ColorEditText(color, ColorType.B) {
                    updateColor(color.copy(blue = it))
                }
            }
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