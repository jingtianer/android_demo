package com.jingtian.demoapp.main.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.main.rank.Utils
import kotlin.math.max
import kotlin.math.min

class StarRateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        interface OnScoreChange {
            fun onScoreChange(score: Float)
        }
    }

    var onScoreChange: OnScoreChange? = null

    private val fixNestedScroll = Utils.RecyclerViewUtils.FixNestedScroll(this, RecyclerView.HORIZONTAL)

    private var starCnt = 0
    private var starPadding = 0f
    private var enable = false
    private var starDrawable: Drawable? = null
    private var starGreyDrawable: Drawable? = null

    private var starSize = 0f
    private var progress = 0f
    private var score = 0f
    private var scoreInvalid = false
    private var starTotalWidth = 0f

    fun updateStarConfig(
        enable: Boolean = this.enable,
        starCnt: Int = this.starCnt,
        starPadding: Float = this.starPadding,
        starDrawable: Drawable? = this.starDrawable,
        starGreyDrawable: Drawable? = this.starGreyDrawable,
    ) {
        this.enable = enable
        this.starCnt = starCnt
        this.starPadding = starPadding
        this.starDrawable = starDrawable
        this.starGreyDrawable = starGreyDrawable
        invalidate()
        requestLayout()
    }

    private fun getStarSize(): Float {
        val width = if (starCnt <= 1) {
            measuredWidth * 1f
        } else {
            (measuredWidth - (starCnt - 1) * starPadding) / starCnt.toFloat()
        }
        return min(measuredHeight.toFloat(), width)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        starSize = getStarSize()
        starTotalWidth = starSize * starCnt + (starCnt - 1) * starPadding
        if (!scoreInvalid) {
            progress = scoreToProgress(score)
        }
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(starTotalWidth.toInt(), MeasureSpec.EXACTLY), heightMeasureSpec)
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        if (direction > 0) {
            return progress > 0
        } else if (direction < 0) {
            return progress < 1
        }
        return super.canScrollHorizontally(direction)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        fixNestedScroll.onTouch(this, event)
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> {
                if (enable && starTotalWidth > 0){
                    progress = event.x / starTotalWidth
                    scoreInvalid = true
                    val onScoreChange = onScoreChange
                    if (onScoreChange != null) {
                        onScoreChange.onScoreChange(progressToScore())
                    }
                    invalidate()
                }
            }
        }
        val ret = super.onTouchEvent(event)
        return enable || ret
    }

    fun setScore(score: Float) {
        scoreInvalid = false
        progress = scoreToProgress(score)
        this.score = score
        invalidate()
    }

    fun getScore(): Float {
        if (scoreInvalid) {
            scoreInvalid = false
            score = progressToScore()
        }
        return score
    }

    private fun progressToScore(): Float {
        var star = 0
        if (progress > 0.5f) {
            for (i in starCnt - 1 downTo 0) {
                val p = ((starSize + starPadding) * i) / starTotalWidth
                if (p < progress) {
                    star = i
                    break
                }
            }
        } else {
            for (i in 0..< starCnt) {
                val p = ((starSize + starPadding) * i) / starTotalWidth
                if (p > progress) {
                    star = i - 1
                    break
                }
            }
        }
        var remainedWidth = starTotalWidth * progress - star * (starSize + starPadding)
        if (remainedWidth > starSize) {
            remainedWidth = starSize
        }
        return max(star + remainedWidth / starSize, 0f)
    }

    private fun scoreToProgress(score: Float): Float {
        val starCnt = score.toInt()
        val width = starCnt * (starSize + starPadding) + starSize * (score - starCnt)
        return width / starTotalWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val starDrawable = this.starDrawable ?: return
        val starGreyDrawable = this.starGreyDrawable ?: return
        val savedCount: Int
        savedCount = canvas.save()
        val highlightedWith = starTotalWidth * progress
        canvas.clipRect(0f, 0f, highlightedWith, measuredHeight.toFloat())
        for(i in 0 until starCnt) {
            val left = i * (starSize + starPadding)
            starDrawable.setBounds(
                left.toInt(),
                0,
                (left + starSize).toInt(),
                starSize.toInt()
            )
            starDrawable.draw(canvas)
        }
        canvas.restoreToCount(savedCount)

        canvas.clipRect(highlightedWith, 0f, starTotalWidth, measuredHeight.toFloat())
        for(i in 0 until starCnt) {
            val left = i * (starSize + starPadding)
            starGreyDrawable.setBounds(
                left.toInt(),
                0,
                (left + starSize).toInt(),
                starSize.toInt()
            )
            starGreyDrawable.draw(canvas)
        }
        canvas.restoreToCount(savedCount)
    }
}