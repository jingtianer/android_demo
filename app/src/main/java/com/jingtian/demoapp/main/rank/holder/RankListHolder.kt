package com.jingtian.demoapp.main.rank.holder

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingtian.demoapp.databinding.ItemAddMoreBinding
import com.jingtian.demoapp.databinding.ItemRankListBinding
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.adapter.RankItemAdapter
import com.jingtian.demoapp.main.rank.dialog.AddRankItemDialog
import com.jingtian.demoapp.main.rank.dialog.JsonDialog
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankItem.Companion.isValid

class RankListHolder private constructor(private val binding: ItemRankListBinding) : BaseViewHolder<ModelRank>(binding.root), AddRankItemDialog.Companion.Callback, JsonDialog.Companion.Callback {
    companion object {
        fun create(parent: ViewGroup): RankListHolder {
            return RankListHolder(ItemRankListBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    private val context = binding.root.context

    private val rankItemAdapter = RankItemAdapter()
    private var addMore: ItemAddMoreBinding
    private val rankItemHeaderFooterAdapter = BaseHeaderFooterAdapter<ModelRankItem>()


    init {
        binding.recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        addMore = ItemAddMoreBinding.inflate(LayoutInflater.from(context), binding.recyclerView, false)
        rankItemHeaderFooterAdapter.addFooter(addMore.root)
        with(addMore.layoutAdd) {
            setOnClickListener {
                AddRankItemDialog(context, this@RankListHolder).show()
            }
        }
        with(addMore.layoutImport) {
            setOnClickListener {
                JsonDialog(context, this@RankListHolder, true).apply {
                    show()
                }
            }
        }
        with(addMore.layoutExport) {
            setOnClickListener {
                JsonDialog(context, this@RankListHolder, false).apply {
                    setJson(Utils.DataHolder.toJson(rankItemAdapter.getDataList()))
                    show()
                }
            }
        }
        with(addMore.root) {
            layoutParams?.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    override fun onBind(data: ModelRank, position: Int) {
        with(binding.title) {
            text = data.name
        }
        with(binding.recyclerView) {
            rankItemAdapter.setDataList(data.list)
            rankItemHeaderFooterAdapter.bindRecyclerView(this, rankItemAdapter)
        }
    }

    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem) {
        dialog.dismiss()
        if (modelRank.isValid()) {
            rankItemAdapter.append(modelRank)
            currentData?.list?.add(modelRank)
            Utils.DataHolder.rankDataStore = Utils.DataHolder.rankDataStore
        } else {
            Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPositiveClick(dialog: Dialog, json: String, import: Boolean) {
        dialog.dismiss()
        if (import) {
            Utils.DataHolder.toModelRankItemList(json)?.let {
                rankItemAdapter.appendAll(it)
                currentData?.list?.addAll(it)
                Utils.DataHolder.rankDataStore = Utils.DataHolder.rankDataStore
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }
}