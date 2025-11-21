package com.jingtian.demoapp.main.list.colorblock

import android.content.Context
import android.graphics.Color
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.main.ColorUtils.revers
import com.jingtian.demoapp.main.ScreenUtils.screenWidth
import com.jingtian.demoapp.main.TextUtils.measure
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.px2Dp
import com.jingtian.demoapp.main.widget.appbar.IAppBarAdapter
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

class AppBarColorBlockHolder(context: Context) :
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
    fun onBind(value: Int, color: Int, height: Int, isScrollable: Boolean, position: Int) {
        itemView.initLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        itemView.setBackgroundColor(color)
        textView.text = "$value ${if (!isScrollable) "*" else ""}"
        textView.setTextColor(color.revers)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, autoAdjustTextSize(position.toString(), 0f, min(ITEM_WIDTH, height.toFloat()).px2Dp, ITEM_WIDTH.toInt().toFloat().px2Dp, height.toFloat().px2Dp).dp)
    }

    private fun View.initLayoutParams(width: Int, height: Int) {
        this.layoutParams = this.layoutParams?.also { lp->
            lp.width = width
            lp.height = height
        } ?: RecyclerView.LayoutParams(width, height)
    }
}

class AppBarColorBlockAdapter(private val minHeight: Int = 50, private val maxHeight: Int = 400) : RecyclerView.Adapter<AppBarColorBlockHolder>(), IAppBarAdapter {
    private val random = Random
    private val dataList = mutableListOf<AppBarColorBlockModel>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppBarColorBlockHolder {
        return AppBarColorBlockHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: AppBarColorBlockHolder, position: Int) {
        val (value, color, height, isScrollable) = dataList[position]
        holder.onBind(value, color, height, isScrollable, position)
        holder.itemView.setOnClickListener {
            val cnt = Random.nextInt(1, 5)
            repeat(cnt) {
                addData(holder.bindingAdapterPosition + 1)
            }
            notifyItemRangeInserted(holder.bindingAdapterPosition + 1, cnt)
        }
        holder.itemView.setOnLongClickListener {
            val cnt = min(dataList.size - holder.bindingAdapterPosition, Random.nextInt(1, 5))
            repeat(cnt) {
                dataList.removeAt(holder.bindingAdapterPosition)
            }
            notifyItemRangeRemoved(holder.bindingAdapterPosition, cnt)
            true
        }
    }

    fun addData(position: Int = dataList.size, value: Int = dataList.size) {
        dataList.add(position, createRandomItem(value))
    }

    fun insert(list: List<AppBarColorBlockModel>, position: Int = dataList.size) {
        dataList.addAll(position, list)
        notifyItemRangeInserted(position, list.size)
    }

    fun createRandomItem(value: Int): AppBarColorBlockModel {
        return AppBarColorBlockModel(
            value,
            Color.argb(
                255,
                random.nextInt(0, 255),
                random.nextInt(0, 255),
                random.nextInt(0, 255)
            ),
            random.nextInt(minHeight, maxHeight).toFloat().dp.toInt(),
            random.nextInt(0, 10) != 6
        )
    }

    override fun getScrollMode(position: Int): Boolean {
        return dataList[position].isScrollable
    }
}

class AppBarColorBlockModel(
    val value: Int,
    @ColorInt val color: Int,
    @Px val height: Int,
    val isScrollable: Boolean
) {
    @ColorInt
    operator fun component1(): Int {
        return value
    }

    @ColorInt
    operator fun component2(): Int {
        return color
    }

    @Px
    operator fun component3(): Int {
        return height
    }

    operator fun component4(): Boolean {
        return isScrollable
    }
}