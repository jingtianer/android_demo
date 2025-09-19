package com.jingtian.demoapp.main.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jingtian.demoapp.databinding.FragmentEmptyBinding

class EmptyFragment : BaseFragment() {

    lateinit var binding: FragmentEmptyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmptyBinding.inflate(layoutInflater, container, false)
        return binding.root
    }
}