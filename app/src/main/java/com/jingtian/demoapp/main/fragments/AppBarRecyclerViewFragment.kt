package com.jingtian.demoapp.main.fragments

import com.jingtian.demoapp.main.widget.appbar.AppBarLayoutManager
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.jingtian.demoapp.databinding.FragmentAppBarColorBlockBinding
import com.jingtian.demoapp.main.RxEvents.setDoubleClickListener
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.list.colorblock.AppBarColorBlockAdapter
import kotlin.math.abs

class AppBarRecyclerViewFragment : BaseFragment() {
    companion object {
        private const val DATA_SIZE = 120
    }

    private lateinit var binding: FragmentAppBarColorBlockBinding
    private val adapter =
        AppBarColorBlockAdapter(minHeight = 20f.dp.toInt(), maxHeight = 40f.dp.toInt())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppBarColorBlockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            adapter = this@AppBarRecyclerViewFragment.adapter
            layoutManager = AppBarLayoutManager(this)
            isNestedScrollingEnabled = true
            setOnTouchListener(object : View.OnTouchListener {
                private var lastX = 0f
                private var lastY = 0f
                private val mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
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
        repeat(DATA_SIZE) {
            adapter.addData()
        }
        getTabView()?.setDoubleClickListener(300L) {
            binding.recyclerView.scrollToPosition(0)
        }
    }
}