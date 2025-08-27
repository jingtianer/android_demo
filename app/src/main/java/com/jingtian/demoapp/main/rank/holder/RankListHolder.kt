package com.jingtian.demoapp.main.rank.holder

import android.app.Dialog
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.ItemAddMoreBinding
import com.jingtian.demoapp.databinding.ItemRankListBinding
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.Utils.CoroutineUtils.lifecycleLaunch
import com.jingtian.demoapp.main.rank.activity.RankActivity
import com.jingtian.demoapp.main.rank.adapter.RankItemAdapter
import com.jingtian.demoapp.main.rank.dialog.AddRankItemDialog
import com.jingtian.demoapp.main.rank.dialog.AlertDialog
import com.jingtian.demoapp.main.rank.dialog.JsonDialog
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankItem.Companion.isValid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RankListHolder private constructor(private val binding: ItemRankListBinding) :
    BaseViewHolder<ModelRank>(binding.root), AddRankItemDialog.Companion.Callback,
    JsonDialog.Companion.Callback {
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
            val ignore = Utils.Share.readShareRankItemList(uri).subscribe {
                rankItemAdapter.appendAll(it)
            }
        }
    }

    init {
        binding.recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        addMore =
            ItemAddMoreBinding.inflate(LayoutInflater.from(context), binding.recyclerView, false)
        rankItemHeaderFooterAdapter.addFooter(addMore.root)
        with(addMore.layoutImport) {
            setOnClickListener {
                (context as? BaseActivity)?.pickFile?.launch(arrayOf("*/*"))
            }
        }
        with(addMore.layoutExport) {
            setOnClickListener {
                val ignore = Utils.Share.startShare(
                    currentData?.rankName ?: "",
                    rankItemAdapter.getDataList()
                ).subscribe {
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, it)
                        type = "*/*"
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
            }
        }
        with(addMore.root) {
            layoutParams.height = 300f.dp.toInt()
            layoutParams.width = RecyclerView.LayoutParams.WRAP_CONTENT
        }
        (context as? BaseActivity)?.addDocumentPickerCallbacks(documentCallback)
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
                    currentAdapter?.remove(currentPosition)
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
        with(binding.recyclerView) {
            context.lifecycleLaunch {
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

    override fun onDetach() {
        (context as? BaseActivity)?.removeDocumentPickerCallbacks(documentCallback)
    }

    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem) {
        dialog.dismiss()
        if (modelRank.isValid()) {
            Utils.CoroutineUtils.runIOTask({
                Utils.DataHolder.rankDB.rankItemDao().insert(modelRank)
            }) {}
            context.lifecycleLaunch {
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
            Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPositiveClick(dialog: Dialog, json: String, import: Boolean) {
        dialog.dismiss()
        if (import) {
            Utils.CoroutineUtils.runIOTask({
                Utils.DataHolder.toModelRankItemList(json)
            }) { list->
                list?.let {
                    rankItemAdapter.appendAll(it)
                    Utils.CoroutineUtils.runIOTask({
                        Utils.DataHolder.rankDB.rankItemDao().insertAll(it)
                    }) {}
                }
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }

    override fun onDeleteClick(dialog: Dialog) {
        dialog.dismiss()
    }
}