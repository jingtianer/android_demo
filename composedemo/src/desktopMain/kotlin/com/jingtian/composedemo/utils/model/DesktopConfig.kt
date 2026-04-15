package com.jingtian.composedemo.utils.model

import androidx.compose.ui.window.WindowPlacement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class DesktopConfig {
    var screenWidthDp: Float = 380f
    var screenHeightDp: Float = 820f

    @Serializable(with = WindowPlacementSerializer::class)
    var windowPlacement: WindowPlacement = WindowPlacement.Floating
}

object WindowPlacementSerializer : KSerializer<WindowPlacement> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("WindowPlacement", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: WindowPlacement) {
        val intValue = when (value) {
            WindowPlacement.Floating -> 0
            WindowPlacement.Maximized -> 1
            WindowPlacement.Fullscreen -> 2
        }
        encoder.encodeInt(intValue)
    }

    override fun deserialize(decoder: Decoder): WindowPlacement {
        return when (val intValue = decoder.decodeInt()) {
            0 -> WindowPlacement.Floating
            1 -> WindowPlacement.Maximized
            2 -> WindowPlacement.Fullscreen
            else -> WindowPlacement.Floating
        }
    }
}
