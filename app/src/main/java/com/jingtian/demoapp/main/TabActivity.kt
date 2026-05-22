package com.jingtian.demoapp.main

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import com.jingtian.demoapp.main.fragments.BaseFragment.Companion.getFragmentDesc

open class TabActivity : BaseActivity(), BaseFragmentCallback {

    class BaseFragmentDesc(
        val name: CharSequence,
        val desc: CharSequence? = null
    )

    private lateinit var binding: ActivityMainBinding
    private val viewPager2 get() = binding.viewpager2
    private val tabs get() =  binding.tabs


    protected open fun onCreateFragmentList(): List<Triple<Class<out BaseFragment>, ()->BaseFragment, BaseFragmentDesc>> {
        return listOf()
    }

    private val fragmentList by lazy {
        onCreateFragmentList()
    }

    companion object {
        inline fun <reified T : TabActivity> Activity.backToTab(tabIndex: Class<out BaseFragment>? = null) {
            val intent = Intent(this, T::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(KEY_TAB_INDEX, tabIndex)
            this.startActivity(intent)
            this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        viewPager2.adapter = ReflectAdapter(this, fragmentList.map { (clazz, creator, _)->
            creator
        })
        viewPager2.visibility = View.VISIBLE
        TabLayoutMediator(tabs, viewPager2) { tab, position ->
            val fragment = fragmentList[position]
            tab.customView = MainTabItem(this@TabActivity, fragment.third.name.takeIf { it.isNotEmpty() } ?: fragment.first.getFragmentName(), fragment.third.desc ?: fragment.first.getFragmentDesc()).rootView()
        }.attach()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val targetTab = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(KEY_TAB_INDEX, Class::class.java)
        } else {
            intent.getSerializableExtra(KEY_TAB_INDEX) as Class<BaseFragment>
        }
        viewPager2.setCurrentItem(fragmentList.withIndex().find { it.value.first.equals(targetTab) }?.index ?: 0, false)
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