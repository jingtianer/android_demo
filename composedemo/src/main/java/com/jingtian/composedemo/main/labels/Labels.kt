package com.jingtian.composedemo.main.labels

import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jingtian.composedemo.R
import com.jingtian.composedemo.base.AppThemeBasicTextField
import com.jingtian.composedemo.ui.widget.FollowTailLayout
import com.jingtian.composedemo.utils.splitByWhiteSpace

@Composable
fun CheckableLabelView(label: String, isChecked: Boolean, onCheckStateChange: (Boolean) -> Unit) {
    LabelViewImpl(
        label = label,
        editable = false,
        checkable = true,
        isChecked = isChecked,
        onRemove = {},
        onCheckStateChange = onCheckStateChange,
        onValueChange = { })
}

@Composable
fun LabelView(label: String) {
    LabelViewImpl(
        label = label,
        editable = false,
        checkable = false,
        isChecked = false,
        onRemove = { },
        onCheckStateChange = {},
        onValueChange = { })
}

@Composable
fun EditableLabelView(label: String, editable: Boolean = true, enableEdit: Boolean = editable, onRemove: ()->Unit, onValueChange: (String) -> Unit) {
    LabelViewImpl(
        label = label,
        editable = editable,
        enableEdit = enableEdit,
        checkable = false,
        isChecked = false,
        onRemove = onRemove,
        onCheckStateChange = {},
        onValueChange = onValueChange
    )
}

@Composable
fun EditLabelView(onAddLabel: (List<String>)->Unit) {
    var labelText by remember { mutableStateOf("") }
    AndroidView(
        factory = { context ->
            val followTailLayout = FollowTailLayout(context)
            followTailLayout.orientation = Gravity.LEFT // right不行
            val headView = ComposeView(context)
            headView.setContent {
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(
                        fontSize = 24.sp
                    )
                ) {
                    AppThemeBasicTextField(
                        labelText, onValueChange = { value -> labelText = value },
                        Modifier.wrapContentSize(),
                        hint = "添加标签",
                    )
                }
            }

            val tailView = ComposeView(context)
            tailView.setContent {
                Spacer(Modifier.width(2.dp))
                Image(
                    painter = painterResource(R.drawable.add_green),
                    contentDescription = "添加标签",
                    Modifier
                        .size(24.dp)
                        .clickable {
                            if (labelText.isNotBlank()) {
                                onAddLabel(
                                    labelText
                                        .trim()
                                        .splitByWhiteSpace()
                                )
                                labelText = ""
                            }
                        },
                )
            }
            tailView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            followTailLayout.addHead(
                headView, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            )
            followTailLayout.addTail(tailView)
            followTailLayout
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    )
}