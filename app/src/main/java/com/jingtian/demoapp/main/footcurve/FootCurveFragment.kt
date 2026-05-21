package com.jingtian.demoapp.main.footcurve

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import com.jingtian.demoapp.databinding.FragmentFootCurveBinding
import com.jingtian.demoapp.main.fragments.BaseFragment

@BaseFragment.FragmentInfo(desc = "垂足曲线")
class FootCurveFragment: BaseFragment() {
    private lateinit var binding: FragmentFootCurveBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFootCurveBinding.inflate(inflater)
        return binding.root
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnTouchListener { v, event ->
            binding.root.requestDisallowInterceptTouchEvent(true)
            return@setOnTouchListener true
        }
        val initCurve = Curve()
        binding.footCurveView.curve = initCurve

        // SeekBar 映射参数t
        val range = initCurve.tMax - initCurve.tMin
        binding.seekBarT.max = 1000
        binding.seekBarT.progress = (1000 * (initCurve.initT - initCurve.tMin) * 1f / (initCurve.tMax - initCurve.tMin)).toInt()
        binding.seekBarT.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val t = initCurve.tMin + range * progress / 1000f
                binding.footCurveView.currentT = t
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
}