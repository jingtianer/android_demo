package com.jingtian.demoapp.main.fragments

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.jingtian.demoapp.main.Reflect

open class ReflectFragment(private val obj: Any, title: String) : LogFragment(
    title,
    disableClearButton = true,
    reverse = false,
    stackFromEnd = false
) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when(obj) {
            is Class<*> -> {
                addLog(Reflect.create(obj).toString())
            }
            else -> {
                addLog(Reflect.create(obj).toString())
            }
        }
    }
}

class BuildInfoFragment : ReflectFragment(Build::class.java, Build::class.java.name)

class FragmentInfoFragment : ReflectFragment(Fragment(), Fragment::class.java.name)