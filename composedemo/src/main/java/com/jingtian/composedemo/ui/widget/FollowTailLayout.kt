package com.jingtian.composedemo.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.jingtian.composedemo.R

class FollowTailLayout @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        inflate(context, R.layout.widget_follow_left, this)
    }

    val tailContainer: LinearLayout = findViewById(R.id.tail)

    val headView: MutableList<View> = mutableListOf()
    var orientation: Int = Gravity.LEFT
        set(value) {
            gravity = field
            field = value
            initLp()
        }

    init {
        gravity = orientation
        initLp()
    }
    fun addHead(view: View, layoutParams: ViewGroup.LayoutParams) {
        headView.add(view)
        addView(view, LayoutParams(layoutParams).tailLp())
    }

    fun addHead(view: View, layoutParams: MarginLayoutParams) {
        headView.add(view)
        addView(view, LayoutParams(layoutParams).tailLp())
    }

    private fun RelativeLayout.LayoutParams.tailLp(): RelativeLayout.LayoutParams {
        this.addRule(CENTER_VERTICAL)
        if (orientation == Gravity.LEFT) {
            this.addRule(START_OF, R.id.tail)
            this.removeRule(END_OF)
        } else if (orientation == Gravity.RIGHT) {
            this.addRule(END_OF, R.id.tail)
            this.removeRule(START_OF)
        }
        return this
    }

    private fun ViewGroup.LayoutParams?.relativeLayout(): RelativeLayout.LayoutParams? {
        return if (this == null) {
            null
        } else if (this is RelativeLayout.LayoutParams) {
            this
        } else {
            RelativeLayout.LayoutParams(this)
        }
    }

    private fun RelativeLayout.LayoutParams.headLp(): RelativeLayout.LayoutParams {
        this.addRule(CENTER_VERTICAL)
        if (orientation == Gravity.LEFT) {
            this.addRule(ALIGN_PARENT_END)
            this.removeRule(ALIGN_PARENT_START)
        } else if (orientation == Gravity.RIGHT) {
            this.addRule(ALIGN_PARENT_START)
            this.removeRule(ALIGN_PARENT_END)
        }
        return this
    }

    fun addTail(view: View) {
        tailContainer.addView(view)
    }

    fun addTail(view: View, index: Int) {
        tailContainer.addView(view, index)
    }

    private fun initLp() {
        headView.forEach { view->
            view.layoutParams = view.layoutParams.relativeLayout()?.headLp()
        }
        tailContainer.layoutParams = tailContainer.layoutParams.relativeLayout()?.tailLp()
    }
}