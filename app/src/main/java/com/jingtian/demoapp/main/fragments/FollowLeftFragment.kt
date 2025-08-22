package com.jingtian.demoapp.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jingtian.demoapp.databinding.FragmentFollowLeftBinding

class FollowLeftFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentFollowLeftBinding.inflate(inflater).root
    }

}