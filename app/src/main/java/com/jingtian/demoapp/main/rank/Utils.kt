package com.jingtian.demoapp.main.rank

import android.content.Context
import android.net.Uri
import android.os.FileUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.jingtian.demoapp.R
import com.jingtian.demoapp.main.StorageUtil
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.RankItemImage
import com.jingtian.demoapp.main.widget.StarRateView
import java.io.File
import kotlin.math.abs

object Utils {
    object DataHolder {

        object ImageStorage {

            private val sp = app.getSharedPreferences("rank-image-store", Context.MODE_PRIVATE)
            private const val RANK_IMAGE_PREFIX = "rank_image_"
            private const val RANK_IMAGE_STORE_DIR = "rank_image"
            private var id by StorageUtil.StorageLong(sp, "image-id", 0L)

            private val imageCache = HashMap<Long, Uri>()

            fun getImage(id: Long): Uri? {
                if (imageCache.containsKey(id)) {
                    return imageCache[id]
                }
                val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
                val storageFile = File(storeDir, RANK_IMAGE_PREFIX + id)
                return if (storageFile.exists()) {
                    storageFile.toUri()
                } else {
                    null
                }
            }

            fun storeImage(uri: Uri): Long {
                if (uri == Uri.EMPTY) {
                    return -1
                }
                val id = this.id++
                imageCache[id] = uri
                val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
                if (!storeDir.exists()) {
                    storeDir.mkdirs()
                } else if (storeDir.isFile) {
                    storeDir.delete()
                    storeDir.mkdirs()
                }
                val storageFile = File(storeDir, RANK_IMAGE_PREFIX + id)
                if (storageFile.exists()) {
                    storageFile.delete()
                }
                app.contentResolver.openInputStream(uri)?.use { input->
                    storageFile.outputStream().use { output->
                        FileUtils.copy(input, output)
                    }
                }
                return id
            }
        }

        object UriConverter : TypeAdapter<RankItemImage>() {
            override fun write(out: JsonWriter, value: RankItemImage) {
                out.value(value.id)
            }

            override fun read(`in`: JsonReader?): RankItemImage {
                val id = `in`?.nextLong()
                if (id != null) {
                    return RankItemImage(id, ImageStorage.getImage(id) ?: Uri.EMPTY)
                }
                return RankItemImage()
            }
        }

        private val rankItemGson = GsonBuilder()
            .registerTypeAdapter(RankItemImage::class.java, UriConverter)
            .create()

        private val modelRankListType = object : TypeToken<List<ModelRank>>(){}
        private val modelRankItemListType = object : TypeToken<List<ModelRankItem>>(){}

        fun toJson(any: Any) : String {
            return rankItemGson.toJson(any)
        }

        fun toModelRankList(json: String) : List<ModelRank>? {
            return try {
                rankItemGson.fromJson(json, modelRankListType.type) as List<ModelRank>
            } catch (ignore : Exception) {
                null
            }
        }

        fun toModelRankItemList(json: String) : List<ModelRankItem>? {
            return try {
                rankItemGson.fromJson(json, modelRankItemListType.type) as List<ModelRankItem>
            } catch (ignore : Exception) {
                null
            }
        }

        var rankDataStore by StorageUtil.StorageJson<Any, MutableList<ModelRank>>(
            app.getSharedPreferences("rank-list", Context.MODE_PRIVATE),
            "rank-list",
            mutableListOf(),
            rankItemGson,
            object : TypeToken<MutableList<ModelRank>>() {}
        )
    }

    object RecyclerViewUtils {
        class FixNestedScroll(
            private val view: View,
            private val orientation: Int
        ) : View.OnTouchListener {
            private var initX = 0f
            private var initY = 0f
            private var scrolled = false
            private var touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

            private val Float.intSign: Int
                get() = when {
                    this > 0 -> {
                        1
                    }

                    this < 0 -> {
                        -1
                    }

                    else -> {
                        0
                    }
                }

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = event.x
                        initY = event.y
                        scrolled = false
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!scrolled) {
                            val dx = initX - event.x
                            val dy = initY - event.y
                            val absDx = abs(dx)
                            val absDy = abs(dy)
                            if (orientation == RecyclerView.HORIZONTAL && absDx > absDy && view.canScrollHorizontally(
                                    dx.intSign
                                )
                            ) {
                                scrolled = true
                            } else if (orientation == RecyclerView.VERTICAL && absDy > absDx && view.canScrollVertically(
                                    dy.intSign
                                )
                            ) {
                                scrolled = true
                            } else if (absDy > touchSlop && absDy > touchSlop) {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                }
                return false
            }

        }
    }

    fun StarRateView.commonConfig() {
        updateStarConfig(
            false,
            5,
            3f.dp,
            ResourcesCompat.getDrawable(resources, R.drawable.star_high_lighted, null),
            ResourcesCompat.getDrawable(resources, R.drawable.star, null),
        )
    }

    fun StarRateView.commonScrollableConfig() {
        updateStarConfig(
            true,
            5,
            3f.dp,
            ResourcesCompat.getDrawable(resources, R.drawable.star_high_lighted, null),
            ResourcesCompat.getDrawable(resources, R.drawable.star, null),
        )
    }
}