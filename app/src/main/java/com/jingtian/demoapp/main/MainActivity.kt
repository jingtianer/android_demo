package com.jingtian.demoapp.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jingtian.demoapp.databinding.ActivityMainBinding
import com.jingtian.demoapp.main.fragments.BaseFragment
import com.jingtian.demoapp.main.fragments.OverDrawFragment
import com.jingtian.demoapp.main.fragments.RxMergeFragment
import com.jingtian.demoapp.main.fragments.RxJavaFragment
import com.jingtian.demoapp.main.fragments.RxZipFragment
import com.jingtian.demoapp.main.fragments.WidthAnimFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewPager2 by lazy { binding.viewpager2 }
    private val tabs by lazy { binding.tabs }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        val fragmentList: List<Lazy<BaseFragment>> = listOf(
            RxJavaFragment().lazy(),
            RxMergeFragment().lazy(),
            RxZipFragment().lazy(),
            OverDrawFragment().lazy(),
            WidthAnimFragment().lazy()
        )
        viewPager2.adapter = ReflectAdapter(this, fragmentList)
        viewPager2.visibility = View.VISIBLE
        TabLayoutMediator(tabs, viewPager2) { tab, position ->
            tab.text = fragmentList[position].value.getName()
        }.attach()
    }


    class ReflectAdapter(
        activity: FragmentActivity,
        private val dataList: List<Lazy<out Fragment>>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun createFragment(position: Int): Fragment {
            return dataList[position].value
        }
    }
}