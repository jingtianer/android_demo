package com.jingtian.demoapp.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.jingtian.demoapp.databinding.TabMainBinding

class MainTabItem(context: Context, tabName: String, tabDesc: String) {
    private val rootView = TabMainBinding.inflate(LayoutInflater.from(context))
    init {
        rootView.tabName.text = tabName
        if (tabDesc.isEmpty()) {
            rootView.tabDesc.visibility = View.GONE
        } else {
            rootView.tabDesc.visibility = View.VISIBLE
            rootView.tabDesc.text = tabDesc
        }
    }

    fun rootView(): View = rootView.root
}