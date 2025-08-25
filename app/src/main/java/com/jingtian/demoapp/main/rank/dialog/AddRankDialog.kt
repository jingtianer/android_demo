package com.jingtian.demoapp.main.rank.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.jingtian.demoapp.databinding.DialogAddRankBinding
import com.jingtian.demoapp.main.rank.model.ModelRank

class AddRankDialog(context: Context, private val callback : Callback) : Dialog(context) {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog, modelRank: ModelRank)
            fun onNegativeClick(dialog: Dialog)
        }
    }

    private val binding = DialogAddRankBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding.positive) {
            setOnClickListener {
                callback.onPositiveClick(this@AddRankDialog, ModelRank(
                    binding.etRankName.text.toString(),
                    listOf()
                ))
            }
        }
        with(binding.negative) {
            setOnClickListener {
                callback.onNegativeClick(this@AddRankDialog)
            }
        }
    }
}