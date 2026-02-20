package com.jingtian.composedemo.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import com.jingtian.composedemo.R
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalAppUIConstants
import com.jingtian.composedemo.ui.theme.LocalMiddleButtonConfig


enum class AppThemeClickEditableTextArrange {
    TOP_START,
    TOP_END,
    BOTTOM_START,
    BOTTOM_END,
}

@Composable
fun AppThemeClickEditableText(
    editSize: Dp,
    // editText
    value: String,
    onValueChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),

    // text
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    editAlignment: AppThemeClickEditableTextArrange = AppThemeClickEditableTextArrange.TOP_END,
    initEditable: Boolean = false,
    horizontalOffset: Dp = 0.dp,
    verticalOffset: Dp = 0.dp,
    hint: String? = null,
    hintColor: Color = Color.Gray,
) {
    var editable by remember { mutableStateOf(initEditable) }
    ConstraintLayout(modifier) {
        val (iconRef, textRef, editTextRef) = createRefs()
        @Composable
        fun EditImage() {
            Image(
                painter = painterResource(if (editable) R.drawable.close else R.drawable.edit),
                contentDescription = if (editable) "结束编辑按钮" else "编辑按钮",
                Modifier
                    .size(editSize)
                    .width(editSize)
                    .constrainAs(iconRef) {
                        val target = if (editable) editTextRef else textRef
                        when (editAlignment) {
                            AppThemeClickEditableTextArrange.TOP_START -> {
                                top.linkTo(target.top, verticalOffset)
                                end.linkTo(target.start, horizontalOffset)
                            }

                            AppThemeClickEditableTextArrange.BOTTOM_START -> {
                                bottom.linkTo(target.bottom, verticalOffset)
                                end.linkTo(target.start, horizontalOffset)
                            }

                            AppThemeClickEditableTextArrange.TOP_END -> {
                                top.linkTo(target.top, verticalOffset)
                                start.linkTo(target.end, horizontalOffset)
                            }

                            AppThemeClickEditableTextArrange.BOTTOM_END -> {
                                bottom.linkTo(target.bottom, verticalOffset)
                                start.linkTo(target.end, horizontalOffset)
                            }
                        }
                    }
                    .clickable {
                        editable = !editable
                    },
            )
        }
        if (editable) {
            val focusRequester = remember { FocusRequester() }
            AppThemeBasicTextField(
                value = TextFieldValue(
                    text = value,
                    selection = TextRange(value.length, value.length)
                ),
                { currentValue ->
                    onValueChange(currentValue.text, editable)
                },
                Modifier
                    .wrapContentWidth()
                    .constrainAs(editTextRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
                    .focusRequester(focusRequester)
                    .clickable {
                        onValueChange(value, editable)
                    },
                enabled,
                readOnly,
                textStyle,
                keyboardOptions,
                keyboardActions,
                singleLine,
                maxLines,
                minLines,
                visualTransformation,
                onTextLayout,
                interactionSource,
                cursorBrush,
                hint,
                hintColor,
            )
            LaunchedEffect(editable) {
                if (editable) {
                    focusRequester.requestFocus()
                }
            }
        } else {
            AppThemeText(
                value,
                Modifier
                    .wrapContentWidth()
                    .constrainAs(textRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
                    .clickable {
                        editable = false
                        onValueChange(value, editable)
                    },
                textStyle,
                onTextLayout,
                overflow,
                softWrap,
                maxLines,
                minLines,
                color = null,
                hint,
                hintColor,
            )
        }
        EditImage()
    }
}


@Composable
fun AppThemeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null,
    hint: String? = null,
    hintColor: Color = Color.Gray,
) {
    if (text.isNullOrBlank() && hint != null) {
        BasicText(
            hint, modifier, style.copy(hintColor), onTextLayout, overflow, softWrap, maxLines, minLines, color
        )
    } else {
        BasicText(
            text, modifier, style, onTextLayout, overflow, softWrap, maxLines, minLines, color
        )

    }}

@Composable
fun AppThemeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors()
) = TextField(
    value,
    onValueChange,
    modifier,
    enabled,
    readOnly,
    textStyle,
    label,
    placeholder,
    leadingIcon,
    trailingIcon,
    prefix,
    suffix,
    supportingText,
    isError,
    visualTransformation,
    keyboardOptions,
    keyboardActions,
    singleLine,
    maxLines,
    minLines,
    interactionSource,
    shape,
    colors
)


@Composable
fun AppThemeBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    hint: String? = null,
    hintColor: Color = Color.Gray,
) {
    Box(modifier.wrapContentSize()) {
        val modifier = if (value.isNullOrBlank() && !hint.isNullOrBlank()) {
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            AppThemeText(
                text = hint,
                modifier
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                style = LocalTextStyle.current.copy(color = hintColor),
                maxLines = 1
            )
            Modifier
                .width(3.dp)
                .focusRequester(focusRequester)
                .align(Alignment.TopStart)
        } else {
            modifier
                .width(IntrinsicSize.Min)
                .align(Alignment.TopStart)
        }
        BasicTextField(
            value,
            onValueChange,
            modifier,
            enabled,
            readOnly,
            textStyle,
            keyboardOptions,
            keyboardActions,
            singleLine,
            maxLines,
            minLines,
            visualTransformation,
            onTextLayout,
            interactionSource,
            cursorBrush,
        )
    }
}

@Composable
fun AppThemeBasicTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(Color.Black),
    hint: String? = null,
    hintColor: Color = Color.Gray,
) {
    Box(modifier.wrapContentSize()) {
        val modifier = if (value.text.isNullOrBlank() && !hint.isNullOrBlank()) {
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current
            AppThemeText(
                text = hint,
                modifier
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                style = LocalTextStyle.current.copy(color = hintColor),
                maxLines = 1
            )
            Modifier
                .width(3.dp)
                .focusRequester(focusRequester)
        } else {
            modifier.width(IntrinsicSize.Min)
        }
        BasicTextField(
            value,
            onValueChange,
            modifier,
            enabled,
            readOnly,
            textStyle,
            keyboardOptions,
            keyboardActions,
            singleLine,
            maxLines,
            minLines,
            visualTransformation,
            onTextLayout,
            interactionSource,
            cursorBrush,
        )
    }
}

@Composable
fun AppThemeHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = LocalAppPalette.current.dividerColor
) {
    HorizontalDivider(modifier, thickness, color)
}

@Composable
fun AppThemeConfirmDialog(
    title: String? = null,
    titleTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 18.sp,
        fontWeight = FontWeight(600)
    ),
    properties: DialogProperties = DialogProperties(),
    onNegative: () -> Unit = {},
    onPositive: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    AppThemeDialog(
        title = title,
        titleTextStyle = titleTextStyle,
        properties = properties,
        onNegative = onNegative,
        onMiddleClick = null,
        onPositive = onPositive,
        onDismissRequest = onDismissRequest
    ) { header, actionButton ->
        header()
        content()
        actionButton()
    }
}

@Composable
fun AppThemeDialog(
    modifier: Modifier = Modifier
        .fillMaxWidth(LocalAppUIConstants.current.dialogPercent)
        .background(LocalAppPalette.current.dialogBg)
        .padding(horizontal = 8.dp),
    title: String? = null,
    titleTextStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 18.sp,
        fontWeight = FontWeight(600)
    ),
    properties: DialogProperties = DialogProperties(),
    onNegative: () -> Unit = {},
    onMiddleClick: (() -> Unit)? = null,
    onPositive: () -> Unit = {},
    onDismissRequest: () -> Unit = onNegative,
    content: @Composable (
        header: @Composable () -> Unit,
        actionButton: @Composable () -> Unit
    ) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Column(modifier) {
            content(
                header = {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!title.isNullOrBlank()) {
                        AppThemeText(title, style = titleTextStyle)
                        Spacer(modifier = Modifier.height(2.dp))
                        AppThemeHorizontalDivider(
                            modifier = Modifier
                                .height(1.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                },
                actionButton = {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(Modifier.align(Alignment.End)) {
                        Button(onClick = onNegative) {
                            AppThemeText("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (onMiddleClick != null) {
                            Button(onClick = onMiddleClick, colors = LocalMiddleButtonConfig.current.colors) {
                                AppThemeText(LocalMiddleButtonConfig.current.text)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(onClick = onPositive) {
                            AppThemeText("确认")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            )
        }
    }
}