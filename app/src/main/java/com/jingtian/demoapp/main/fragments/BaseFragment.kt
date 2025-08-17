package com.jingtian.demoapp.main.fragments

import android.view.View
import androidx.fragment.app.Fragment
import com.jingtian.demoapp.main.ReflectClass

@BaseFragment.FragmentInfo
abstract class BaseFragment: Fragment() {
    companion object {
        fun <T : BaseFragment> creator(clazz: Class<T>, args: Array<out Any>): () -> T = {
            ReflectClass(clazz).newInstance(args)!!
        }

        fun <T : BaseFragment> Class<T>.getFragmentName(): String {
            return getAnnotation(FragmentInfo::class.java)?.name ?: simpleName.removeSuffix("Fragment")
        }

        interface BaseFragmentCallback {
            fun getTab(index: Int) : View?
        }

        const val KEY_TAB_INDEX = "KEY_TAB_INDEX"
    }

    fun getTabView(): View? {
        val index = arguments?.getInt(KEY_TAB_INDEX) ?: return null
        return (activity as? BaseFragmentCallback)?.getTab(index)
    }

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS)
    annotation class FragmentInfo(val name: String = "")

}