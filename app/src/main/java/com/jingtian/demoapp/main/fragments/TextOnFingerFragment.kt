package com.jingtian.demoapp.main.fragments

import android.animation.Animator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.FragmentTextOnFingerBinding
import com.jingtian.demoapp.databinding.SettingsTextOnFingerBinding
import com.jingtian.demoapp.main.RxEvents.setDoubleClickListener
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.insideOfView

class TextOnFingerFragment : BaseFragment() {

    private lateinit var binding: FragmentTextOnFingerBinding
    private lateinit var settingsBinding: SettingsTextOnFingerBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextOnFingerBinding.inflate(inflater, container, false)
        settingsBinding = SettingsTextOnFingerBinding.bind(binding.popUpMenu.bottomView)
        return binding.root
    }

    private fun initSettingsMenu() {
        val popUpMenu = binding.popUpMenu
        val title = popUpMenu.title
        val bottomView = popUpMenu.bottomView
        val icon = popUpMenu.icon
        popUpMenu.animator.duration = 1000
        with(title) {
            text = "画布设置"
            setTextColor(Color.BLACK)
            textSize = 24f
        }
        val drawable = AppCompatResources.getDrawable(app, R.drawable.arrow_down)
        with(icon) {
            setImageDrawable(drawable)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            post {
                val aspectRation = drawable?.let {
                    it.intrinsicWidth / it.intrinsicHeight.toFloat()
                } ?: 0f

                val lp = layoutParams
                lp.width = (aspectRation * height).toInt()
                layoutParams = lp
            }
        }
        popUpMenu.animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                bottomView.clearFocus()
                for(view in arrayOf(bottomView, settingsBinding.et, settingsBinding.textColor, settingsBinding.textSize)) {
                    view.isFocusable = false
                    view.isFocusableInTouchMode = false
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                for(view in arrayOf(bottomView, settingsBinding.et, settingsBinding.textColor, settingsBinding.textSize)) {
                    view.isFocusable = true
                    view.isFocusableInTouchMode = true
                }
            }
            override fun onAnimationCancel(animation: Animator) = Unit

            override fun onAnimationRepeat(animation: Animator) = Unit
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initRootViewTouchListener() {
        val popUpMenu = binding.popUpMenu
        val topView = popUpMenu.topView
        val root = binding.root
        val canvas = binding.canvas
        root.setOnTouchListener(object : View.OnTouchListener {
            private var needDown = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val view = if (!popUpMenu.expand) {
                    popUpMenu
                } else {
                    topView
                }
                if (event.insideOfView(view)) {
                    view.dispatchTouchEvent(event)
                    val cancelEvent = MotionEvent.obtain(event)
                    if (!needDown && event.actionMasked != MotionEvent.ACTION_DOWN) {
                        cancelEvent.action = MotionEvent.ACTION_CANCEL
                        canvas.dispatchTouchEvent(cancelEvent)
                        cancelEvent.recycle()
                        needDown = true
                    }
                } else {
                    if (needDown && event.actionMasked != MotionEvent.ACTION_DOWN) {
                        val downEvent = MotionEvent.obtain(event)
                        downEvent.action = MotionEvent.ACTION_DOWN
                        canvas.dispatchTouchEvent(downEvent)
                        downEvent.recycle()
                        needDown = false
                    }
                    canvas.dispatchTouchEvent(event)
                }
                return true
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSettingsMenu()
        initRootViewTouchListener()
        getTabView()?.setDoubleClickListener(300L) {
            binding.canvas.clearCanvas()
        }
        settingsBinding.et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString() ?: null
                if (!text.isNullOrEmpty()) {
                    binding.canvas.setDrawText(text)
                }
            }

        })

        settingsBinding.textSize.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s != null) {
                        binding.canvas.setTextSize(s.toString().toFloat())
                    }
                } catch (ignore: Exception) {

                }
            }
        })


        settingsBinding.textColor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s != null) {
                        val colors = s.toString().split(",").map {
                            it.trim().toInt()
                        }
                        val (a, r, g, b) = if (colors.size == 4) {
                            colors
                        } else if (colors.size == 3) {
                            val (r,g,b) = colors
                            listOf(255, r ,g , b)
                        } else {
                            val color = binding.canvas.getColor()
                            val alpha = ((color shr 24) and 0xFF)
                            // Red通道：取次高8位，右移16位后与0xFF
                            val red = ((color shr 16) and 0xFF)
                            // Green通道：取中8位，右移8位后与0xFF
                            val green = ((color shr 8) and 0xFF)
                            // Blue通道：取低8位，直接与0xFF
                            val blue = (color and 0xFF)
                            listOf(alpha, red, green, blue)
                        }
                        binding.canvas.setColor(Color.argb(a, r, g, b))
                    }
                } catch (ignore: Exception) {

                }
            }
        })
    }
}