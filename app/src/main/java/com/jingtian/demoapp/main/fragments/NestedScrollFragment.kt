package com.jingtian.demoapp.main.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingtian.demoapp.databinding.FragmentNestedScrollBinding
import com.jingtian.demoapp.main.RxEvents.setDoubleClickListener
import com.jingtian.demoapp.main.ScreenUtils.screenHeight
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.list.colorblock.ColorBlockAdapter
import kotlin.math.abs

@BaseFragment.FragmentInfo(desc = "NestedScrollView和RecyclerView嵌套")
class NestedScrollFragment : BaseFragment() {
    companion object {
        private const val DATA_SIZE = 200
    }

    private lateinit var binding: FragmentNestedScrollBinding
    private val adapter = ColorBlockAdapter(minHeight = 20f.dp.toInt(), maxHeight = 40f.dp.toInt())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNestedScrollBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            adapter = this@NestedScrollFragment.adapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = true
            layoutParams?.height = context.screenHeight
            binding.nestedScrollView.setInnerRecyclerView(this)
            setOnTouchListener(object : View.OnTouchListener {
                private var lastX = 0f
                private var lastY = 0f
                private val mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when(event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            lastX = event.x
                            lastY = event.y
                            stopScroll()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = abs(event.x - lastX)
                            val dy = abs(event.y - lastY)
                            if (dx > dy && dx > mTouchSlop) {
                                parent.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                    return false
                }
            })
        }
        for (i in 0 until DATA_SIZE) {
            adapter.addData()
        }
        getTabView()?.setDoubleClickListener(300L) {
            binding.nestedScrollView.scrollTo(0, 0)
            binding.recyclerView.scrollToPosition(0)
        }
    }
}