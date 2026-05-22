package com.jingtian.demoapp.main.footcurve

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.jingtian.demoapp.main.TabActivity
import com.jingtian.demoapp.main.fragments.BaseFragment

class FootCurveActivity : TabActivity() {
    override fun onCreateFragmentList(): List<Triple<Class<out BaseFragment>, ()->BaseFragment, BaseFragmentDesc>> {
        return DefaultConfigs.defaultConfigs.map {
            Triple(FootCurveFragment::class.java, {FootCurveFragment(it.curve.copy(), false)}, BaseFragmentDesc(it.name, it.desc ?: ""))
        }
    }

    companion object {
        fun startFootCurveActivity(activity: Context) {
            activity.startActivity(Intent(activity, FootCurveActivity::class.java))
        }
    }
}