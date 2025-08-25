package com.jingtian.demoapp.main.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.main.rank.Utils

class FixHorizontalScrollRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private val fixHorizontalScroll = Utils.RecyclerViewUtils.FixNestedScroll(this, RecyclerView.HORIZONTAL)


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        fixHorizontalScroll.onTouch(this, ev)
        return super.dispatchTouchEvent(ev)
    }
}