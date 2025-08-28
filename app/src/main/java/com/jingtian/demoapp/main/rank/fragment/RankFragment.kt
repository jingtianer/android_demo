package com.jingtian.demoapp.main.rank.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.FragmentRankBinding
import com.jingtian.demoapp.databinding.ItemAddMoreBinding
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.fragments.BaseFragment
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.Utils.DataHolder.rankDB
import com.jingtian.demoapp.main.rank.adapter.RankListAdapter
import com.jingtian.demoapp.main.rank.dialog.AddRankDialog
import com.jingtian.demoapp.main.rank.dialog.AddUserDialog
import com.jingtian.demoapp.main.rank.dialog.JsonDialog
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRank.Companion.isValid
import com.jingtian.demoapp.main.rank.model.ModelRankUser
import com.jingtian.demoapp.main.rank.model.ModelRankUser.Companion.getUserNameOrDefault
import com.jingtian.demoapp.main.rank.model.ModelRankUser.Companion.loadUserImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RankFragment : BaseFragment(), AddRankDialog.Companion.Callback, JsonDialog.Companion.Callback {

    private lateinit var binding: FragmentRankBinding
    private val rankListAdapter = RankListAdapter()
    private val headerFooterAdapter = BaseHeaderFooterAdapter<ModelRank>()
    private lateinit var addMore:ItemAddMoreBinding


    private var user: ModelRankUser? = null

    private val addUserCallback = object :AddUserDialog.Companion.Callback {
        override fun onPositiveClick(dialog: Dialog, modelRankUser: ModelRankUser) {
            modelRankUser.let {
                user = it
                it.loadUserImage(binding.avatar, 30f.dp.toInt(), 30f.dp.toInt())
                binding.userName.text = it.userName
            }
        }

        override fun onNegativeClick(dialog: Dialog) {
        }

        override fun onDeleteClick(dialog: Dialog) {
            user = null
            user.loadUserImage(binding.avatar, 30f.dp.toInt(), 30f.dp.toInt())
            binding.userName.text = user.getUserNameOrDefault()
        }
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
        lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                rankDB.rankListDao().getAllRankModel()
            }
            rankListAdapter.setDataList(list)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            user = withContext(Dispatchers.IO) {
                ModelRankUser.getUserInfo()
            }
            user.loadUserImage(binding.avatar, 30f.dp.toInt(), 30f.dp.toInt())
            binding.userName.text = user.getUserNameOrDefault()
            with(binding.root) {
                binding.avatar.setOnClickListener {
                    AddUserDialog(context, addUserCallback, user).show()
                }
                binding.userName.setOnClickListener {
                    AddUserDialog(context, addUserCallback, user).show()
                }
            }
        }
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
        with(addMore.layoutDelete) {
            visibility = View.GONE
        }
        with(addMore.layoutExport) {
            setOnClickListener {
                JsonDialog(context, this@RankFragment).apply {
                    lifecycleScope.launch {
                        val json = withContext(Dispatchers.IO) {
                            Utils.DataHolder.modelRank2Json(rankListAdapter.getDataList())
                        }
                        setJson(json)
                        show()
                    }
                }
            }
        }
    }
    override fun onPositiveClick(dialog: Dialog, modelRank: ModelRank) {
        dialog.dismiss()
        if (modelRank.isValid()) {
            Utils.CoroutineUtils.runIOTask({
                Utils.DataHolder.rankDB.rankListDao().insert(modelRank) != -1L
            }) { success->
                if (success) {
                    lifecycleScope.launch {
                        rankListAdapter.append(modelRank)
                    }
                } else {
                    Toast.makeText(context, "添加失败，${modelRank.rankName}已存在", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPositiveClick(dialog: Dialog, json: String, import: Boolean) {
        dialog.dismiss()
        if (import) {
            Utils.CoroutineUtils.runIOTask({
                Utils.DataHolder.toModelRankList(json)
            }) { list->
                list?.let {
                    Utils.CoroutineUtils.runIOTask({
                        Utils.DataHolder.rankDB.rankListDao().insertAll(it).map { it != -1L }
                    }) { success->
                        lifecycleScope.launch {
                            rankListAdapter.appendAll(it.filterIndexed { index, _ ->
                                success[index]
                            })
                        }
                    }
                }
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }
}