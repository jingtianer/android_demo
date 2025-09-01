package com.jingtian.demoapp.main.rank.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import com.jingtian.demoapp.databinding.DialogJsonBinding

class AddCommentDialog(context: Context, var callback: Callback, private val showDelete: Boolean = false): Dialog(context) {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog, comment: String)
            fun onNegativeClick(dialog: Dialog)
            fun onDeleteClick(dialog: Dialog)
        }
    }

    private lateinit var binding: DialogJsonBinding

    private var comment: String = ""

    fun setComment(comment: String) {
        this.comment = comment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogJsonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        with(binding.et) {
            setText(comment)
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
        with(binding.delete) {
            if (showDelete) {
                visibility = View.VISIBLE
            }
            setOnClickListener {
                callback.onDeleteClick(this@AddCommentDialog)
            }
        }
    }
}