package com.jingtian.demoapp.main.fragments

import com.jingtian.demoapp.main.widget.appbar.AppBarLayoutManager
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import com.jingtian.demoapp.databinding.FragmentAppBarColorBlockBinding
import com.jingtian.demoapp.main.RxEvents.setDoubleClickListener
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.list.colorblock.AppBarColorBlockAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

@BaseFragment.FragmentInfo(desc = "支持吸顶的RecyclerView\n(自定义LayoutManager)")
class AppBarRecyclerViewFragment : BaseFragment() {
    companion object {
        private const val DATA_SIZE = 999
        private const val BATCH = 3000
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
            layoutManager = AppBarLayoutManager(this@AppBarRecyclerViewFragment.adapter)
            adapter = this@AppBarRecyclerViewFragment.adapter
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
        val dataSize = DATA_SIZE - adapter.itemCount
        insertData(dataSize)
        getTabView()?.setDoubleClickListener(300L) {
            binding.recyclerView.scrollToPosition(0)
        }
    }

    private fun insertData(cnt: Int) {
        var dataSize = cnt
        Log.d("jingtian", "insertData: $cnt")
        lifecycleScope.launch(Dispatchers.Default + Job()) {
            while (dataSize > 0) {
                val dataList = withContext(Dispatchers.Default) {
                    (0..min(dataSize, BATCH)).map { adapter.createRandomItem(DATA_SIZE - dataSize + it) }
                }
                withContext(Dispatchers.Main + Job()) {
                    binding.recyclerView.post {
                        adapter.insert(dataList)
                    }
                }
                dataSize -= dataList.size
            }
        }
    }
}