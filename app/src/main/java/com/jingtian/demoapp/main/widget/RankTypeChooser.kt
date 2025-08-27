package com.jingtian.demoapp.main.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.WidgetRankChooserBinding
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.model.RankItemRankType
import kotlin.math.max

class RankTypeChooser @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
): FrameLayout(context, attributeSet, defStyleAttr, defStyleRes)  {
    private val binding = WidgetRankChooserBinding.inflate(LayoutInflater.from(context), this, true)
    init {
        var height = 0
        val apply : View.(RankItemRankType) -> Unit = { rankItemRankType ->
            val selected = createBg(rankItemRankType)
            val unselected = createUnselectedBg(rankItemRankType)

            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_checked), selected)
                addState(intArrayOf(-android.R.attr.state_checked), unselected)
            }
            layoutParams.width = max(selected.getWidth(), unselected.getWidth()).toInt() + paddingLeft + paddingRight
            height = max(height, max(selected.getHeight(), unselected.getHeight()).toInt())
        }
        binding.rank1.apply(RankItemRankType.夯)
        binding.rank2.apply(RankItemRankType.顶尖)
        binding.rank3.apply(RankItemRankType.人上人)
        binding.rank4.apply(RankItemRankType.NPC)
        binding.rank5.apply(RankItemRankType.史)
        binding.rank1.layoutParams.height = height
        binding.rank2.layoutParams.height = height
        binding.rank3.layoutParams.height = height
        binding.rank4.layoutParams.height = height
        binding.rank5.layoutParams.height = height
    }
    companion object {
        fun createBg(rankType: RankItemRankType) : StrokeTextDrawable {
            return StrokeTextDrawable(Color.argb(
                rankType.a, rankType.r, rankType.g, rankType.b
            )).apply {
                setText(rankType.name)
                setStrokeColor(Color.WHITE, 2f.dp)
                setTextSize(28f.dp)
                setTextColor(Color.BLACK)
            }
        }
        fun createUnselectedBg(rankType: RankItemRankType) : StrokeTextDrawable {
            return StrokeTextDrawable(Color.TRANSPARENT).apply {
                setText(rankType.name)
                setStrokeColor(Color.TRANSPARENT, 0f.dp)
                setTextSize(28f.dp)
                setTextColor(Color.BLACK)
            }
        }
    }

    fun getRankType(): RankItemRankType {
        return when(binding.root.checkedRadioButtonId) {
            binding.rank1.id -> {
                RankItemRankType.夯
            }
            binding.rank2.id -> {
                RankItemRankType.顶尖
            }
            binding.rank3.id -> {
                RankItemRankType.人上人
            }
            binding.rank4.id -> {
                RankItemRankType.NPC
            }
            binding.rank5.id -> {
                RankItemRankType.史
            }
            else -> {
                RankItemRankType.NONE
            }
        }
    }

    fun setRankType(rankType: RankItemRankType) {
        when(rankType) {
            RankItemRankType.NONE -> {
                binding.root.clearCheck()
            }
            RankItemRankType.夯 -> {
                binding.root.check(binding.rank1.id)
            }
            RankItemRankType.顶尖 -> {
                binding.root.check(binding.rank2.id)
            }
            RankItemRankType.人上人 -> {
                binding.root.check(binding.rank3.id)
            }
            RankItemRankType.NPC -> {
                binding.root.check(binding.rank4.id)
            }
            RankItemRankType.史 -> {
                binding.root.check(binding.rank5.id)
            }
        }
    }
}