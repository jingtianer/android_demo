package com.jingtian.demoapp.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.jingtian.demoapp.main.fragments.BrandInfoFragment
import com.jingtian.demoapp.main.fragments.FocusFragment
import com.jingtian.demoapp.main.fragments.NBPlusTextFragment
import com.jingtian.demoapp.main.fragments.OverDrawFragment
import com.jingtian.demoapp.main.fragments.RxJavaFragment
import com.jingtian.demoapp.main.fragments.RxMergeFragment
import com.jingtian.demoapp.main.fragments.RxZipFragment
import com.jingtian.demoapp.main.fragments.WidthAnimFragment

class MainActivity : AppCompatActivity() {

    companion object {
        fun Activity.backToMain(tabIndex: Class<out BaseFragment>? = null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(TAB_INDEX, tabIndex)
            this.startActivity(intent)
            this.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        private const val TAB_INDEX = "TAB_INDEX"
    }

    private lateinit var binding: ActivityMainBinding
    private val viewPager2 by lazy { binding.viewpager2 }
    private val tabs by lazy { binding.tabs }

    private val fragmentList: List<Lazy<BaseFragment>> = listOf(
        NBPlusTextFragment().lazy(),
        FocusFragment().lazy(),
        RxJavaFragment().lazy(),
        RxMergeFragment().lazy(),
        RxZipFragment().lazy(),
        BrandInfoFragment().lazy(),
        OverDrawFragment().lazy(),
        WidthAnimFragment().lazy()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        viewPager2.adapter = ReflectAdapter(this, fragmentList)
        viewPager2.visibility = View.VISIBLE
        TabLayoutMediator(tabs, viewPager2) { tab, position ->
            tab.text = fragmentList[position].value.getName()
        }.attach()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val targetTab = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(TAB_INDEX, Class::class.java)
        } else {
            intent?.getSerializableExtra(TAB_INDEX) as? Class<BaseFragment>
        }
        if (targetTab != null) {
            for ((index, fragment) in fragmentList.withIndex()) {
                if (targetTab.isInstance(fragment)) {
                    viewPager2.setCurrentItem(index, false)
                }
            }
        }
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