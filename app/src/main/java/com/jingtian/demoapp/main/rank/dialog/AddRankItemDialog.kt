package com.jingtian.demoapp.main.rank.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images.Media
import android.view.LayoutInflater
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.DialogAddRankBinding
import com.jingtian.demoapp.databinding.DialogAddRankItemBinding
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils.commonConfig
import com.jingtian.demoapp.main.rank.Utils.commonScrollableConfig
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.widget.StarRateView

class AddRankItemDialog(context: Context, private val callback : Callback) : Dialog(context), StarRateView.Companion.OnScoreChange, BaseActivity.Companion.MediaPickerCallback {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem)
            fun onNegativeClick(dialog: Dialog)
        }
    }

    private val binding = DialogAddRankItemBinding.inflate(LayoutInflater.from(context))

    private var imageUri = Uri.EMPTY

    private val baseActivity = (context as BaseActivity)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        baseActivity.addMediaPickerCallbacks(this)
        setContentView(binding.root)
        with(binding.positive) {
            setOnClickListener {
                callback.onPositiveClick(this@AddRankItemDialog, ModelRankItem(
                    binding.etRankName.text.toString(),
                    binding.starRate.getScore(),
                    binding.etDesc.text.toString(),
                    listOf(),
                    imageUri
                ))
            }
        }
        with(binding.negative) {
            setOnClickListener {
                callback.onNegativeClick(this@AddRankItemDialog)
            }
        }
        with(binding.starRate) {
            commonScrollableConfig()
            onScoreChange = this@AddRankItemDialog
        }
        onScoreChange(binding.starRate.getScore())
        with(binding.image) {
            setOnClickListener {
                baseActivity.pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    override fun onScoreChange(score: Float) {
        binding.score.text = String.format("%.2f分", score)
    }

    override fun onMediaCallback(uri: Uri) {
        if (uri != Uri.EMPTY) {
            binding.image.setImageURI(uri)
            this.imageUri = uri
        }
    }

    override fun dismiss() {
        super.dismiss()
        baseActivity.removeMediaPickerCallbacks(this)
    }
}