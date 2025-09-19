package com.jingtian.demoapp.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jingtian.demoapp.databinding.ActivityMainBinding
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.fragments.BaseFragment
import com.jingtian.demoapp.main.fragments.BaseFragment.Companion.KEY_TAB_INDEX
import com.jingtian.demoapp.main.fragments.BaseFragment.Companion.getFragmentName
import com.jingtian.demoapp.main.fragments.BaseFragment.Companion.BaseFragmentCallback
import com.jingtian.demoapp.main.fragments.BuildInfoFragment
import com.jingtian.demoapp.main.fragments.CanvasFragment
import com.jingtian.demoapp.main.fragments.ConcatAdapterFragment
import com.jingtian.demoapp.main.fragments.SpannableStringFragment
import com.jingtian.demoapp.main.fragments.FocusFragment
import com.jingtian.demoapp.main.fragments.FollowLeftFragment
import com.jingtian.demoapp.main.fragments.FragmentInfoFragment
import com.jingtian.demoapp.main.fragments.NBPlusTextFragment
import com.jingtian.demoapp.main.fragments.NestedScrollFragment
import com.jingtian.demoapp.main.fragments.OverDrawFragment
import com.jingtian.demoapp.main.fragments.RxJavaFragment
import com.jingtian.demoapp.main.fragments.RxMergeFragment
import com.jingtian.demoapp.main.fragments.RxZipFragment
import com.jingtian.demoapp.main.fragments.TextOnFingerFragment
import com.jingtian.demoapp.main.fragments.WidthAnimFragment
import com.jingtian.demoapp.main.rank.fragment.RankFragment

class MainActivity : BaseActivity(), BaseFragmentCallback {

    companion object {

        private val fragmentList = listOf(
            NBPlusTextFragment::class.java to arrayOf<Any>(),
            TextOnFingerFragment::class.java to arrayOf<Any>(),
            RankFragment::class.java to arrayOf<Any>(),
            FocusFragment::class.java to arrayOf<Any>(),
            NestedScrollFragment::class.java to arrayOf<Any>(),
            ConcatAdapterFragment::class.java to arrayOf<Any>(),
            CanvasFragment::class.java to arrayOf<Any>(),
            FollowLeftFragment::class.java to arrayOf<Any>(),
            RxJavaFragment::class.java to arrayOf<Any>(),
            RxMergeFragment::class.java to arrayOf<Any>(),
            RxZipFragment::class.java to arrayOf<Any>(),
            BuildInfoFragment::class.java to arrayOf<Any>(),
            FragmentInfoFragment::class.java to arrayOf<Any>(),
            OverDrawFragment::class.java to arrayOf<Any>(),
            WidthAnimFragment::class.java to arrayOf<Any>(),
            SpannableStringFragment::class.java to arrayOf<Any>(),
        )

        fun Activity.backToMain(tabIndex: Class<out BaseFragment>? = null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            for ((i, clazz) in fragmentList.withIndex()) {
                if (clazz.first.equals(tabIndex)) {
                    intent.putExtra(KEY_TAB_INDEX, i)
                    break
                }
            }
            this.startActivity(intent)
            this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private val viewPager2 get() = binding.viewpager2
    private val tabs get() =  binding.tabs

    private val fragmentCreator = fragmentList.map { (clazz, args)->
        BaseFragment.creator(clazz, args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        viewPager2.adapter = ReflectAdapter(this, fragmentCreator)
        viewPager2.visibility = View.VISIBLE
        TabLayoutMediator(tabs, viewPager2) { tab, position ->
            tab.text = fragmentList[position].first.getFragmentName()
        }.attach()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val targetTab = intent.getIntExtra(KEY_TAB_INDEX, 0) ?: 0
        viewPager2.setCurrentItem(targetTab, false)
    }

    inner class ReflectAdapter(
        activity: FragmentActivity,
        private val dataList: List<() -> BaseFragment>
    ) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int {
            return dataList.size
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = dataList[position].invoke()
            fragment.arguments = Bundle().apply {
                putInt(KEY_TAB_INDEX, position)
            }
            return fragment
        }
    }

    override fun getTab(index: Int): View? {
        return tabs.getTabAt(index)?.view
    }
}