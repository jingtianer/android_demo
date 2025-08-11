package com.jingtian.demoapp.main.fragments

import androidx.fragment.app.Fragment

abstract class BaseFragment: Fragment() {
    fun lazy(): Lazy<BaseFragment> = lazy { this }
    fun getName(): String {
        return this.javaClass.simpleName.removeSuffix("Fragment") + "\nFragment"
    }
}