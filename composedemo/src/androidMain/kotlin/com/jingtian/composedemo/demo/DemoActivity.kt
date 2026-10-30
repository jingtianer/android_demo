package com.jingtian.composedemo.demo

import androidx.compose.runtime.Composable
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.demonstration.DemonstrationMain

class DemoActivity : BaseActivity() {
    @Composable
    override fun Content() {
        DemonstrationMain()
    }
}