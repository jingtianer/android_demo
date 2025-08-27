package com.jingtian.demoapp.main.rank.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.ActivityRankBinding
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.adapter.LinkedListAdapter
import com.jingtian.demoapp.main.rank.model.RankItemRankType
import com.jingtian.demoapp.main.widget.StrokeTextDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RankActivity : BaseActivity() {
    companion object {
        fun startActivity(context: Context, rankName: String) {
            context.startActivity(Intent(context, RankActivity::class.java).apply {
                putExtra(KEY_RANK_NAME, rankName)
            })
        }

        private const val KEY_RANK_NAME = "RANK_NAME"
        private const val ITEM_MARGIN = 2f
    }

    private lateinit var binding: ActivityRankBinding
    private val rankAdapter = LinkedListAdapter(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRankViews()
        initRankRecyclerView()
    }
    private fun initRankRecyclerView() {
        val rankName = intent.getStringExtra(KEY_RANK_NAME)
        if (rankName.isNullOrEmpty()) {
            return
        }
        with(binding.recyclerView) {
            layoutManager = GridLayoutManager(context, RankItemRankType.entries.size - 1, GridLayoutManager.HORIZONTAL, false)
            adapter = rankAdapter
            val ignore = lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    Utils.DataHolder.rankDB.rankItemDao().getAllRankItemByRankName(rankName)
                }
                withContext(Dispatchers.Main) {
                    rankAdapter.setDataList(result)
                }
            }
            addItemDecoration(object: RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(
                        0,
                        ITEM_MARGIN.dp.toInt(),
                        0,
                        ITEM_MARGIN.dp.toInt(),
                    )
                }
            })
        }
    }

    private fun initRankViews() {
        val apply : View.(RankItemRankType) -> Unit = { rankItemRankType ->
            val selected = StrokeTextDrawable(
                Color.argb(
                    rankItemRankType.a, rankItemRankType.r, rankItemRankType.g, rankItemRankType.b
            )).apply {
                setText(rankItemRankType.name)
                setStrokeColor(Color.WHITE, 4f.dp)
                setTextSize(40f.dp)
                setTextColor(Color.BLACK)
                setAutoAdjust(true, 12f.dp)
            }
            background = selected
        }
        with(binding) {
            rank1.apply(RankItemRankType.夯)
            rank2.apply(RankItemRankType.顶尖)
            rank3.apply(RankItemRankType.人上人)
            rank4.apply(RankItemRankType.NPC)
            rank5.apply(RankItemRankType.史)
        }
    }
}