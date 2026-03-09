package com.jingtian.composedemo.main.labels

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.base.AppThemeBasicTextField
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import demoapp.composedemo.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun LabelViewImpl(label: String, editable: Boolean = false, enableEdit: Boolean = false, checkable: Boolean = false, isChecked: Boolean = false, onRemove: ()->Unit = {}, onCheckStateChange: (Boolean) -> Unit = {}, onValueChange: (String)->Unit = {}) {
    fun Modifier.onClickListener(): Modifier {
        return if (checkable) {
            this.clickable {
                onCheckStateChange(!isChecked)
            }
        } else {
            this
        }
    }
    @Composable
    fun Modifier.viewBackground(): Modifier {
        return if (isChecked) {
            this.background(
                color = LocalAppPalette.current.labelChecked,
                shape = RoundedCornerShape(4.dp)
            )
        } else {
            this.background(
                color = LocalAppPalette.current.labelUnChecked,
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
    fun Modifier.viewHeight(): Modifier {
        return if (editable) {
            this.height(26.dp)
        } else {
            this.height(24.dp)
        }
    }
    val fontSize = if (editable) {
        16.sp
    } else {
        14.sp
    }
    Row(
        Modifier
            .padding(2.dp)
            .viewHeight()
            .viewBackground()
            .wrapContentWidth()
            .onClickListener(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (editable && enableEdit) {
            AppThemeBasicTextField(
                label,
                onValueChange = onValueChange,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.CenterVertically)
                    .wrapContentSize(),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = fontSize,
                    color = LocalAppPalette.current.labelTextColor
                ),
                hint = "新建标签"
            )
        } else {
            AppThemeText(
                label,
                Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.CenterVertically)
                    .wrapContentSize(),
                style = LocalTextStyle.current.copy(
                    fontSize = fontSize,
                    color = LocalAppPalette.current.labelTextColor
                )
            )
        }
        if (editable) {
            Spacer(Modifier.padding(2.dp))
            Image(
                painter = painterResource(
                    if (enableEdit) Res.drawable.add_green
                    else Res.drawable.close
                ),
                contentDescription = if (enableEdit) "添加标签" else "删除标签",
                Modifier
                    .padding(end = 4.dp)
                    .size(16.dp)
                    .align(Alignment.CenterVertically)
                    .clickable { onRemove() },
            )
        }
    }
}