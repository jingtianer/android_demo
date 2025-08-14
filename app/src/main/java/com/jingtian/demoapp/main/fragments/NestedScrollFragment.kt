package com.jingtian.demoapp.main.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.FragmentNestedScrollBinding
import com.jingtian.demoapp.main.ColorUtils.revers
import com.jingtian.demoapp.main.RxEvents.setDoubleClickListener
import com.jingtian.demoapp.main.ScreenUtils.screenHeight
import com.jingtian.demoapp.main.ScreenUtils.screenWidth
import com.jingtian.demoapp.main.TextUtils.measure
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.px2Dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

class NestedScrollFragment : BaseFragment() {
    private lateinit var binding: FragmentNestedScrollBinding
    private val adapter = ColorBlockAdapter()

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
        for (i in 0 until 200) {
            adapter.addData()
        }
        getTabView()?.setDoubleClickListener(300L) {
            binding.nestedScrollView.scrollTo(0, 0)
            binding.recyclerView.scrollToPosition(0)
        }
    }

    class ColorBlockHolder(context: Context) :
        RecyclerView.ViewHolder(TextView(context)) {

        private val textView = itemView as TextView

        companion object {
            private val ITEM_WIDTH = app.screenWidth - 40f.dp
            private val paint = TextPaint()
            private val measuredTextSize = FloatArray(2)

            private fun autoAdjustTextSize(text: String, left: Float, right: Float, width: Float, height: Float): Float {
                var l = left.toInt()
                var r = ceil(right.toDouble()).toInt()
                while(l <= r) {
                    Log.d("TAG", "autoAdjustTextSize: l=$l, r=$r")
                    val mid = ((r - l) / 2f  + l).toInt()
                    paint.textSize = mid.toFloat()
                    paint.measure(text, measuredTextSize)
                    if (measuredTextSize[0] > width || measuredTextSize[1] > height) {
                        r = mid - 1
                    } else {
                        l = mid + 1
                    }
                }
                return r.toFloat()
            }
        }

        init {
            textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            textView.gravity = Gravity.CENTER
        }
        fun onBind(color: Int, height: Int, position: Int) {
            itemView.layoutParams = if (itemView.layoutParams != null) {
                itemView.layoutParams.height = height
                itemView.layoutParams.width = RecyclerView.LayoutParams.MATCH_PARENT
                itemView.layoutParams
            } else {
                RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, height)
            }
            itemView.setBackgroundColor(color)
            textView.text = position.toString()
            textView.setTextColor(color.revers)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, autoAdjustTextSize(position.toString(), 0f, min(ITEM_WIDTH, height.toFloat()).px2Dp, ITEM_WIDTH.toInt().toFloat().px2Dp, height.toFloat().px2Dp).dp)
        }
    }

    class ColorBlockAdapter : RecyclerView.Adapter<ColorBlockHolder>() {
        private val random = Random
        private val dataList = mutableListOf<Pair<Int, Int>>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorBlockHolder {
            return ColorBlockHolder(parent.context)
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun onBindViewHolder(holder: ColorBlockHolder, position: Int) {
            val (color, height) = dataList[position]
            holder.onBind(color, height, position)
        }

        fun addData() {
            dataList.add(
                Color.argb(
                    255,
                    random.nextInt(0, 255),
                    random.nextInt(0, 255),
                    random.nextInt(0, 255)
                ) to random.nextInt(50, 400).toFloat().dp.toInt()
            )
        }
    }
}