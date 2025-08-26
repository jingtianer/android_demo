package com.jingtian.demoapp.main.rank

import android.content.Context
import android.net.Uri
import android.os.FileUtils
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Database
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.jingtian.demoapp.R
import com.jingtian.demoapp.main.StorageUtil
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.dao.RankDatabase
import com.jingtian.demoapp.main.rank.dao.RankModelDao
import com.jingtian.demoapp.main.rank.dao.RankModelItemCommentDao
import com.jingtian.demoapp.main.rank.dao.RankModelItemDao
import com.jingtian.demoapp.main.rank.model.DateTypeConverter
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.RankItemImage
import com.jingtian.demoapp.main.rank.model.RankItemImageTypeConverter
import com.jingtian.demoapp.main.rank.model.RankItemRankTypeConverter
import com.jingtian.demoapp.main.widget.StarRateView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.abs
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Utils {
    object Share {
        fun readShareRankItemList(uri: Uri) : Observable<List<ModelRankItem>> {
            return Observable.create<List<ModelRankItem>> { emitter ->
                app.contentResolver.openInputStream(uri)?.use {
                    val json = it.readBytes().decodeToString()
                    Utils.DataHolder.toModelRankItemList(json)?.let {
                        emitter.onNext(it)
                    }
                }
                emitter.onComplete()
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }

        fun startShare(rankName: String, data: Any) : Observable<Uri> {
            return Observable.create<Uri> {
                val shareDir = File(app.filesDir, "share")
                if (!shareDir.exists()) {
                    shareDir.mkdir()
                } else if (shareDir.exists() && shareDir.isFile) {
                    shareDir.delete()
                    shareDir.mkdir()
                }
                val file = File.createTempFile("share-${rankName}", "", shareDir)
                file.outputStream().use {
                    it.write(Utils.DataHolder.toJson(data).encodeToByteArray())
                }
                it.onNext(
                    FileProvider.getUriForFile(
                        app,
                        app.packageName + ".fileprovider",
                        file
                    )
                )
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }
    }
    object DataHolder {

        val rankDB = Room.databaseBuilder(app, RankDatabase::class.java, "rank_db")
            .addTypeConverter(DateTypeConverter())
            .addTypeConverter(RankItemImageTypeConverter())
            .addTypeConverter(RankItemRankTypeConverter())
            .allowMainThreadQueries()
            .build()

        object ImageStorage {

            private val sp = app.getSharedPreferences("rank-image-store", Context.MODE_PRIVATE)
            private const val RANK_IMAGE_PREFIX = "rank_image_"
            private const val RANK_IMAGE_STORE_DIR = "rank_image"
            private var id by StorageUtil.StorageLong(sp, "image-id", 0L)

            private val imageCache = HashMap<Long, Uri>()

            fun getImage(id: Long): Uri? {
                if (imageCache.containsKey(id)) {
                    return imageCache[id] ?: Uri.EMPTY
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
                app.contentResolver.openInputStream(uri)?.use { input ->
                    storageFile.outputStream().use { output ->
                        FileUtils.copy(input, output)
                    }
                }
                return id
            }

            fun storeImage(byteArray: ByteArray): RankItemImage {
                val id = this.id++
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
                ByteArrayInputStream(byteArray).use { input ->
                    storageFile.outputStream().use { output ->
                        FileUtils.copy(input, output)
                    }
                }
                val uri = storageFile.toUri()
                imageCache[id] = uri
                return RankItemImage(id, uri)
            }
        }

        object UriConverter : TypeAdapter<RankItemImage>() {
            override fun write(out: JsonWriter, value: RankItemImage) {
                app.contentResolver.openInputStream(value.image)?.use { input ->
                    val image = input.readBytes()
                    val base64 = Base64.encodeToString(image, Base64.DEFAULT)
                    out.value(base64)
                }
            }

            override fun read(`in`: JsonReader?): RankItemImage {
                val base64 = `in`?.nextString()
                if (base64 != null) {
                    val image = Base64.decode(base64, Base64.DEFAULT)
                    return ImageStorage.storeImage(image)
                }
                return RankItemImage()
            }
        }

        private val rankItemGson = GsonBuilder()
            .registerTypeAdapter(RankItemImage::class.java, UriConverter)
            .create()

        private val modelRankListType = object : TypeToken<List<ModelRank>>() {}
        private val modelRankItemListType = object : TypeToken<List<ModelRankItem>>() {}

        fun toJson(any: Any): String {
            return rankItemGson.toJson(any)
        }

        fun toModelRankList(json: String): List<ModelRank>? {
            return try {
                rankItemGson.fromJson(json, modelRankListType.type) as List<ModelRank>
            } catch (ignore: Exception) {
                null
            }
        }

        fun toModelRankItemList(json: String): List<ModelRankItem>? {
            return try {
                rankItemGson.fromJson(json, modelRankItemListType.type) as List<ModelRankItem>
            } catch (ignore: Exception) {
                null
            }
        }

        var rankDataStore = rankDB.rankListDao().getAllRankModel()
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