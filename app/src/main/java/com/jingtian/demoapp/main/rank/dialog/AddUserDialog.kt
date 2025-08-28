package com.jingtian.demoapp.main.rank.dialog

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.jingtian.demoapp.databinding.DialogUserRegisterBinding
import com.jingtian.demoapp.main.ScreenUtils.screenWidth
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.getBaseActivity
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.Utils.commonScrollableConfig
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankUser
import com.jingtian.demoapp.main.rank.model.RankItemImage
import com.jingtian.demoapp.main.widget.StarRateView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddUserDialog(context: Context, private val callback : Callback, private val modelRankUser: ModelRankUser? = null) : Dialog(context), BaseActivity.Companion.MediaPickerCallback {

    companion object {
        interface Callback {
            fun onPositiveClick(dialog: Dialog, modelRankUser: ModelRankUser)
            fun onNegativeClick(dialog: Dialog)
            fun onDeleteClick(dialog: Dialog)
        }
    }

    private val binding = DialogUserRegisterBinding.inflate(LayoutInflater.from(context))

    private var imageUri = Uri.EMPTY

    private var baseActivity = context.getBaseActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding.positive) {
            setOnClickListener {
                val userName = binding.etUserName.text.toString()
                if (userName.isEmpty()) {
                    Toast.makeText(context, "不写名字的小朋友不许走~", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dismiss()
                val modelRankUser= modelRankUser
                val oldId = modelRankUser?.image?.id
                Utils.CoroutineUtils.runIOTask({
                    val id = if (oldId != null) {
                        Utils.DataHolder.ImageStorage.storeImage(oldId, imageUri)
                    } else {
                        Utils.DataHolder.ImageStorage.storeImage(imageUri)
                    }
                    val user = ModelRankUser(
                        userName,
                        RankItemImage(id, imageUri)
                    )
                    Utils.CoroutineUtils.runIOTask({
                        if (modelRankUser != null) {
                            Utils.DataHolder.rankDB.runInTransaction {
                                Utils.DataHolder.rankDB.rankUserDao().delete(modelRankUser)
                                Utils.DataHolder.rankDB.rankUserDao().insert(user)
                            }
                        } else {
                            Utils.DataHolder.rankDB.rankUserDao().insert(user)
                        }
                        Utils.DataHolder.userName = userName
                    }) {}
                    user
                }) { user->
                    callback.onPositiveClick(this@AddUserDialog, user)
                }
            }
        }
        with(binding.negative) {
            setOnClickListener {
                dismiss()
                callback.onNegativeClick(this@AddUserDialog)
            }
        }
        with(binding.delete) {
            setOnClickListener {
                dismiss()
                val modelRankUser = modelRankUser
                if (modelRankUser != null) {
                    Utils.CoroutineUtils.runIOTask({
                        Utils.DataHolder.rankDB.rankUserDao().delete(modelRankUser) > 0
                    }) { success ->
                        if (!success) {
                            Toast.makeText(context, "用户${modelRankUser.userName}删除失败", Toast.LENGTH_SHORT).show()
                        }
                        callback.onDeleteClick(this@AddUserDialog)
                    }
                }
            }
            if (modelRankUser != null) {
                binding.delete.visibility = View.VISIBLE
            }
        }
        with(binding.image) {
            setOnClickListener {
                baseActivity?.pickMedia(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly), null, this@AddUserDialog)
            }
            modelRankUser?.let {
                if (it.image.image != Uri.EMPTY) {
                    it.image.loadImage(this, maxWidth = context.screenWidth)
                    this@AddUserDialog.imageUri = it.image.image
                }
            }
        }
        with(binding.etUserName) {
            modelRankUser?.let {
                setText(it.userName)
            }
        }
    }

    override fun onMediaCallback(uri: Uri) {
        if (uri != Uri.EMPTY) {
            RankItemImage(image = uri).loadImage(binding.image, maxWidth = context.screenWidth)
            this.imageUri = uri
        }
    }
}