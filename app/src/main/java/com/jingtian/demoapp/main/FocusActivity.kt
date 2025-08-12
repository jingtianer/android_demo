package com.jingtian.demoapp.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.ActivityFocusBinding
import com.jingtian.demoapp.main.MainActivity.Companion.backToMain
import com.jingtian.demoapp.main.fragments.FocusFragment

class FocusActivity : AppCompatActivity() {

    companion object {
        fun startActivity(
            context: Context,
            requestFocus: Boolean,
            softInputMode: Int,
            textGravity: Int,
            etFocusable: Int?,
            etFocusableOnTouchMode: Boolean?,
            config: Bundle,
        ) {
            val intent = Intent(context, FocusActivity::class.java)
            intent.putExtra(Params.REQUEST_FOCUS.name, requestFocus)
            intent.putExtra(Params.SOFT_INPUT_MODE.name, softInputMode)
            intent.putExtra(Params.TEXT_GRAVITY.name, textGravity)
            intent.putExtra(Params.FOCUS_CONFIG.name, config)
            etFocusable?.let { intent.putExtra(Params.ET_FLAG_FOCUSABLE.name, it) }
            etFocusableOnTouchMode?.let { intent.putExtra(Params.ET_FLAG_FOCUSABLE_ON_TOUCH_MODE.name, etFocusableOnTouchMode) }
            context.startActivity(intent)
        }

        enum class Params {
            REQUEST_FOCUS,
            SOFT_INPUT_MODE,
            TEXT_GRAVITY,
            FOCUS_CONFIG,
            ET_FLAG_FOCUSABLE,
            ET_FLAG_FOCUSABLE_ON_TOUCH_MODE,
        }
    }

    private lateinit var binding: ActivityFocusBinding

    private val onBackPressedHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
            backToMain(FocusFragment::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        intent?.let { intent ->
            val gravity = intent.getIntExtra(
                Params.TEXT_GRAVITY.name,
                FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY
            )
            val container = binding.container
            val et = binding.et
            when (gravity) {
                Gravity.BOTTOM -> {
                    binding.root.removeAllViews()
                    binding.root.addView(
                        container,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            weight = 1f
                        })
                    binding.root.addView(
                        et,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )
                }

                else -> {
                    binding.root.removeAllViews()
                    binding.root.addView(
                        et,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )
                    binding.root.addView(
                        container,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            weight = 1f
                        })
                }
            }
            if (intent.getBooleanExtra(Params.REQUEST_FOCUS.name, false)) {
                binding.et.requestFocus()
            }
            val softInputMode = intent.getIntExtra(Params.SOFT_INPUT_MODE.name, 0)
            window.setSoftInputMode(softInputMode)
            if (intent.hasExtra(Params.ET_FLAG_FOCUSABLE.name)) {
                val focusable = intent.getIntExtra(Params.ET_FLAG_FOCUSABLE.name, 0)
                et.focusable = focusable
            }
            if (intent.hasExtra(Params.ET_FLAG_FOCUSABLE_ON_TOUCH_MODE.name)) {
                val focusableOnTouchMode = intent.getBooleanExtra(Params.ET_FLAG_FOCUSABLE_ON_TOUCH_MODE.name, false)
                et.isFocusableInTouchMode = focusableOnTouchMode
            }
        }
        supportFragmentManager.beginTransaction().add(R.id.container, FocusFragment().apply {
            arguments = intent.getBundleExtra(Params.FOCUS_CONFIG.name)
        }).commit()
        onBackPressedDispatcher.addCallback(this, onBackPressedHandler)
    }
}