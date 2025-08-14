package com.jingtian.demoapp.main.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView

class FixedNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    NestedScrollView(context, attrs, defStyleAttr) {

    private var recyclerView: RecyclerView? = null

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (shouldConsumeVerticalScroll(target, dy)) {
            super.onNestedScroll(target, 0, 0, dx, dy, type, consumed)
        }
        super.onNestedPreScroll(target, dx, dy, consumed, type)
    }

    private fun shouldConsumeVerticalScroll(target: View, dy: Int): Boolean {
        if (target != recyclerView) {
            return false
        }
        return dy < 0 && !target.canScrollVertically(-1)
                || dy > 0 && canScrollVertically(1)
    }

    fun setInnerRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun fling(velocityY: Int) {
        super.fling(velocityY)
        if (velocityY > 0) {
            recyclerView?.fling(0, velocityY)
        }
    }

    private fun MotionEvent.insideOf(view: View?): Boolean {
        view ?: return false
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        val w = view.width
        val h = view.height
        return this.rawX >= x && this.rawX <= x + w && this.rawY >= y && this.rawY <= y + h
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.insideOf(recyclerView)) {
            return false
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        Log.d("FixedNestedScrollView", "onTouchEvent: $motionEvent")
        return super.onTouchEvent(motionEvent)
    }

}
