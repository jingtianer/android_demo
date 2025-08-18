package com.jingtian.demoapp.main.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
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


    private val bottomView: FrameLayout = FrameLayout(context)
    val topView: View = LayoutInflater.from(context).inflate(R.layout.popup_menu_top_view, this, false)
    val icon: ImageView = topView.findViewById(R.id.icon)
    val title: TextView = topView.findViewById(R.id.title)
    val animator = ObjectAnimator.ofFloat(AnimationExecutor(), "progress", 0f, 1f)
    var expand = true
        private set

    val bottomContentView: View

    init {
        this.orientation = LinearLayout.VERTICAL
        addView(topView)
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.PopupMenuView)
        bottomContentView = if (typedArray.hasValue(R.styleable.PopupMenuView_popUpMenuBottomView)) {
            val resId = typedArray.getResourceId(R.styleable.PopupMenuView_popUpMenuBottomView, 0)
            LayoutInflater.from(context).inflate(resId, bottomView, false)
        } else {
            ViewStub(context)
        }
        bottomView.addView(bottomContentView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        bottomContentView.translationY = 0f
        bottomContentView.alpha = 0f
        addView(bottomView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        typedArray.recycle()
        animator.addListener(AnimationObserver())
    }

    private var progress: Float = 0f

    fun bind(view: View) {
        view.setOnClickListener {
            if (animator.isStarted) {
                expand = !expand
                animator.reverse()
            } else {
                if (expand) {
                    animator.start()
                } else {
                    animator.reverse()
                }
            }
        }
    }

    open inner class AnimationExecutor {

        @Keep
        fun setProgress(progress: Float) {
//            Log.d(TAG, "setProgress: $progress")
            this@PopupMenuView.progress = progress
            bottomContentView.alpha = progress
            bottomContentView.translationY = bottomView.height * (progress - 1)
            icon.rotation = 180 * progress
        }

        @Keep
        fun getProgress(): Float {
            return progress
        }
    }

    inner class AnimationObserver : Animator.AnimatorListener {
        private fun startHide() {
        }

        private fun startShow() {
            bottomContentView.visibility = View.VISIBLE
        }

        private fun hide() {
            bottomContentView.visibility = View.GONE
            bottomContentView.alpha = 0f
            bottomContentView.translationY = -1f * bottomView.height
        }

        private fun show() {
            bottomContentView.alpha = 1f
            bottomContentView.translationY = 0f
        }

        override fun onAnimationStart(animation: Animator) {
            Log.d(TAG, "onAnimationStart: $progress")
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