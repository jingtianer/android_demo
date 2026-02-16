package com.jingtian.composedemo.model

class LabelInfo(
    var albumItemId: Long = INVALID_ID,
    var label: Label = Label.DEFAULT
)

enum class Label {
    DEFAULT
}