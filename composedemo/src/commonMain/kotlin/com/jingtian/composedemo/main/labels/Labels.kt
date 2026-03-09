package com.jingtian.composedemo.main.labels

import androidx.compose.runtime.Composable

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
