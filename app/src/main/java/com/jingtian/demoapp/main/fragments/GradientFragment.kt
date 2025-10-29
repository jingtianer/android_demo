package com.jingtian.demoapp.main.fragments

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jingtian.demoapp.databinding.FragmentGradientBinding

class GradientFragment : BaseFragment() {

    lateinit var binding: FragmentGradientBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGradientBinding.inflate(inflater, container, false)
        return binding.root
    }

    private val startColor = 0xffff0000.toInt()
    private val endColor = 0x00ff0000

    private val Int.alpha:Int
        get() = this ushr 24

    private fun Int.setAlpha(alpha: Int): Int {
        return (alpha shl 24) or (this and 0x00FFFFFF)
    }

    private fun Double.mixAlpha(start: Int, end: Int): Int {
        return start.setAlpha((start.alpha * this + end.alpha * (1 - this)).toInt())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val alpha = 2/3.0
        binding.view1.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                startColor,
                alpha.mixAlpha(startColor, endColor),
            )
        )

        binding.view2.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                alpha.mixAlpha(startColor, endColor),
                endColor,
            )
        )

        binding.view3.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                startColor,
                endColor,
            )
        )
    }

}