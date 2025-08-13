package com.jingtian.demoapp.main.fragments

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jingtian.demoapp.databinding.FragmentNbPlusTextBinding
import com.jingtian.demoapp.main.dp

class NBPlusTextFragment : BaseFragment() {
    private lateinit var binding: FragmentNbPlusTextBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNbPlusTextBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.nbText) {
            setTextSize(32f.dp)
            setText("周末精神状态")
            setTextColor(Color.argb(1f, 0.2f, 0.65f, 0.3f))
            setDuration(10000)
            start()
        }
        with(binding.nbText1) {
            setTextSize(48f.dp)
            setText("打工人精神状态")
            setTextColor(Color.RED)
            start()
        }
        with(binding.nbText2) {
            setTextSize(64f.dp)
            setText("就这个战斗")
            setTextColor(Color.RED)
            setProgress(0.24f)
            start()
        }
        with(binding.nbText3) {
            setTextSize(128f.dp)
            setText("爽")
            setTextColor(Color.RED)
            setProgress(0.5f)
            start()
        }
    }
}