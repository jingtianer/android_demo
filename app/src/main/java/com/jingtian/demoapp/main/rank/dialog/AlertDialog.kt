package com.jingtian.demoapp.main.rank.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.jingtian.demoapp.databinding.DialogAlertBinding

class AlertDialog (context: Context, var callback: Callback, private val hint: String) : Dialog(context) {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog)
            fun onNegativeClick(dialog: Dialog)
        }
    }

    private lateinit var binding: DialogAlertBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding.title) {
            text = this@AlertDialog.hint
        }
        with(binding.positive) {
            setOnClickListener {
                callback.onPositiveClick(this@AlertDialog)
            }
        }
        with(binding.negative) {
            setOnClickListener {
                callback.onNegativeClick(this@AlertDialog)
            }
        }
    }
}