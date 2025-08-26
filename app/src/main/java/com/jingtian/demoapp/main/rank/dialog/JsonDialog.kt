package com.jingtian.demoapp.main.rank.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.jingtian.demoapp.databinding.DialogJsonBinding

class JsonDialog(context: Context, var callback: Callback, var import: Boolean = false) : Dialog(context) {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog, json: String, import: Boolean)
            fun onNegativeClick(dialog: Dialog)
        }
    }

    private lateinit var binding: DialogJsonBinding

    private var json: String = ""
    fun setJson(json: String) {
        this.json = json
        if (::binding.isInitialized) {
            binding.et.setText(json)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogJsonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding.et) {
            binding.et.setText(json)
        }
        with(binding.positive) {
            setOnClickListener {
                callback.onPositiveClick(this@JsonDialog, binding.et.text.toString(), import)
            }
        }
        with(binding.negative) {
            setOnClickListener {
                callback.onNegativeClick(this@JsonDialog)
            }
        }
    }
}