package com.jingtian.demoapp.main.lissajous

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.FragmentLisajousCurveBinding
import com.jingtian.demoapp.databinding.PanelPopupLisajousConfigBinding
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.fragments.BaseFragment
import kotlin.random.Random

@BaseFragment.FragmentInfo(desc = "李萨如曲线")
class LisajousCurveFragment @JvmOverloads constructor(
    config: DefaultConfigs.DefConfig = DefaultConfigs.defaultConfigs.random(),
    private val lisajousCurve: LisajousCurve = config.lisajousCurve,
    private val fromMain: Boolean = true,
    speed: Int = config.speed
): BaseFragment() {
    private lateinit var binding: FragmentLisajousCurveBinding

    private lateinit var popUpBinding: PanelPopupLisajousConfigBinding

    private var index = Random.nextInt(0, DefaultConfigs.defaultConfigs.size)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLisajousCurveBinding.inflate(inflater)
        popUpBinding = PanelPopupLisajousConfigBinding.bind(binding.popUpMenu.bottomContentView)
        return binding.root
    }

    private var silentWatching: Boolean = false
    private var visible: Boolean = false

    private var xSpeed: Int = speed
        set(value) {
            field = value
            if (field > 0) {
                updateX()
            }
        }

    private var pendingUpdate: Boolean = false

    private fun updateAngel(update: ()->LisajousCurve) {
        if (xSpeed <= 0) {
            return
        }
        if (pendingUpdate) {
            return
        }
        if (!visible) {
            return
        }
        pendingUpdate = true
        binding.footCurveView.postDelayed({
            binding.footCurveView.lisajousCurve = update()
            pendingUpdate = false
            updateAngel(update)
        }, 50)
    }

    private fun updateX() {
        updateAngel {
            val curve = binding.footCurveView.lisajousCurve
            curve.copy(
                x = curve.x + (2 * Math.PI / 1000 * (xSpeed * xSpeed / 100)).toFloat()
            )
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnTouchListener { v, event ->
            binding.root.requestDisallowInterceptTouchEvent(true)
            return@setOnTouchListener true
        }
        val initCurve = if (fromMain) {
            DefaultConfigs.defaultConfigs[index].lisajousCurve
        } else {
            lisajousCurve
        }
        binding.footCurveView.lisajousCurve = initCurve

        binding.popUpMenu.animator.duration = 300
        with(binding.popUpMenu) {
            title.text = "画布设置"
            title.setTextColor(ResourcesCompat.getColor(context.resources, R.color.color_toast_text_color, null))
            title.textSize = 24f
            bind(this)
            animator.duration = 300
            with(icon) {
                val drawable = AppCompatResources.getDrawable(app, R.drawable.arrow_down)
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
        }

        binding.seekBarX.progress = xSpeed
        updateX()
        binding.seekBarX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                xSpeed = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        with(popUpBinding) {
            if (fromMain) {
                more.visibility = View.VISIBLE
                more.setOnClickListener {
                    LisajousCurveActivity.startLisajousCurveActivity(context ?: return@setOnClickListener)
                }
            }
            aExpr.watchValue(initCurve.a) {
                this.copy(a = it ?: 0f)
            }

            bExpr.watchValue(initCurve.b) {
                this.copy(b = it ?: 0f)
            }

            xExpr.watchValue(initCurve.x) {
                this.copy(x = it ?: 0f)
            }
            tMinValue.watchValue(initCurve.tMin) {
                this.copy(tMin = it ?: 0f).apply {
                }
            }

            tMaxValue.watchValue(initCurve.tMax) {
                this.copy(tMax = it ?: 0f).apply {
                }
            }

            tStepCount.watchValue(initCurve.stepCount) {
                this.copy(stepCount = it ?: 300)
            }
        }
    }

    private inline fun EditText.watchValue(initValue: Float, crossinline onUpdate: LisajousCurve.(Float?)->LisajousCurve) {
        this.setText(String.format("%.2f", initValue))
        this.addTextChangedListener(afterTextChanged = {
            if (silentWatching) {
                return@addTextChangedListener
            }
            val value = it?.takeIf { !it.isNullOrEmpty() }?.toString()?.let {
                LisajousCurveMath.eval(it)
            }
            binding.footCurveView.lisajousCurve = binding.footCurveView.lisajousCurve.onUpdate(value)
        })
    }

    private inline fun EditText.watchValue(initValue: Int, crossinline onUpdate: LisajousCurve.(Int?)->LisajousCurve) {
        this.setText(String.format("%d", initValue))
        this.addTextChangedListener(afterTextChanged = {
            if (silentWatching) {
                return@addTextChangedListener
            }
            val value = it?.takeIf { !it.isNullOrEmpty() }?.toString()?.let {
                LisajousCurveMath.evalInt(it)
            }
            binding.footCurveView.lisajousCurve = binding.footCurveView.lisajousCurve.onUpdate(value)
        })
    }

    override fun onResume() {
        super.onResume()
        visible = true
        updateX()
    }

    override fun onPause() {
        super.onPause()
        visible = false

    }
}