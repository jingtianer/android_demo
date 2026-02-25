package com.jingtian.composedemo.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object BitMapCachePool {
    private val overallImagePool: ConcurrentHashMap<FileType, BitMapCache> = ConcurrentHashMap()

    private fun getBitMapCachePool(fileType: FileType): BitMapCache {
        return overallImagePool.computeIfAbsent(fileType) { k->
            BitMapCache(k)
        }
    }

    fun Bitmap?.toImmutable(): Bitmap? {
        return if (this != null && this.isMutable) {
            return this.copy(this.config, false)
        } else {
            this
        }
    }

    class BitMapCache(private val fileType: FileType) {
        private val imagePool = ConcurrentHashMap<Long, ArrayList<Pair<Int, SoftReference<Bitmap?>>>>()

        @Synchronized
        private fun getQueue(id: Long): ArrayList<Pair<Int, SoftReference<Bitmap?>>> {
            return imagePool.computeIfAbsent(id) { k->
                ArrayList()
            }
        }

        private fun getIfNotNull(queue: ArrayList<Pair<Int, SoftReference<Bitmap?>>>, index: Int): Pair<Int, Bitmap>? {
            queue.lastOrNull()?.let { last->
                last.second.get()?.let { bitmap ->
                    return last.first to bitmap
                }
            }
            return null
        }

        private fun tryGetNeighborBitmap(queue: ArrayList<Pair<Int, SoftReference<Bitmap?>>>, id: Long, scaleFactor: Int, insertPos: Int, bitmapCreator: () -> Bitmap?): Pair<Int, Bitmap>? {
            getIfNotNull(queue, insertPos-1)?.let { (neighborScaleFactor, bitmap)->
                CoroutineUtils.runIOTask({
                    createAndStoreBitmap(id, scaleFactor, -insertPos-1, queue, bitmapCreator).toImmutable()
                })
                return neighborScaleFactor to bitmap
            }
            getIfNotNull(queue, insertPos)?.let { (neighborScaleFactor, bitmap)->
                CoroutineUtils.runIOTask({
                    createAndStoreBitmap(id, scaleFactor, -insertPos-1, queue, bitmapCreator).toImmutable()
                })
                return neighborScaleFactor to bitmap
            }
            return null
        }

        fun put(id: Long, scaleFactor: Int = -1, bitmapCreator: () -> Bitmap?): Bitmap? {
            val queue = getQueue(id)
            synchronized(queue) {
                if (scaleFactor == -1) {
                    getIfNotNull(queue, 0)?.let { (scaleFactor, bitmap)->
                        return bitmap
                    }
                    return bitmapCreator().toImmutable()
                }
                val insertPos = queue.binarySearch {
                    scaleFactor - it.first
                }
                if (insertPos < 0) {
                    tryGetNeighborBitmap(queue, id, scaleFactor, -insertPos-1, bitmapCreator)?.let { (scaleFactor, bitmap)->
                        return bitmap
                    }
                    return createAndStoreBitmap(id, scaleFactor, -insertPos-1, queue, bitmapCreator)
                }
                val cachedBitmap = queue[insertPos].second.get()
                if (cachedBitmap == null || cachedBitmap.isRecycled) {
                    tryGetNeighborBitmap(queue, id, scaleFactor, insertPos, bitmapCreator)?.let { (scaleFactor, bitmap)->
                        return bitmap
                    }
                    val bitmap = bitmapCreator().toImmutable()
                    queue[insertPos] = scaleFactor to SoftReference(bitmap)
                    return bitmap
                }
                return cachedBitmap
            }
        }

        fun clear(id: Long) {
            val queue = getQueue(id)
            synchronized(queue) {
                queue.clear()
            }
            getCacheDir(fileType.value, id).takeIf { it.exists() }?.deleteRecursively()
        }

        private fun createAndStoreBitmap(id: Long, scaleFactor: Int, @IntRange(from = 0) insertPos: Int, queue: ArrayList<Pair<Int, SoftReference<Bitmap?>>>, bitmapCreator: () -> Bitmap?): Bitmap? {
            val cacheFile = getCacheFile(fileType.value, id, scaleFactor)
            if (cacheFile?.exists() == true) {
                FileInputStream(cacheFile).use {
                    BitmapFactory.decodeStream(it)?.let {
                        queue.add(insertPos, scaleFactor to SoftReference(it))
                        return@createAndStoreBitmap it
                    }
                }
            }
            val bitmap = bitmapCreator()
            queue.add(insertPos, scaleFactor to SoftReference(bitmap))
            if (bitmap != null) {
                CoroutineUtils.runIOTask({
                    val file = getCacheFile(fileType.value, id, scaleFactor)
                    if (file == null || file.exists()) {
                        return@runIOTask
                    }
                    FileOutputStream(file).use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                        it.flush()
                    }
                }, { e->

                }) {
                }
            }
            return bitmap
        }
    }

    private fun getCacheFile(fileType: Int, storageId: Long, scaleFactor: Int): File? {
        ensureCacheDir(fileType, storageId)
        return File(app.cacheDir, "bitmap_cache/bitmap_${fileType}/${storageId}/${scaleFactor}").ensureFile()
    }

    private fun File.ensureDir(): File {
        if (exists()) {
            if (isFile) {
                delete()
                mkdirs()
            }
        } else {
            mkdirs()
        }
        return this
    }

    private fun File.ensureFile(): File {
        if (exists()) {
            if (isDirectory) {
                deleteRecursively()
            }
        }
        return this
    }

    private fun ensureCacheDir(fileType: Int, storageId: Long) {
        File(app.cacheDir, "bitmap_cache/bitmap_${fileType}/${storageId}").ensureDir()
    }

    private fun getCacheDir(fileType: Int, storageId: Long): File {
        return File(app.cacheDir, "bitmap_cache/bitmap_${fileType}/${storageId}")
    }

    fun getImageRatio(uri: Uri): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只获取尺寸，不加载像素
        }
        val (width, height) =  app.contentResolver.openInputStream(uri)?.use { `is`->
            // 第一步：仅解码边界，获取图片原始宽高
            BitmapFactory.decodeStream(`is`, null, options)
            options.outWidth to options.outHeight
        } ?: (-1 to -1)
        return width to height
    }

    fun invalid(id: Long, fileType: FileType) {
        getBitMapCachePool(fileType).clear(id)
    }

    private fun calculateScaleFactor(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
    ): Int {
        var widthScaleFactor = 1
        var heightScaleFactor = 1
        if (maxWidth > 0) {
            while (originalWidth / widthScaleFactor > maxWidth) {
                widthScaleFactor *= 2 // 每次翻倍缩放
            }
        }
        if (maxHeight > 0) {
            while (originalHeight / heightScaleFactor > maxHeight) {
                heightScaleFactor *= 2 // 每次翻倍缩放
            }
        }
        return max(1,  max(widthScaleFactor,heightScaleFactor) / 2)
    }

    fun toBitMap(scope: CoroutineScope, uri: Uri, maxWidth: Int = -1, maxHeight: Int = -1, onImageResult: (Int, Bitmap?)->Unit) {
        scope.launch(Dispatchers.IO) {
            val (scaleFactor, bitmap) = BitMapCachePool.toBitMap(uri, maxWidth, maxHeight)
            withContext(Dispatchers.Main) {
                onImageResult(scaleFactor, bitmap)
            }
        }
    }

    fun toBitMap(image: Uri, maxWidth: Int = -1, maxHeight: Int = -1): Pair<Int, Bitmap?> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只获取尺寸，不加载像素
        }
        val scaleFactor =  app.contentResolver.openInputStream(image)?.use { `is`->
            // 第一步：仅解码边界，获取图片原始宽高
            BitmapFactory.decodeStream(`is`, null, options)
            // 第二步：计算缩放比例（避免图片过大导致 OOM）
            calculateScaleFactor(options.outWidth, options.outHeight, maxWidth, maxHeight)
        } ?: -1
        val bitmap = app.contentResolver.openInputStream(image)?.use { `is`->
//            Log.d("TAG", "loadImage failed: $image, $scaleFactor, $image")
            // 第三步：按缩放比例解码图片
            options.apply {
                inJustDecodeBounds = false // 实际加载像素
                inSampleSize = scaleFactor // 缩放比例（2的倍数，如 2=1/2 大小，4=1/4 大小）
                inPreferredConfig = Bitmap.Config.RGB_565 // 可选：使用 RGB_565 节省内存（比 ARGB_8888 节省一半）
            }
            BitmapFactory.decodeStream(`is`, null, options)
        }
        return scaleFactor to bitmap
    }

    fun loadImage(
        fileInfo: FileInfo,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        creator: () -> Bitmap?,
    ): Pair<Int, Bitmap?> {
        val scaleFactor = calculateScaleFactor(fileInfo.intrinsicWidth, fileInfo.intrinsicHeight, maxWidth, maxHeight)
        val bitmap =  getBitMapCachePool(fileInfo.fileType).put(fileInfo.storageId, scaleFactor, creator)
        return scaleFactor to bitmap
    }

    fun loadImage(
        fileInfo: FileInfo,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
    ): Pair<Int, Bitmap?> {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true // 只获取尺寸，不加载像素
        }
        val image = fileInfo.getFileUri()?.takeIf { it != Uri.EMPTY } ?: return -1 to null
        val id = fileInfo.storageId.takeIf { it != DataBase.INVALID_ID } ?: return -1 to null
        val scaleFactor =  app.contentResolver.openInputStream(image)?.use { `is`->
            // 第一步：仅解码边界，获取图片原始宽高
            BitmapFactory.decodeStream(`is`, null, options)
            // 第二步：计算缩放比例（避免图片过大导致 OOM）
            calculateScaleFactor(options.outWidth, options.outHeight, maxWidth, maxHeight)
        } ?: -1
        val bitmap = getBitMapCachePool(fileInfo.fileType).put(id, scaleFactor) {
            app.contentResolver.openInputStream(image)?.use { `is`->
                // 第三步：按缩放比例解码图片
                options.apply {
                    inJustDecodeBounds = false // 实际加载像素
                    inSampleSize = scaleFactor // 缩放比例（2的倍数，如 2=1/2 大小，4=1/4 大小）
                    inPreferredConfig = Bitmap.Config.RGB_565 // 可选：使用 RGB_565 节省内存（比 ARGB_8888 节省一半）
                    inMutable = false // 不可变
                }
                BitmapFactory.decodeStream(`is`, null, options)
            }
        }
        return scaleFactor to bitmap
    }
}