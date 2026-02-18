package com.jingtian.composedemo.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.jingtian.composedemo.R


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
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },

    // text
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    editAlignment: AppThemeClickEditableTextArrange = AppThemeClickEditableTextArrange.TOP_END,
    initEditable: Boolean = false,
    horizontalOffset: Dp = 0.dp,
    verticalOffset: Dp = 0.dp,

    ) {
    var editable by remember { mutableStateOf(initEditable) }
    ConstraintLayout(modifier) {
        val (iconRef, textRef, editTextRef) = createRefs()
        @Composable
        fun EditImage() {
            Image(
                painter = painterResource(R.drawable.edit),
                contentDescription = "编辑按钮",
                Modifier
                    .size(editSize)
                    .width(editSize)
                    .constrainAs(iconRef) {
                        val target = if (editable) editTextRef else textRef
                        when (editAlignment) {
                            AppThemeClickEditableTextArrange.TOP_START -> {
                                bottom.linkTo(target.top, -verticalOffset)
                                end.linkTo(target.start, -horizontalOffset)
                            }

                            AppThemeClickEditableTextArrange.BOTTOM_START -> {
                                top.linkTo(target.bottom, -verticalOffset)
                                end.linkTo(target.start, -horizontalOffset)
                            }

                            AppThemeClickEditableTextArrange.TOP_END -> {
                                bottom.linkTo(target.top, -verticalOffset)
                                start.linkTo(target.end, -horizontalOffset)
                            }

                            AppThemeClickEditableTextArrange.BOTTOM_END -> {
                                top.linkTo(target.bottom, -verticalOffset)
                                start.linkTo(target.end, -horizontalOffset)
                            }
                        }
                    }
                    .clickable { editable = !editable },
            )
        }
        if (editable) {
            AppThemeBasicTextField(
                value,
                { currentValue ->
                    onValueChange(currentValue, editable)
                },
                Modifier
                    .wrapContentWidth()
                    .constrainAs(editTextRef) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
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
            )
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
                minLines
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
    color: ColorProducer? = null
) = BasicText(
    text, modifier, style, onTextLayout, overflow, softWrap, maxLines, minLines, color
)

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
) = BasicTextField(
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
    decorationBox = @Composable { innerTextField ->
        // places leading icon, text field with label and placeholder, trailing icon
        if (!hint.isNullOrEmpty() && value.isEmpty()) {
            AppThemeText(hint, style = LocalTextStyle.current.copy(color = hintColor))
        }

        innerTextField()
    }
)
