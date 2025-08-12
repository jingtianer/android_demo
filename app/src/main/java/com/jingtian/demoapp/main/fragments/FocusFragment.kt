package com.jingtian.demoapp.main.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.FragmentFocusBinding
import com.jingtian.demoapp.main.FocusActivity

class FocusFragment : BaseFragment() {

    private lateinit var binding: FragmentFocusBinding

    companion object {
        enum class Config {
            RequestFocus,
            SoftInputMode,
            Gravity,
            FOCUSABLE,
            FOCUSABLE_IN_TOUCH_MODE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun shouldRequestFocus(): Boolean {
        return when(binding.requestFocusGp.checkedRadioButtonId) {
            R.id.request_focus_yes -> {
                true
            }
            R.id.request_focus_no -> {
                false
            }
            else -> {
                false
            }
        }
    }

    private fun getSoftInputMode(): Int {
        val state =  when(binding.stateGp.checkedRadioButtonId) {
            R.id.stateHidden -> {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            }
            R.id.stateVisible -> {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            }
            R.id.stateUnchanged -> {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
            }
            R.id.stateUnspecified -> {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
            }
            R.id.stateAlwaysHidden -> {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            }
            R.id.stateAlwaysVisible -> {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            }
            else -> {
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
            }
        }

        val adjust = when(binding.adjustGp.checkedRadioButtonId) {
            R.id.adjust_pan -> {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            }
            R.id.adjust_nothing -> {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            }
            R.id.adjust_resize -> {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
            R.id.adjust_unspecified -> {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED
            }
            else -> {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED
            }
        }
        return state or adjust
    }

    private fun getGravity(): Int {
        return when(binding.gravityGp.checkedRadioButtonId) {
            R.id.gravity_top -> {
                Gravity.TOP
            }
            R.id.gravity_bottom -> {
                Gravity.BOTTOM
            }
            else -> {
                FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY
            }
        }
    }

    private fun getArgument(): Bundle {
        return Bundle().apply {
            putBoolean(Config.RequestFocus.name, shouldRequestFocus())
            putInt(Config.SoftInputMode.name, getSoftInputMode())
            putInt(Config.Gravity.name, getGravity())
            getFocusable()?.let { putInt(Config.FOCUSABLE.name, it) }
            getFocusableInTouchMode()?.let { putBoolean(Config.FOCUSABLE_IN_TOUCH_MODE.name, it) }
        }
    }

    private fun parseRequestFocus(argument: Bundle) {
        if (argument.containsKey(Config.RequestFocus.name)) {
            val requestFocus = argument.getBoolean(Config.RequestFocus.name, false)
            if (requestFocus) {
                binding.requestFocusGp.check(R.id.request_focus_yes)
            } else {
                binding.requestFocusGp.check(R.id.request_focus_no)
            }
        }
    }

    private fun Int.isFlag(flag: Int, mask: Int) : Boolean {
        return (this and mask) == flag
    }

    private fun Int.hasFlag(flag: Int) : Boolean {
        return (this and flag) == flag
    }

    private fun parseSoftInputMode(argument: Bundle) {
        val softInputMode = argument.getInt(Config.SoftInputMode.name, 0)
        when {
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN, 0x0f)->{
                binding.stateGp.check(R.id.stateHidden)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE, 0x0f)->{
                binding.stateGp.check(R.id.stateVisible)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED, 0x0f)->{
                binding.stateGp.check(R.id.stateUnchanged)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN, 0x0f)->{
                binding.stateGp.check(R.id.stateAlwaysHidden)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE, 0x0f)->{
                binding.stateGp.check(R.id.stateAlwaysVisible)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED, 0x0f)->{
                binding.stateGp.check(R.id.stateUnspecified)
            }
            else -> {
                binding.stateGp.check(R.id.stateUnspecified)
            }
        }

        when {
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN, 0xf0) -> {
                binding.adjustGp.check(R.id.adjust_pan)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE, 0xf0) -> {
                binding.adjustGp.check(R.id.adjust_resize)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN, 0xf0) -> {
                binding.adjustGp.check(R.id.adjust_pan)
            }
            softInputMode.isFlag(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED, 0xf0) -> {
                binding.adjustGp.check(R.id.adjust_unspecified)
            }
            else -> {
                binding.adjustGp.check(R.id.adjust_unspecified)
            }
        }
    }

    private fun parseGravity(argument: Bundle) {
        val gravity = argument.getInt(Config.Gravity.name, 0)
        when(gravity) {
            Gravity.TOP -> {
                binding.gravityGp.check(R.id.gravity_top)
            }
            Gravity.BOTTOM -> {
                binding.gravityGp.check(R.id.gravity_bottom)
            }
            else -> {

            }
        }
    }

    private fun parseFocusable(argument: Bundle) {
        if (!argument.containsKey(Config.FOCUSABLE.name)) {
            return
        }
        val focusable = argument.getInt(Config.FOCUSABLE.name, 0)
        if (focusable.hasFlag(View.FOCUSABLE)) {
            binding.etFocusable.isChecked = true
        }
        if (focusable.hasFlag(View.FOCUSABLE_AUTO)) {
            binding.etFocusableAuto.isChecked = true
        }
        if (focusable.hasFlag(View.NOT_FOCUSABLE)) {
            binding.etNotFocusable.isChecked = true
        }

    }

    private fun parseFocusableInTouchMode(argument: Bundle) {
        if (!argument.containsKey(Config.FOCUSABLE_IN_TOUCH_MODE.name)) {
            return
        }
        val focusInTouchMode = argument.getBoolean(Config.FOCUSABLE_IN_TOUCH_MODE.name, false)
        if (focusInTouchMode) {
            binding.touchModeFocusYes.isChecked = true
        } else {
            binding.touchModeFocusNo.isChecked = true
        }
    }

    private fun parseArgument(argument: Bundle) {
        parseRequestFocus(argument)
        parseSoftInputMode(argument)
        parseGravity(argument)
        parseFocusable(argument)
        parseFocusableInTouchMode(argument)
    }

    private fun getFocusable(): Int? {
        if (binding.etFocusable.isChecked || binding.etFocusableAuto.isChecked || binding.etNotFocusable.isChecked) {
            var focusable = 0
            if (binding.etFocusable.isChecked) {
                focusable = focusable or View.FOCUSABLE
            }
            if (binding.etFocusableAuto.isChecked) {
                focusable = focusable or View.FOCUSABLE_AUTO
            }
            if (binding.etNotFocusable.isChecked) {
                focusable = focusable or View.NOT_FOCUSABLE
            }
            return focusable
        }
        return null
    }

    private fun getFocusableInTouchMode(): Boolean? {
        return when(binding.touchModeFocusGp.checkedRadioButtonId) {
            R.id.touch_mode_focus_yes -> {
                true
            }
            R.id.touch_mode_focus_no -> {
                false
            }
            else -> {
                null
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let { parseArgument(it) }
        binding.start.setOnClickListener {
            FocusActivity.startActivity(
                requireContext(),
                shouldRequestFocus(),
                getSoftInputMode(),
                getGravity(),
                getFocusable(),
                getFocusableInTouchMode(),
                getArgument(),
            )
        }
    }
}