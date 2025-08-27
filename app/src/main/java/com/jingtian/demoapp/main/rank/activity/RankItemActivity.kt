package com.jingtian.demoapp.main.rank.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.ActivityRankItemBinding
import com.jingtian.demoapp.databinding.ItemAddCommentBinding
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.adapter.CommentListAdapter
import com.jingtian.demoapp.main.rank.dialog.AddCommentDialog
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.widget.RankTypeChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class RankItemActivity : BaseActivity(), AddCommentDialog.Companion.Callback {

    companion object  {
        fun startActivity(context: Context, rankName: String, rankItemName: String) {
            context.startActivity(Intent(context, RankItemActivity::class.java).apply {
                putExtra(KEY_RANK_NAME, rankName)
                putExtra(KEY_RANK_ITEM_NAME, rankItemName)
            })
        }

        private const val KEY_RANK_NAME = "RANK_NAME"
        private const val KEY_RANK_ITEM_NAME = "RANK_ITEM_NAME"
    }

    private lateinit var binding: ActivityRankItemBinding
    private var rankName: String = ""
    private var rankItemName: String = ""

    private lateinit var addCommentBinding: ItemAddCommentBinding
    private val commentAdapter = CommentListAdapter()
    private val commentHeaderFooterAdapter = BaseHeaderFooterAdapter<ModelItemComment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rankName = intent.getStringExtra(KEY_RANK_NAME) ?: ""
        rankItemName = intent.getStringExtra(KEY_RANK_ITEM_NAME) ?: ""

        if (rankName.isNotEmpty() && rankItemName.isNotEmpty()) {
            lifecycleScope.launch {
                val data = withContext(Dispatchers.IO) {
                    Utils.DataHolder.rankDB.rankItemDao().getRankItem(rankName, rankItemName)
                }.getOrNull(0) ?: return@launch
                withContext(Dispatchers.Main) {
                    with(binding.starRate) {
                        updateStarConfig(
                            false,
                            5,
                            3f.dp,
                            ResourcesCompat.getDrawable(resources, R.drawable.star_high_lighted, null),
                            ResourcesCompat.getDrawable(resources, R.drawable.star, null),
                        )
                        setScore(data.score)
                    }
                    with(binding.rankType) {
                        val bg = RankTypeChooser.createBg(data.rankType)
                        binding.rankType.layoutParams.apply {
                            width = bg.getWidth().toInt()
                            height = bg.getHeight().toInt()
                        }
                        background = bg
                    }
                    with(binding.title) {
                        text = data.itemName
                    }
                    with(binding.image) {
                        setImageURI(data.image.image)
                    }
                    with(binding.score) {
                        text = String.format("%.2f分", binding.starRate.getScore())
                    }
                }
            }
        }

        with(binding.root) {
            setInnerRecyclerView(binding.recyclerView)
        }

        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            commentHeaderFooterAdapter.bindRecyclerView(this, commentAdapter)
            addCommentBinding = ItemAddCommentBinding.inflate(layoutInflater, this, false)
            commentHeaderFooterAdapter.addFooter(addCommentBinding.root)
            with(addCommentBinding.root) {
                setOnClickListener {
                    AddCommentDialog(this@RankItemActivity, this@RankItemActivity).show()
                }
            }
            lifecycleScope.launch {
                val data = withContext(Dispatchers.IO) {
                    Utils.DataHolder.rankDB.rankCommentDao().getAllComment(rankItemName)
                }
                withContext(Dispatchers.Main) {
                    commentAdapter.setDataList(data)
                }
            }
        }
    }

    override fun onPositiveClick(dialog: Dialog, comment: String) {
        dialog.dismiss()
        lifecycleScope.launch {
            val model = withContext(Dispatchers.IO) {
                val model = ModelItemComment(
                    itemName = rankItemName,
                    comment = comment,
                    creationDate = Date(),
                    lastModifyDate = Date()
                )
                Utils.DataHolder.rankDB.rankCommentDao().insert(model)
                model
            }
            withContext(Dispatchers.Main) {
                commentAdapter.append(model)
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }
}