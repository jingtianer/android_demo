package com.jingtian.demoapp.main.fragments

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jingtian.demoapp.databinding.FragmentWidthAnimationBinding

class WidthAnimFragment: BaseFragment() {
    private lateinit var binding: FragmentWidthAnimationBinding
    private val button by lazy { binding.button }
    private val currentTextAnimation: Animator by lazy {
        AnimatorSet().apply  {
            play(button.startAnimation()).after(button.makeHeightEqWidth())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWidthAnimationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener {
            if (currentTextAnimation.isPaused) {
                currentTextAnimation.resume()
            } else {
                currentTextAnimation.pause()
            }
        }
    }

    private val runAnim = Runnable {
        if (currentTextAnimation.isStarted) {
            currentTextAnimation.resume()
        } else {
            currentTextAnimation.start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (button.width <= 0) {
            button.post(runAnim)
        } else {
            runAnim.run()
        }
    }

    override fun onPause() {
        super.onPause()
        currentTextAnimation.pause()
    }

    private fun TextView.makeHeightEqWidth():ObjectAnimator {
        val width = width
        val height = height
        val wrappedView = object {
            fun getHeight(): Int {
                return layoutParams.height
            }

            fun setHeight(height: Int) {
                layoutParams = layoutParams.apply {
                    this.height = height
                }
            }
        }
        return ObjectAnimator.ofInt(wrappedView, "height", height, width).apply {
            duration = 200
        }
    }

    private fun TextView.startAnimation(): ObjectAnimator {
        val width = width
        val wrappedView = object {
            fun getWidth(): Int {
                return layoutParams.width
            }

            fun setWidth(width: Int) {
                layoutParams = layoutParams.apply {
                    this.width = width
                    this.height = width
                }
            }
        }
        val minWidth = paint.measureText(text.toString()).toInt()
        return ObjectAnimator.ofInt(wrappedView, "width", width, minWidth).apply {
            duration = 500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
    }
}