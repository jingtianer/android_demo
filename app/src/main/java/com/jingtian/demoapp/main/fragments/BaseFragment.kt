package com.jingtian.demoapp.main.fragments

import android.view.View
import androidx.fragment.app.Fragment
import com.jingtian.demoapp.main.ReflectClass
import com.jingtian.demoapp.main.ReflectObject

abstract class BaseFragment: Fragment() {
    companion object {
        fun <T : BaseFragment> lazy(clazz: Class<T>, args: Array<out Any>): Lazy<T> = lazy {
            ReflectClass(clazz).newInstance(args)!!
        }
    }

    private var tabView: View? = null

    fun setTabView(view: View?) {
        this.tabView = view
    }

    fun getTabView(): View? {
        return tabView
    }

    fun getName(): String {
        return this.javaClass.simpleName.removeSuffix("Fragment") + "\nFragment"
    }
}