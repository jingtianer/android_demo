package com.jingtian.composedemo.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object BitMapCachePool {
    private val imagePool = ConcurrentHashMap<Long, ArrayList<Pair<Int, SoftReference<Bitmap?>>>>()

    private fun getQueue(id: Long):ArrayList<Pair<Int, SoftReference<Bitmap?>>> {
        return imagePool.getOrPut(id) { ArrayList() }
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

    fun invalid(id: Long) {
        imagePool[id]?.clear()
    }

    fun get(id: Long, scaleFactor: Int = -1): Bitmap? {
        val queue = getQueue(id)
        synchronized(queue) {
            if (queue.isEmpty()) {
                return null
            }
            if (scaleFactor == -1) {
                return queue.lastOrNull()?.second?.get()
            }
            var insertPos = queue.binarySearch {
                scaleFactor - it.first
            }
            if (insertPos < 0) {
                insertPos = -insertPos-1
            }
            if (insertPos >= queue.size) {
                insertPos = queue.size - 1
            }
            val cachedBitmap = queue[insertPos].second.get()
            if (cachedBitmap == null || cachedBitmap.isRecycled) {
                return null
            }
            return cachedBitmap
        }
    }

    fun put(id: Long, scaleFactor: Int = -1, bitmapCreator: () -> Bitmap?): Bitmap? {
        val queue = getQueue(id)
        synchronized(queue) {
            if (scaleFactor == -1) {
                return queue.lastOrNull()?.second?.get() ?: bitmapCreator()
            }
            val insertPos = queue.binarySearch {
                scaleFactor - it.first
            }
            if (insertPos < 0) {
                val bitmap = bitmapCreator()
                queue.add(-insertPos-1, scaleFactor to SoftReference(bitmap))
                return bitmap
            }
            val cachedBitmap = queue[insertPos].second.get()
            if (cachedBitmap == null || cachedBitmap.isRecycled) {
                val bitmap = bitmapCreator()
                queue[insertPos] = scaleFactor to SoftReference(bitmap)
                return bitmap
            }
            return cachedBitmap
        }
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
            Log.d("TAG", "loadImage failed: $image, $scaleFactor, $image")
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
        val bitmap = put(id, scaleFactor) {
            app.contentResolver.openInputStream(image)?.use { `is`->
                Log.d("TAG", "loadImage failed: $id, $scaleFactor, $image")
                // 第三步：按缩放比例解码图片
                options.apply {
                    inJustDecodeBounds = false // 实际加载像素
                    inSampleSize = scaleFactor // 缩放比例（2的倍数，如 2=1/2 大小，4=1/4 大小）
                    inPreferredConfig = Bitmap.Config.RGB_565 // 可选：使用 RGB_565 节省内存（比 ARGB_8888 节省一半）
                }
                BitmapFactory.decodeStream(`is`, null, options)
            }
        }
        return scaleFactor to bitmap
    }
}