package com.jingtian.composedemo

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.app.ComponentActivity
import com.jingtian.composedemo.demo.DemoActivity
import com.jingtian.composedemo.main.MainActivity

class LaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = if (BuildKonfig.isDemo) {
            DemoActivity::class.java
        } else {
            MainActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }
}