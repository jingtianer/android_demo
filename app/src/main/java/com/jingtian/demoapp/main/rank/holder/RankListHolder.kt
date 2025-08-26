package com.jingtian.demoapp.main.rank.holder

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingtian.demoapp.databinding.ItemAddMoreBinding
import com.jingtian.demoapp.databinding.ItemRankListBinding
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.adapter.RankItemAdapter
import com.jingtian.demoapp.main.rank.dialog.AddRankItemDialog
import com.jingtian.demoapp.main.rank.dialog.JsonDialog
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankItem.Companion.isValid
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

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
        with(binding.more) {
            setOnClickListener {
                
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
            layoutParams?.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        (context as? BaseActivity)?.addDocumentPickerCallbacks(documentCallback)
    }

    override fun onBind(data: ModelRank, position: Int) {
        with(binding.title) {
            text = data.rankName
        }
        with(binding.recyclerView) {
            rankItemAdapter.setDataList(
                Utils.DataHolder.rankDB.rankItemDao().getAllRankModel(data.rankName)
            )
            rankItemHeaderFooterAdapter.bindRecyclerView(this, rankItemAdapter)
        }
        with(addMore.layoutAdd) {
            setOnClickListener {
                AddRankItemDialog(context, data.rankName, this@RankListHolder).show()
            }
        }
    }

    override fun onDetach() {
        (context as? BaseActivity)?.removeDocumentPickerCallbacks(documentCallback)
    }

    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem) {
        dialog.dismiss()
        if (modelRank.isValid()) {
            rankItemAdapter.append(modelRank)
            Utils.DataHolder.rankDB.rankItemDao().insert(modelRank)
        } else {
            Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPositiveClick(dialog: Dialog, json: String, import: Boolean) {
        dialog.dismiss()
        if (import) {
            Utils.DataHolder.toModelRankItemList(json)?.let {
                rankItemAdapter.appendAll(it)
                Utils.DataHolder.rankDB.rankItemDao().insertAll(it)
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }
}