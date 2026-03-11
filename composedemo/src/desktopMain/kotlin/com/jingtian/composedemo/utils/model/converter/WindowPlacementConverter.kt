package com.jingtian.composedemo.utils.model.converter

import androidx.compose.ui.window.WindowPlacement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class WindowPlacementConverter: TypeAdapter<WindowPlacement>() {
    fun WindowPlacement.toInt(): Int {
        return when(this) {
            WindowPlacement.Floating -> 0
            WindowPlacement.Maximized -> 1
            WindowPlacement.Fullscreen -> 2
        }
    }
    fun Int.fromInt(): WindowPlacement {
        return when(this) {
            0 -> WindowPlacement.Floating
            1 -> WindowPlacement.Maximized
            2 -> WindowPlacement.Fullscreen
            else -> WindowPlacement.Floating
        }
    }
    override fun write(out: JsonWriter?, value: WindowPlacement?) {
        out?.value((value ?: WindowPlacement.Floating).toInt())
    }

    override fun read(`in`: JsonReader?): WindowPlacement {
        return `in`?.nextInt()?.fromInt() ?: WindowPlacement.Floating
    }

}