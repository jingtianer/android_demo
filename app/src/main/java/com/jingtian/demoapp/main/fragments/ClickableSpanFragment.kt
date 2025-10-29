package com.jingtian.demoapp.main.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.FragmentClickableSpanBinding
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.widget.ClickableSimpleToast

class ClickableSpanFragment : BaseFragment() {
    private lateinit var binding: FragmentClickableSpanBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentClickableSpanBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.text.movementMethod = LinkMovementMethod.getInstance()
        binding.text.text = buildClickableString()
        binding.text.highlightColor = Color.TRANSPARENT
        binding.button.setOnClickListener {
            ClickableSimpleToast.show(context, buildClickableString(), Toast.LENGTH_LONG)
        }
    }
    
    private fun buildClickableString(): CharSequence {
        val sb = SpannableStringBuilder("啦啦啦，点击这里！>")
        sb.setSpan(ForegroundColorSpan(Color.GREEN), 6, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "点击了ClickableSpan", Toast.LENGTH_SHORT).show()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
            }

        }, 6, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(ImageSpan(ResourcesCompat.getDrawable(requireContext().resources, R.drawable.arrow_down, null)?.mutate()?.apply {
            setBounds(0, 0, 18f.dp.toInt(), 18f.dp.toInt())
        } ?: return sb), 9, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }
}