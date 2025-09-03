package com.jingtian.demoapp.main.rank.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.transition.addListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.R
import com.jingtian.demoapp.databinding.ActivityRankItemBinding
import com.jingtian.demoapp.main.ScreenUtils.screenWidth
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.base.BaseActivity
import com.jingtian.demoapp.main.base.BaseHeaderFooterAdapter
import com.jingtian.demoapp.main.base.ISharedElement
import com.jingtian.demoapp.main.base.SharedElementParcelable
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.getBaseActivity
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.adapter.CommentListAdapter
import com.jingtian.demoapp.main.rank.dialog.AddCommentDialog
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRankUser
import com.jingtian.demoapp.main.rank.model.RelationUserAndComment
import com.jingtian.demoapp.main.widget.RankTypeChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class RankItemActivity : BaseActivity(), AddCommentDialog.Companion.Callback {

    companion object {
        fun startActivity(
            context: Context,
            rankName: String,
            rankItemName: String,
            placeHoldImage: Long = -1,
            placeHoldImageFactor: Int = -1,
            imageRadius: FloatArray = FloatArray(4),
            rankTypeRadius: FloatArray = FloatArray(4),
            transitionElementMap: Map<View, String> = mapOf()
        ) {
            val options = context.getBaseActivity()?.let {
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    it,
                    *transitionElementMap.mapTo(ArrayList()) { (k, v) ->
                        androidx.core.util.Pair<View, String>(k, v)
                    }.toTypedArray()
                )
            }
            context.startActivity(Intent(context, RankItemActivity::class.java).apply {
                putExtra(KEY_RANK_NAME, rankName)
                putExtra(KEY_RANK_ITEM_NAME, rankItemName)
                putExtra(KEY_PLACE_HOLD_IMAGE, placeHoldImage)
                putExtra(KEY_PLACE_HOLD_IMAGE_FACTOR, placeHoldImageFactor)
            }, options?.toBundle())
        }

        private const val KEY_RANK_NAME = "RANK_NAME"
        private const val KEY_RANK_ITEM_NAME = "RANK_ITEM_NAME"
        private const val KEY_PLACE_HOLD_IMAGE = "PLACE_HOLD_IMAGE"
        private const val KEY_PLACE_HOLD_IMAGE_FACTOR = "PLACE_HOLD_IMAGE_FACTOR"
        private val IMAGE_WIDTH = app.screenWidth - 24f.dp
    }

    private lateinit var binding: ActivityRankItemBinding
    private var rankName: String = ""
    private var rankItemName: String = ""

    private val commentAdapter = CommentListAdapter()
    private val commentHeaderFooterAdapter = BaseHeaderFooterAdapter<RelationUserAndComment>()

    class SharedElementCallbackImpl() : SharedElementCallback() {
        private val snapshotMap = mutableMapOf<View, SharedElementParcelable>()
        override fun onCreateSnapshotView(context: Context?, snapshot: Parcelable?): View {
            if (snapshot is SharedElementParcelable && context != null) {
                val view = snapshot.createSnapShotView(context)
                snapshotMap[view] = snapshot
                Log.d("TAG", "onCreateSnapshotView: ${view.hashCode()}")
                return view
            }
            return super.onCreateSnapshotView(context, snapshot)
        }

        override fun onSharedElementStart(
            sharedElementNames: MutableList<String>?,
            sharedElements: MutableList<View>?,
            sharedElementSnapshots: MutableList<View>?
        ) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots)
            sharedElements ?: return
            sharedElementSnapshots ?: return
            for ((index, shareElement) in sharedElements.withIndex()) {
                if (shareElement is ISharedElement) {
                    Log.d(
                        "TAG",
                        "onSharedElementStart: ${shareElement.hashCode()} ${shareElement.onCaptureSharedElementSnapshot().bundle}"
                    )
//                    val newSnapShot = shareElement.onCaptureSharedElementSnapshot()
                    shareElement.applySnapShot(snapshotMap[sharedElementSnapshots[index]], true)
//                    snapshotMap[sharedElementSnapshots[index]] = newSnapShot
                }
            }
        }

        override fun onSharedElementEnd(
            sharedElementNames: MutableList<String>?,
            sharedElements: MutableList<View>?,
            sharedElementSnapshots: MutableList<View>?
        ) {
            super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
            sharedElements ?: return
            sharedElementSnapshots ?: return
            for ((index, shareElement) in sharedElements.withIndex()) {
                if (shareElement is ISharedElement) {
                    Log.d(
                        "TAG",
                        "onSharedElementStart: ${shareElement.hashCode()} ${shareElement.onCaptureSharedElementSnapshot().bundle}"
                    )
//                    val newSnapShot = shareElement.onCaptureSharedElementSnapshot()
                    shareElement.applySnapShot(snapshotMap[sharedElementSnapshots[index]], false)
//                    snapshotMap[sharedElementSnapshots[index]] = newSnapShot
                }
            }
        }

        override fun onCaptureSharedElementSnapshot(
            sharedElement: View?,
            viewToGlobalMatrix: Matrix?,
            screenBounds: RectF?
        ): Parcelable {
            if (sharedElement is ISharedElement) {
                return sharedElement.onCaptureSharedElementSnapshot()
            }
            return super.onCaptureSharedElementSnapshot(
                sharedElement,
                viewToGlobalMatrix,
                screenBounds
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRankItemBinding.inflate(layoutInflater)
        setEnterSharedElementCallback(SharedElementCallbackImpl())
        setExitSharedElementCallback(SharedElementCallbackImpl())
        setContentView(binding.root)
        supportPostponeEnterTransition()
        rankName = intent.getStringExtra(KEY_RANK_NAME) ?: ""
        rankItemName = intent.getStringExtra(KEY_RANK_ITEM_NAME) ?: ""

        lifecycleScope.launch {
            val placeHoldImage = intent.getLongExtra(KEY_PLACE_HOLD_IMAGE, -1)
            val placeHoldImageFactor = intent.getIntExtra(KEY_PLACE_HOLD_IMAGE_FACTOR, -1)
            Utils.DataHolder.ImagePool.get(placeHoldImage, placeHoldImageFactor)?.let {
                Log.d(
                    "TAG",
                    "onCreate: placeHoldImage=$placeHoldImage, placeHoldImageFactor=$placeHoldImageFactor"
                )
                binding.image.setImageBitmap(it)
            }
            if (rankName.isNotEmpty() && rankItemName.isNotEmpty()) {
                lifecycleScope.launch {
                    val data = withContext(Dispatchers.IO) {
                        Utils.DataHolder.rankDB.rankItemDao().getRankItem(rankName, rankItemName)
                    }.getOrNull(0) ?: return@launch
                    withContext(Dispatchers.Main) {
                        with(binding.starRate) {
                            updateStarConfig(
                                false,
                                5,
                                3f.dp,
                                ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.star_high_lighted,
                                    null
                                ),
                                ResourcesCompat.getDrawable(resources, R.drawable.star, null),
                            )
                            setScore(data.score)
                        }
                        with(binding.rankType) {
                            val bg = RankTypeChooser.createBg(data.rankType)
                            bg.setTextSize(32f.dp)
                            binding.rankType.layoutParams.apply {
                                width = bg.getWidth().toInt() + 8f.dp.toInt()
                                height = bg.getHeight().toInt()
                            }
                            background = bg
                        }
                        with(binding.title) {
                            text = data.itemName
                        }
                        with(binding.score) {
                            text = String.format("%.2f分", binding.starRate.getScore())
                        }
                        if (data.desc.isNotEmpty()) {
                            with(binding.desc) {
                                text = data.desc
                            }
                        } else {
                            binding.descLayout.visibility = View.GONE
                        }
                        window.sharedElementEnterTransition.addListener(onEnd = {
                            with(binding.image) {
                                if (data.image.isValid()) {
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                    data.image.loadImage(
                                        this,
                                        maxWidth = IMAGE_WIDTH.toInt()
                                    )
                                }
                            }
                        })
                        startPostponedEnterTransition()
                    }
                }
            }
        }
        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            commentHeaderFooterAdapter.bindRecyclerView(this, commentAdapter)
            with(binding.add) {
                setOnClickListener {
                    AddCommentDialog(this@RankItemActivity, this@RankItemActivity).apply {
                        setHint("发表评论")
                        show()
                    }
                }
            }
            lifecycleScope.launch {
                val data = withContext(Dispatchers.IO) {
                    Utils.DataHolder.rankDB.rankCommentDao().getAllUserWithComments(rankItemName)
                }
                withContext(Dispatchers.Main) {
                    commentAdapter.setDataList(data)
                }
            }
        }
    }

    override fun onPositiveClick(dialog: Dialog, comment: String) {
        dialog.dismiss()
        val user = ModelRankUser.getUserInfo() ?: run {
            Toast.makeText(this, "评论失败，请登录后再发评论哦", Toast.LENGTH_SHORT).show()
            return
        }
        val model = ModelItemComment(
            itemName = rankItemName,
            comment = comment,
            creationDate = Date(),
            lastModifyDate = Date()
        )
        Utils.CoroutineUtils.runIOTask({
            try {
                Utils.DataHolder.rankDB.rankCommentDao().insert(model)
                null
            } catch (e: Exception) {
                e
            }
        }) { e: Exception? ->
            if (e != null) {
                Toast.makeText(this, "评论失败$e", Toast.LENGTH_SHORT).show()
                return@runIOTask
            }
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    commentAdapter.append(RelationUserAndComment(user, model))
                }
            }
        }
    }

    override fun onNegativeClick(dialog: Dialog) {
        dialog.dismiss()
    }

    override fun onDeleteClick(dialog: Dialog) {

    }
}