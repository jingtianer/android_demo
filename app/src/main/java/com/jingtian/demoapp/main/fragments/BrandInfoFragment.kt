package com.jingtian.demoapp.main.fragments

import android.os.Build
import android.os.Bundle
import android.view.View
import com.jingtian.demoapp.main.getStaticValue
import com.jingtian.demoapp.main.getStaticValueString
import java.lang.reflect.Modifier

class BrandInfoFragment : LogFragment(
        "BrandInfo",
        disableClearButton = true,
        reverse = false,
        stackFromEnd = false
    ) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        for (field in Build::class.java.declaredFields) {
            field.isAccessible = true
            addLog("${Modifier.toString(field.modifiers)} ${field.type.simpleName} ${field.name} = ${field.getStaticValueString(Build::class.java, field.type)}")
        }
    }
}