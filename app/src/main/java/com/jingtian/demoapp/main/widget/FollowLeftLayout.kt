package com.jingtian.demoapp.main.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.jingtian.demoapp.R

class FollowLeftLayout @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        inflate(context, R.layout.widget_follow_left, this)
    }

    val tailContainer: LinearLayout = findViewById(R.id.tail)

    fun addHead(view: View, layoutParams: ViewGroup.LayoutParams) {
        addView(view, LayoutParams(layoutParams).followLeft())
    }

    fun addHead(view: View, layoutParams: MarginLayoutParams) {
        addView(view, LayoutParams(layoutParams).followLeft())
    }

    private fun RelativeLayout.LayoutParams.followLeft(): RelativeLayout.LayoutParams {
        this.addRule(CENTER_VERTICAL)
        this.addRule(START_OF, R.id.tail)
        return this
    }

    fun addTail(view: View) {
        tailContainer.addView(view)
    }

    fun addTail(view: View, index: Int) {
        tailContainer.addView(view, index)
    }
}