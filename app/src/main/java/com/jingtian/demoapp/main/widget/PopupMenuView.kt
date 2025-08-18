package com.jingtian.demoapp.main.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Keep
import com.jingtian.demoapp.R

class PopupMenuView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
): LinearLayout(context, attributeSet, defStyleAttr, defStyleRes) {

    companion object {
        private const val TAG = "PopupMenuView"
    }

    val bottomView: View
    val topView: View = LayoutInflater.from(context).inflate(R.layout.popup_menu_top_view, this, false)
    val icon: ImageView = topView.findViewById(R.id.icon)
    val title: TextView = topView.findViewById(R.id.title)
    val animator = ObjectAnimator.ofFloat(AnimationExecutor(), "progress", 0f, 1f)
    var expand = true
        private set
    init {
        this.orientation = LinearLayout.VERTICAL
        addView(topView)
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.PopupMenuView)
        bottomView = if (typedArray.hasValue(R.styleable.PopupMenuView_popUpMenuBottomView)) {
            val resId = typedArray.getResourceId(R.styleable.PopupMenuView_popUpMenuBottomView, 0)
            LayoutInflater.from(context).inflate(resId, this, false)
        } else {
            FrameLayout(context)
        }
        addView(bottomView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        bottomView.translationY = 0f
        bottomView.alpha = 0f
        typedArray.recycle()
        icon.setOnClickListener {
            if (animator.isStarted) {
                animator.reverse()
            } else {
                if (expand) {
                    animator.start()
                } else {
                    animator.reverse()
                }
            }
        }
        animator.addListener(AnimationObserver())
    }

    private var progress: Float = 0f

    open inner class AnimationExecutor {

        @Keep
        fun setProgress(progress: Float) {
            Log.d(TAG, "setProgress: $progress")
            this@PopupMenuView.progress = progress
            bottomView.alpha = progress
            bottomView.translationY = bottomView.height * (progress - 1)
            icon.rotation = 180 * progress
        }

        @Keep
        fun getProgress(): Float {
            return progress
        }
    }

    inner class AnimationObserver : Animator.AnimatorListener {
        private fun startHide() {
            bottomView.isFocusableInTouchMode = false
            bottomView.isFocusable = false
            bottomView.clearFocus()
        }

        private fun startShow() {
            bottomView.visibility = View.VISIBLE
            bottomView.isFocusableInTouchMode = false
            bottomView.isFocusable = false
        }

        private fun hide() {
            bottomView.isFocusableInTouchMode = false
            bottomView.isFocusable = false
            bottomView.visibility = View.GONE
            bottomView.alpha = 0f
            bottomView.translationY = -1f * bottomView.height
        }

        private fun show() {
            bottomView.alpha = 1f
            bottomView.translationY = 0f
            bottomView.isFocusableInTouchMode = true
            bottomView.isFocusable = true
        }

        override fun onAnimationStart(animation: Animator) {
            Log.d(TAG, "onAnimationEnd: $progress")
            if (expand) {
                startShow()
            } else {
                startHide()
            }
        }

        override fun onAnimationEnd(animation: Animator) {
            Log.d(TAG, "onAnimationEnd: $progress")
            if (!expand) {
                hide()
            } else {
                show()
            }
            expand = !expand
        }

        override fun onAnimationCancel(animation: Animator) {

        }

        override fun onAnimationRepeat(animation: Animator) {

        }
    }

}