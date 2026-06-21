package com.jingtian.demoapp.main.lissajous

import android.content.Context
import android.content.Intent
import com.jingtian.demoapp.main.TabActivity
import com.jingtian.demoapp.main.fragments.BaseFragment

class LisajousCurveActivity : TabActivity() {
    override fun onCreateFragmentList(): List<Triple<Class<out BaseFragment>, () -> BaseFragment, BaseFragmentDesc>> {
        return DefaultConfigs.defaultConfigs.map {
            Triple(
                LisajousCurveFragment::class.java,
                {
                    LisajousCurveFragment(
                        lisajousCurve = it.lisajousCurve.copy(),
                        fromMain = false,
                        speed = it.speed
                    )
                },
                BaseFragmentDesc(it.name, it.desc ?: "")
            )
        }
    }

    companion object {
        fun startLisajousCurveActivity(activity: Context) {
            activity.startActivity(Intent(activity, LisajousCurveActivity::class.java))
        }
    }
}