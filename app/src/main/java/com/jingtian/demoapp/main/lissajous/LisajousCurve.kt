package com.jingtian.demoapp.main.lissajous

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LisajousCurve(
    val paramName: String = "t",
    val tMin: Float = 0f,
    val tMax: Float = (2 * Math.PI).toFloat(),
    val a: Float = 2f,
    val b: Float = 3f,
    val x: Float = 0f,
    val stepCount: Int = 300,
) {
    val xExprStr: String = "sinr(a*t)"
    val yExprStr: String = "cosr(b*t+x)"
}

object DefaultConfigs {
    class DefConfig(val lisajousCurve: LisajousCurve, val name: CharSequence, val desc: CharSequence = "", val speed: Int = 39)
    val defaultConfigs = listOf(
        DefConfig(LisajousCurve(), "2:3"),
        DefConfig(LisajousCurve(
            a = 1f,
            b = 1f,
        ), "1:1"),
        DefConfig(LisajousCurve(
            a = 4f,
            b = 7f,
        ), "4:7"),
        DefConfig(LisajousCurve(
            a = 9f,
            b = 8f,
            stepCount = 600
        ), "9:8", speed = 20),
    )
}