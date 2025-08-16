package com.jingtian.demoapp.main.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import com.jingtian.demoapp.databinding.FragmentTextOnFingerBinding

class TextOnFingerFragment : BaseFragment() {

    lateinit var binding: FragmentTextOnFingerBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextOnFingerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.et.addTextChangedListener(object : TextWatcher {
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

        binding.textSize.addTextChangedListener(object : TextWatcher {
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


        binding.textColor.addTextChangedListener(object : TextWatcher {
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