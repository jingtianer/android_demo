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
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.DialogAddRankBinding
import com.jingtian.demoapp.databinding.DialogAddRankItemBinding
import com.jingtian.demoapp.main.ScreenUtils.screenWidth
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.Utils.commonConfig
import com.jingtian.demoapp.main.rank.Utils.commonScrollableConfig
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.RankItemImage
import com.jingtian.demoapp.main.widget.StarRateView

class AddRankItemDialog(context: Context, private val rankName: String, private val callback : Callback, private val modelRank: ModelRankItem? = null) : Dialog(context), StarRateView.Companion.OnScoreChange, BaseActivity.Companion.MediaPickerCallback {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog, modelRank: ModelRankItem)
            fun onNegativeClick(dialog: Dialog)
            fun onDeleteClick(dialog: Dialog)
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
                val oldId = modelRank?.image?.id
                Utils.CoroutineUtils.runIOTask({
                    if (oldId != null) {
                        Utils.DataHolder.ImageStorage.storeImage(oldId, imageUri)
                    } else {
                        Utils.DataHolder.ImageStorage.storeImage(imageUri)
                    }
                }) { id->
                    callback.onPositiveClick(this@AddRankItemDialog, ModelRankItem(
                        binding.etRankName.text.toString(),
                        rankName,
                        binding.starRate.getScore(),
                        binding.etDesc.text.toString(),
                        binding.rankType.getRankType(),
                        RankItemImage(id, imageUri)
                    ))
                }
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
            modelRank?.let {
                setScore(it.score)
            }
        }
        with(binding.delete) {
            setOnClickListener {
                callback.onDeleteClick(this@AddRankItemDialog)
            }
            if (modelRank != null) {
                binding.delete.visibility = View.VISIBLE
            }
        }
        onScoreChange(binding.starRate.getScore())
        with(binding.image) {
            setOnClickListener {
                baseActivity.pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            modelRank?.let {
                if (it.image.image != Uri.EMPTY) {
                    it.image.loadImage(this, maxWidth = context.screenWidth)
                    this@AddRankItemDialog.imageUri = it.image.image
                }
            }
        }
        with(binding.etRankName) {
            modelRank?.let {
                setText(it.itemName)
            }
        }
        with(binding.etDesc) {
            modelRank?.let {
                setText(it.desc)
            }
        }
        with(binding.rankType) {
            modelRank?.let {
                setRankType(it.rankType)
            }
        }
    }

    override fun onScoreChange(score: Float) {
        binding.score.text = String.format("%.2f分", score)
    }

    override fun onMediaCallback(uri: Uri) {
        if (uri != Uri.EMPTY) {
            RankItemImage(image = uri).loadImage(binding.image, maxWidth = context.screenWidth)
            this.imageUri = uri
        }
    }

    override fun dismiss() {
        super.dismiss()
        baseActivity.removeMediaPickerCallbacks(this)
    }
}