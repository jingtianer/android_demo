package com.jingtian.demoapp.main.rank.holder

import android.app.Dialog
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.ItemAddMoreBinding
import com.jingtian.demoapp.databinding.ItemRankListBinding
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.getBaseActivity
import com.jingtian.demoapp.main.mergeSortedListsByDescending
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.Utils.CoroutineUtils.activityLifecycleLaunch
import com.jingtian.demoapp.main.rank.activity.RankActivity
import com.jingtian.demoapp.main.rank.adapter.RankItemAdapter
import com.jingtian.demoapp.main.rank.dialog.AddRankItemDialog
import com.jingtian.demoapp.main.rank.dialog.AlertDialog
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankItem.Companion.isValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RankListHolder private constructor(private val binding: ItemRankListBinding) :
    BaseViewHolder<ModelRank>(binding.root), AddRankItemDialog.Companion.Callback {
    companion object {
        fun create(parent: ViewGroup): RankListHolder {
            return RankListHolder(
                ItemRankListBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }

    private val context = binding.root.context

    private val rankItemAdapter = RankItemAdapter()
    private var addMore: ItemAddMoreBinding
    private val rankItemHeaderFooterAdapter = BaseHeaderFooterAdapter<ModelRankItem>()
    private val documentCallback = object : BaseActivity.Companion.DocumentPickerCallback {
        override fun onDocumentCallback(uri: Uri) {
            Utils.CoroutineUtils.runIOTask({
                val list = Utils.Share.readShareRankItemList(uri)
                val success = Utils.DataHolder.rankDB.rankItemDao().insertAll(list).map { it != -1L }
                list.filterIndexed { index, _ ->
                    success[index]
                }.sortedByDescending  { it.score }
            }) { list->
                lifecycleScope.launch {
                    rankItemAdapter.setDataList(mergeSortedListsByDescending(list, rankItemAdapter.getDataList()) {
                        it.score
                    })
                }
            }

        }
    }
    
    private var exportingData = false

    init {
        binding.recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        addMore =
            ItemAddMoreBinding.inflate(LayoutInflater.from(context), binding.recyclerView, false)
        rankItemHeaderFooterAdapter.addFooter(addMore.root)
        with(addMore.layoutImport) {
            setOnClickListener {
                context.getBaseActivity()?.pickFile(arrayOf("*/*"), null, documentCallback)
            }
        }
        with(addMore.layoutExport) {
            setOnClickListener {
                if (exportingData) {
                    Toast.makeText(context, "别催啦，正在努力导出啦！~~", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                exportingData = true
                lifecycleScope.launch {
                    val uri = withContext(Dispatchers.IO) {
                        Utils.Share.startShare(
                            currentData?.rankName ?: "",
                            rankItemAdapter.getDataList()
                        )
                    }
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "*/*"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, ""))
                    exportingData = false
                }
            }
        }
        with(addMore.root) {
            layoutParams.height = 300f.dp.toInt()
            layoutParams.width = RecyclerView.LayoutParams.WRAP_CONTENT
        }
        with(binding.recyclerView) {
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(
                        6f.dp.toInt(),
                        0,
                        6f.dp.toInt(),
                        0,
                    )
                }
            })
        }
    }

    private val deleteDialogCallback = object : AlertDialog.Companion.Callback {
        override fun onPositiveClick(dialog: Dialog) {
            dialog.dismiss()
            currentData?.let {
                Utils.CoroutineUtils.runIOTask({
                    Utils.DataHolder.rankDB.deleteRank(it)
                }) {
                    context.activityLifecycleLaunch {
                        // 不能用自己的lifecycle，因为自己要被删除
                        currentAdapter?.remove(currentPosition)
                    }
                }
            }
        }

        override fun onNegativeClick(dialog: Dialog) {
            dialog.dismiss()
        }

    }

    override fun onBind(data: ModelRank, position: Int) {
        with(binding.title) {
            text = data.rankName
        }
        rankItemAdapter.setDataList(listOf())
        with(binding.recyclerView) {
            lifecycleScope.launch {
                val list = withContext(Dispatchers.IO) {
                    Utils.DataHolder.rankDB.rankItemDao().getAllRankItemByRankName(data.rankName)
                }
                rankItemAdapter.setDataList(list)
            }
            rankItemHeaderFooterAdapter.bindRecyclerView(this, rankItemAdapter)
        }
        with(addMore.layoutAdd) {
            setOnClickListener {
                AddRankItemDialog(context, data.rankName, this@RankListHolder).show()
            }
        }
        with(addMore.layoutDelete) {
            setOnClickListener {
                AlertDialog(context, deleteDialogCallback, "确认删除排行榜：${data.rankName}").show()
            }
        }
        with(binding.more) {
            setOnClickListener {
                RankActivity.startActivity(context, data.rankName)
            }
        }

        with(binding.moreIcon) {
            setOnClickListener {
                RankActivity.startActivity(context, data.rankName)
            }
        }
    }

    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem) {
        dialog.dismiss()
        if (modelRank.isValid()) {
            Utils.CoroutineUtils.runIOTask({
                Utils.DataHolder.rankDB.rankItemDao().insert(modelRank) != -1L
            }) { success->
                if (success) {
                    lifecycleScope.launch {
                        val insertPos = withContext(Dispatchers.Default) {
                            val insertPos = rankItemAdapter.getDataList().binarySearch {
                                if (it.score > modelRank.score) {
                                    -1
                                } else if (it.score < modelRank.score) {
                                    1
                                } else {
                                    0
                                }
                            }
                            if (insertPos < 0) {
                                - insertPos - 1
                            } else {
                                insertPos
                            }
                        }
                        rankItemAdapter.insert(insertPos, modelRank)
                    }
                } else {
                    Toast.makeText(context, "添加失败，${modelRank.itemName}已存在", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }

    override fun onDeleteClick(dialog: Dialog) {
        dialog.dismiss()
    }
}