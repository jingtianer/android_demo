package com.jingtian.demoapp.main.rank.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.databinding.FragmentRankBinding
import com.jingtian.demoapp.main.fragments.BaseFragment
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.adapter.RankListAdapter

class RankFragment : BaseFragment() {

    private lateinit var binding: FragmentRankBinding
    private val rankListAdapter = RankListAdapter()

    init {
        rankListAdapter.setDataList(Utils.DataHolder.fakeData)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRankBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = rankListAdapter
        }
    }
}