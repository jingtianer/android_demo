package com.jingtian.demoapp.main

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.jingtian.demoapp.main.base.BaseActivity

class DemoApplication : Application() {
    var activityStack = HashMap<Context, BaseActivity>()
    init {
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is BaseActivity) {
                    activityStack[activity] = activity
                }
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                activityStack.remove(activity)
            }

        })
    }
}