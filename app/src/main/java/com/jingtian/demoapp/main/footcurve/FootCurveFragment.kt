package com.jingtian.demoapp.main.footcurve

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.FragmentFootCurveBinding
import com.jingtian.demoapp.databinding.PanelPopupFootCurveConfigBinding
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.fragments.BaseFragment

@BaseFragment.FragmentInfo(desc = "垂足曲线")
class FootCurveFragment: BaseFragment() {
    private lateinit var binding: FragmentFootCurveBinding

    private lateinit var popUpBinding: PanelPopupFootCurveConfigBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFootCurveBinding.inflate(inflater)
        popUpBinding = PanelPopupFootCurveConfigBinding.bind(binding.popUpMenu.bottomContentView)
        return binding.root
    }

    private var silentWatching: Boolean = false


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnTouchListener { v, event ->
            binding.root.requestDisallowInterceptTouchEvent(true)
            return@setOnTouchListener true
        }
        val initCurve = Curve()
        binding.footCurveView.curve = initCurve

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

        with(popUpBinding) {
            xExpr.watchString(initCurve.xExprStr) {
                this.copy(xExprStr = it ?: "")
            }

            yExpr.watchString(initCurve.yExprStr) {
                this.copy(yExprStr = it ?: "")
            }

            tLabel.watchString(initCurve.paramName) {
                this.copy(paramName = it ?: "")
            }

            tMinValue.watchValue(initCurve.tMin) {
                this.copy(tMin = it ?: 0f)
            }

            tMaxValue.watchValue(initCurve.tMax) {
                this.copy(tMax = it ?: 0f)
            }

            fixPointX.watchValue(initCurve.initPx) {
                this.copy(initPx = it ?: 0f)
            }

            fixPointY.watchValue(initCurve.initPy) {
                this.copy(initPy = it ?: 0f)
            }

            // SeekBar 映射参数t
            val range = initCurve.tMax - initCurve.tMin
            seekBarT.max = 1000
            seekBarT.progress = (1000 * (initCurve.initT - initCurve.tMin) * 1f / (initCurve.tMax - initCurve.tMin)).toInt()
            seekBarT.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val t = initCurve.tMin + range * progress / 1000f
                    binding.footCurveView.currentT = t
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            binding.footCurveView.pxPyChangeListener.add { px, py->
                silentWatching = true
                fixPointX.setText(String.format("%.2f", px))
                fixPointY.setText(String.format("%.2f", py))
                silentWatching = false
            }
        }
    }

    private inline fun EditText.watchValue(initValue: Float, crossinline onUpdate: Curve.(Float?)->Curve) {
        this.setText(String.format("%.2f", initValue))
        this.addTextChangedListener(afterTextChanged = {
            if (silentWatching) {
                return@addTextChangedListener
            }
            val value = it?.takeIf { !it.isNullOrEmpty() }?.toString()?.let {
                FootCurveMath.eval(it)
            }
            binding.footCurveView.curve = binding.footCurveView.curve.onUpdate(value)
        })
    }

    private inline fun EditText.watchString(initValue: String, crossinline onUpdate: Curve.(String?)->Curve) {
        this.setText(initValue)
        this.addTextChangedListener(afterTextChanged = {
            if (silentWatching) {
                return@addTextChangedListener
            }
            val value = it?.takeIf { !it.isNullOrEmpty() }?.toString()
            binding.footCurveView.curve = binding.footCurveView.curve.onUpdate(value)
        })
    }
}