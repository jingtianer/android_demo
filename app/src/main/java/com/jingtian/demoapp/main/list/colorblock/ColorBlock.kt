package com.jingtian.demoapp.main.list.colorblock

import android.content.Context
import android.graphics.Color
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.main.ColorUtils.revers
import com.jingtian.demoapp.main.ScreenUtils.screenWidth
import com.jingtian.demoapp.main.TextUtils.measure
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.px2Dp
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

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

class ColorBlockAdapter(private val minHeight: Int = 50, private val maxHeight: Int = 400) : RecyclerView.Adapter<ColorBlockHolder>() {
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
        holder.itemView.setOnClickListener {
            addData(holder.bindingAdapterPosition + 1)
            notifyItemInserted(holder.bindingAdapterPosition + 1)
        }
        holder.itemView.setOnLongClickListener {
            dataList.removeAt(holder.bindingAdapterPosition)
            notifyItemRemoved(holder.bindingAdapterPosition)
            true
        }
    }

    fun addData(position: Int = dataList.size) {
        dataList.add(
            position,
            Color.argb(
                255,
                random.nextInt(0, 255),
                random.nextInt(0, 255),
                random.nextInt(0, 255)
            ) to random.nextInt(minHeight, maxHeight).toFloat().dp.toInt()
        )
    }
}