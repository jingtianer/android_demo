package com.jingtian.demoapp.main.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jingtian.demoapp.databinding.FragmentSpannableBinding

class SpannableStringFragment : BaseFragment() {

    lateinit var binding: FragmentSpannableBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSpannableBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tv.text = buildSpannable(listOf(
            "123456789*" to listOf(intArrayOf(1, 3)),
            "123456789*123456789*" to listOf(intArrayOf(4, 13)),
            "123456789*123456789*123456789*123456789*" to listOf(intArrayOf(22, 32)),
            "123456789*123456789*123456789*123456789*123456789*123456789*" to listOf(intArrayOf(12, 42)),
            "123456789*123456789*123456789*123456789*123456789*" to listOf(intArrayOf(2, 7), intArrayOf(12, 32)),
        ))
        binding.tvFull.text = buildSpannable(listOf(
            "123456789*" to listOf(intArrayOf(1, 3)),
            "123456789*123456789*" to listOf(intArrayOf(4, 13)),
            "123456789*123456789*123456789*123456789*" to listOf(intArrayOf(22, 32)),
            "123456789*123456789*123456789*123456789*123456789*123456789*" to listOf(intArrayOf(12, 42)),
            "123456789*123456789*123456789*123456789*123456789*" to listOf(intArrayOf(2, 7), intArrayOf(12, 32)),
        ))
        super.onViewCreated(view, savedInstanceState)
    }

    // 注意返回值
    fun buildSpannable(highLightList: List<Pair<String, List<IntArray>>>): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        for ((string, posList) in highLightList) {
            for ((start, end) in posList) {// start 和 end 本意是从0开始，颜色范围包含start，包含end，但下面使用SPAN_flag不管用哪个都是一样的
                val spannable = SpannableString(string)
                spannable.setSpan(ForegroundColorSpan(Color.RED), start, end + 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
                spannable.setSpan(ForegroundColorSpan(Color.BLUE), string.length - 1, string.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.append(spannable)
            }
        }
        return sb
    }
}