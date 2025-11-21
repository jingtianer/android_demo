package com.jingtian.demoapp.main.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingtian.demoapp.databinding.FragmentColorBlockBinding
import com.jingtian.demoapp.main.RxEvents.setDoubleClickListener
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.list.colorblock.ColorBlockAdapter
import com.jingtian.demoapp.main.widget.HeightChangeRecyclerViewAnimator
import kotlin.math.abs

@BaseFragment.FragmentInfo(desc = "自定义ItemAnimator动画")
class ColorBlockFragment : BaseFragment() {
    companion object {
        private const val DATA_SIZE = 3
        private const val BOTTOM_DATA_SIZE = 100
    }

    private lateinit var binding: FragmentColorBlockBinding
    private val adapter = ColorBlockAdapter(minHeight = 20f.dp.toInt(), maxHeight = 40f.dp.toInt())
    private val bottomAdapter = ColorBlockAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentColorBlockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            adapter = this@ColorBlockFragment.adapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            isNestedScrollingEnabled = true
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
            itemAnimator = HeightChangeRecyclerViewAnimator(this)
        }
        repeat(DATA_SIZE) {
            adapter.addData()
        }
        repeat(BOTTOM_DATA_SIZE) {
            bottomAdapter.addData()
        }
        getTabView()?.setDoubleClickListener(300L) {
            binding.nestedScrollView.scrollTo(0, 0)
            binding.recyclerView.scrollToPosition(0)
        }
        binding.recyclerViewBottom.apply {
            adapter = this@ColorBlockFragment.bottomAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
    }
}