package com.jingtian.demoapp.main.rank.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.jingtian.demoapp.databinding.DialogJsonBinding

class AddCommentDialog(context: Context, var callback: Callback): Dialog(context) {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog, comment: String)
            fun onNegativeClick(dialog: Dialog)
        }
    }

    private lateinit var binding: DialogJsonBinding

    private var comment: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogJsonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding.et) {
            binding.et.setText(comment)
        }
        with(binding.positive) {
            setOnClickListener {
                callback.onPositiveClick(this@AddCommentDialog, binding.et.text.toString())
            }
        }
        with(binding.negative) {
            setOnClickListener {
                callback.onNegativeClick(this@AddCommentDialog)
            }
        }
    }
}