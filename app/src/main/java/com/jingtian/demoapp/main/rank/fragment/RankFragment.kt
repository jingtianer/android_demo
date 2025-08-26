package com.jingtian.demoapp.main.rank.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.FragmentRankBinding
import com.jingtian.demoapp.databinding.ItemAddMoreBinding
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.fragments.BaseFragment
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.adapter.RankListAdapter
import com.jingtian.demoapp.main.rank.dialog.AddRankDialog
import com.jingtian.demoapp.main.rank.dialog.JsonDialog
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRank.Companion.isValid

class RankFragment : BaseFragment(), AddRankDialog.Companion.Callback, JsonDialog.Companion.Callback {

    private lateinit var binding: FragmentRankBinding
    private val rankListAdapter = RankListAdapter()
    private val headerFooterAdapter = BaseHeaderFooterAdapter<ModelRank>()
    private lateinit var addMore:ItemAddMoreBinding


    init {
        rankListAdapter.setDataList(Utils.DataHolder.rankDataStore)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRankBinding.inflate(layoutInflater, container, false)
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            addMore = ItemAddMoreBinding.inflate(LayoutInflater.from(context), binding.recyclerView, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            headerFooterAdapter.addFooter(addMore.root)
            headerFooterAdapter.bindRecyclerView(this, rankListAdapter)
        }
        with(addMore.root) {
            orientation = LinearLayout.HORIZONTAL
            layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        with(addMore.text) {
            text = "添加更多排行榜"
        }
        with(addMore.layoutAdd) {
            setOnClickListener {
                AddRankDialog(context, this@RankFragment).show()
            }
        }
        with(addMore.layoutImport) {
            setOnClickListener {
                JsonDialog(context, this@RankFragment, true).show()
            }
        }
        with(addMore.layoutExport) {
            setOnClickListener {
                JsonDialog(context, this@RankFragment).apply {
                    setJson(Utils.DataHolder.toJson(rankListAdapter.getDataList()))
                    show()
                }
            }
        }
    }
    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRank) {
        dialog.dismiss()
        if (modelRank.isValid()) {
            rankListAdapter.append(modelRank)
            Utils.DataHolder.rankDataStore = rankListAdapter.getDataList().toMutableList()
        } else {
            Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPositiveClick(dialog: Dialog, json: String, import: Boolean) {
        dialog.dismiss()
        if (import) {
            Utils.DataHolder.toModelRankList(json)?.let {
                rankListAdapter.appendAll(it)
                Utils.DataHolder.rankDataStore = rankListAdapter.getDataList().toMutableList()
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }
}