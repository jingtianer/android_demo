package com.jingtian.demoapp.main.rank

import android.content.Context
import android.net.Uri
import android.os.FileUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
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
import com.jingtian.demoapp.main.rank.model.DateTypeConverter
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.RankItemImage
import com.jingtian.demoapp.main.rank.model.RankItemImageTypeConverter
import com.jingtian.demoapp.main.rank.model.RankItemRankTypeConverter
import com.jingtian.demoapp.main.widget.StarRateView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

object Utils {

    fun InputStream.readAllSequence(size: Int = DEFAULT_BUFFER_SIZE) = sequence<ByteArray> {
        var byteArray = readNBytes(size)
        while (byteArray.isNotEmpty()) {
            yield(byteArray)
            byteArray = readNBytes(size)
        }
    }

    fun Sequence<ByteArray>.merge(): ByteArray {
        ByteArrayOutputStream().use { bos->
            for (bytes in this) {
                bos.write(bytes)
            }
            return bos.toByteArray()
        }
    }

    object Share {
        fun readShareRankItemList(uri: Uri) : List<ModelRankItem> {
            app.contentResolver.openInputStream(uri)?.use { `is`->
                var json: String = ""
                val images: HashMap<Long, RankItemImage> = HashMap()
                ZipInputStream(`is`).use { zis->
                    var nextEntry = zis.nextEntry
                    while (nextEntry != null) {
                        val bytes = zis.readAllSequence()
                        if (nextEntry.name.endsWith(".json")) {
                            json = bytes.merge().decodeToString()
                        } else {
                            val id = nextEntry.name.toLong()
                            images[id] = Utils.DataHolder.ImageStorage.storeImage(bytes.merge())
                        }
                        zis.closeEntry()
                        nextEntry = zis.nextEntry
                    }
                }
                Utils.DataHolder.toModelRankItemList(json, images)?.let {
                    return it
                }
            }
            return listOf()
        }

        fun asyncClearShareDir() {
            Utils.CoroutineUtils.runIOTask({
                val shareDir = File(app.filesDir, "share")
                if (!shareDir.exists()) {
                    return@runIOTask
                }
                if (shareDir.isFile) {
                    shareDir.delete()
                    return@runIOTask
                }
                shareDir.listFiles()?.forEach {
                    it.delete()
                }
            }){}
        }

        fun startShare(rankName: String, data: List<ModelRankItem>) : Uri {
            val shareDir = File(app.filesDir, "share")
            if (!shareDir.exists()) {
                shareDir.mkdir()
            } else if (shareDir.exists() && shareDir.isFile) {
                shareDir.delete()
                shareDir.mkdir()
            }
            val file = File.createTempFile("share-${rankName}", ".zip", shareDir)
            val json = Utils.DataHolder.modelRankItem2Json(data)
            FileOutputStream(file).use { fos->
                ZipOutputStream(fos).use { zos->
                    data.forEach {
                        val id = it.image.id
                        val uri = it.image.image
                        app.contentResolver.openInputStream(uri)?.use {
                            val zipEntry = ZipEntry("$id")
                            zipEntry.comment = "$id"
                            zos.putNextEntry(zipEntry)
                            zos.write(it.readBytes())
                            zos.closeEntry()
                        }
                    }
                    zos.putNextEntry(ZipEntry("$rankName.json"))
                    zos.write(json.encodeToByteArray())
                    zos.closeEntry()
                }
            }
            return FileProvider.getUriForFile(
                app,
                app.packageName + ".fileprovider",
                file
            )
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
            private var id by StorageUtil.SynchronizedProperty(
                StorageUtil.StorageLong(sp, "image-id", 0L)
            )

            private val imageCache = HashMap<Long, Uri>()

            fun storagePath(id: Long) : Path {
                return Path.of(".", RANK_IMAGE_STORE_DIR, RANK_IMAGE_PREFIX + id)
            }

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

            fun delete(id: Long) {
                if (imageCache.containsKey(id)) {
                    imageCache.remove(id)
                }
                val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
                val storageFile = File(storeDir, RANK_IMAGE_PREFIX + id)
                if (storageFile.exists()) {
                    storageFile.delete()
                }
            }

            private fun Uri.safeToFile(): File? {
                return if ("file".equals(scheme, ignoreCase = true)) {
                    path?.let { File(it) }
                } else {
                    null
                }
            }

            fun storeImage(oldId: Long, uri: Uri): Long {
                if (uri == Uri.EMPTY) {
                    return -1
                }
                val id  = if (oldId >= this.id || oldId < 0) {
                    this.id++
                } else {
                    oldId
                }
                imageCache[id] = uri
                val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
                if (!storeDir.exists()) {
                    storeDir.mkdirs()
                } else if (storeDir.isFile) {
                    storeDir.delete()
                    storeDir.mkdirs()
                }
                val storageFile = File(storeDir, RANK_IMAGE_PREFIX + id)
                if (uri.safeToFile()?.absolutePath?.equals(storageFile.absolutePath) != true) {
                    if (storageFile.exists()) {
                        storageFile.delete()
                    }
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        storageFile.outputStream().use { output ->
                            FileUtils.copy(input, output)
                        }
                    }
                }
                return id
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

        class UriConverter(private val images: HashMap<Long, RankItemImage>) : TypeAdapter<RankItemImage>() {
            override fun write(out: JsonWriter, value: RankItemImage) {
                out.value(value.id)
            }

            override fun read(`in`: JsonReader?): RankItemImage {
                `in`?.nextLong()?.let { id->
                    images[id]?.let { image->
                        return image
                    }
                }
                return RankItemImage()
            }
        }

        private val rankItemGson = GsonBuilder().create()

        private val modelRankListType = object : TypeToken<List<ModelRank>>() {}
        private val modelRankItemListType = object : TypeToken<List<ModelRankItem>>() {}

        fun modelRank2Json(any: List<ModelRank>): String {
            return rankItemGson.toJson(any)
        }

        fun modelRankItem2Json(any: List<ModelRankItem>): String {
            return GsonBuilder()
                .registerTypeAdapter(RankItemImage::class.java, UriConverter(hashMapOf()))
                .create().toJson(any)
        }

        fun toModelRankList(json: String): List<ModelRank>? {
            return try {
                rankItemGson.fromJson(json, modelRankListType.type) as List<ModelRank>
            } catch (ignore: Exception) {
                null
            }
        }

        fun toModelRankItemList(json: String, images: HashMap<Long, RankItemImage>): List<ModelRankItem>? {
            return try {
                GsonBuilder()
                    .registerTypeAdapter(RankItemImage::class.java, UriConverter(images))
                    .create().fromJson(json, modelRankItemListType.type) as List<ModelRankItem>
            } catch (ignore: Exception) {
                null
            }
        }
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

    object CoroutineUtils {
        private val globalScope = CoroutineScope(Dispatchers.Main + Job())

        fun <T> runIOTask(block: Callable<T>, callback: (T)-> Unit) {
            globalScope.launch {
                val ret = withContext(Dispatchers.IO) {
                    block.call()
                }
                withContext(Dispatchers.Main) {
                    callback.invoke(ret)
                }
            }
        }

        fun Context.activityLifecycleLaunch(
            context: CoroutineContext = globalScope.coroutineContext,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            block: suspend CoroutineScope.() -> Unit
        ) {
            val baseActivity = app.activityStack[this]
            if (baseActivity != null) {
                baseActivity.lifecycleScope.launch(context, start, block)
            } else {
                globalScope.launch(context, start, block)
            }
        }
    }
}