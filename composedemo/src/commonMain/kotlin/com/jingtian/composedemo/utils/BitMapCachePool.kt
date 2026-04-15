package com.jingtian.composedemo.utils

import androidx.annotation.IntRange
import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.multiplatform.WeakRef
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.readAllBytesOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.math.max

object BitMapCachePool {
    private val overallImagePool: MutableMap<FileType, BitMapCache> = mutableMapOf()
    private val lock = Mutex()

    private fun getBitMapCachePool(fileType: FileType): BitMapCache {
        return synchronized(lock) {
            overallImagePool.getOrPut(fileType) {
                BitMapCache(fileType)
            }
        }
    }
    class BitMapCache(private val fileType: FileType) {
        private val imagePool: MutableMap<Long, Pair<Mutex, ArrayList<Pair<Int, WeakRef<ImageBitmap>>>>> = mutableMapOf()
        private val lock = Mutex()

        private fun getQueue(id: Long): Pair<Mutex, ArrayList<Pair<Int, WeakRef<ImageBitmap>>>> {
            return synchronized(lock) {
                imagePool.getOrPut(id) {
                    Mutex() to ArrayList()
                }
            }
        }

        private fun getIfNotNull(queue: ArrayList<Pair<Int, WeakRef<ImageBitmap>>>, index: Int): Pair<Int, ImageBitmap>? {
            queue.lastOrNull()?.let { last->
                last.second.get()?.let { bitmap ->
                    return last.first to bitmap
                }
            }
            return null
        }

        private fun tryGetNeighborBitmap(queue: ArrayList<Pair<Int, WeakRef<ImageBitmap>>>, id: Long, scaleFactor: Int, insertPos: Int, bitmapCreator: () -> ImageBitmap?): Pair<Int, ImageBitmap>? {
            getIfNotNull(queue, insertPos-1)?.let { (neighborScaleFactor, bitmap)->
                CoroutineUtils.runIOTask({
                    createAndStoreBitmap(id, scaleFactor, -insertPos-1, queue, bitmapCreator)
                })
                return neighborScaleFactor to bitmap
            }
            getIfNotNull(queue, insertPos)?.let { (neighborScaleFactor, bitmap)->
                CoroutineUtils.runIOTask({
                    createAndStoreBitmap(id, scaleFactor, -insertPos-1, queue, bitmapCreator)
                })
                return neighborScaleFactor to bitmap
            }
            return null
        }

        fun put(id: Long, scaleFactor: Int = -1, bitmapCreator: () -> ImageBitmap?): ImageBitmap? {
            val (lock, queue) = getQueue(id)
            return synchronized(lock) {
                if (scaleFactor == -1) {
                    getIfNotNull(queue, 0)?.let { (scaleFactor, bitmap)->
                        return@synchronized bitmap
                    }
                    return@synchronized bitmapCreator()
                }
                val insertPos = queue.binarySearch {
                    scaleFactor - it.first
                }
                if (insertPos < 0) {
                    tryGetNeighborBitmap(queue, id, scaleFactor, -insertPos-1, bitmapCreator)?.let { (scaleFactor, bitmap)->
                        return@synchronized bitmap
                    }
                    return@synchronized createAndStoreBitmap(id, scaleFactor, -insertPos-1, queue, bitmapCreator)
                }
                val cachedBitmap = queue[insertPos].second.get()
                if (cachedBitmap == null) {
                    tryGetNeighborBitmap(queue, id, scaleFactor, insertPos, bitmapCreator)?.let { (scaleFactor, bitmap)->
                        return@synchronized bitmap
                    }
                    val bitmap = bitmapCreator()
                    if (bitmap != null) {
                        queue[insertPos] = scaleFactor to WeakRef(bitmap)
                    }
                    return@synchronized bitmap
                }
                return@synchronized cachedBitmap
            }
        }

        fun clear(id: Long) {
            val (lock, queue) = getQueue(id)
            synchronized(lock) {
                queue.clear()
            }
            getCacheDir(fileType.value, id).takeIf { it.exists() }?.deleteRecursively()
        }

        private fun createAndStoreBitmap(id: Long, scaleFactor: Int, @IntRange(from = 0) insertPos: Int, queue: ArrayList<Pair<Int, WeakRef<ImageBitmap>>>, bitmapCreator: () -> ImageBitmap?): ImageBitmap? {
            val cacheFile = getCacheFile(fileType.value, id, scaleFactor)
            if (cacheFile?.exists() == true) {
                SystemFileSystem.source(cacheFile).buffered().use {
                    uriToImageBitmap(it, scaleFactor)?.let {
                        queue.add(insertPos, scaleFactor to WeakRef(it))
                        return@createAndStoreBitmap it
                    }
                }
            }
            val bitmap = bitmapCreator()
            if (bitmap != null) {
                queue.add(insertPos, scaleFactor to WeakRef(bitmap))
                CoroutineUtils.runIOTask({
                    val file = getCacheFile(fileType.value, id, scaleFactor)
                    if (file == null || file.exists()) {
                        return@runIOTask
                    }
                    SystemFileSystem.sink(file).use {
                        writeImage(bitmap, it)
                        it.flush()
                    }
                }, { e->

                }) {
                }
            }
            return bitmap
        }
    }

    private fun getCacheStoreRoot(fileType: Int): Path {
        return if (fileType == FileType.HTML.value) getFileStorageRootDir() else getFileCacheStorageRootDir()
    }

    private fun getCacheFile(fileType: Int, storageId: Long, scaleFactor: Int): Path {
        ensureCacheDir((fileType), storageId)
        return Path(getCacheStoreRoot(fileType), "bitmap_cache/bitmap_${fileType}/${storageId}/${scaleFactor}").ensureFile()
    }

    private fun Path.ensureDir(): Path {
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

    private fun Path.ensureFile(): Path {
        if (exists()) {
            if (isDirectory) {
                deleteRecursively()
            }
        }
        return this
    }

    private fun ensureCacheDir(fileType: Int, storageId: Long) {
        Path(getCacheStoreRoot(fileType), "bitmap_cache/bitmap_${fileType}/${storageId}").ensureDir()
    }

    private fun getCacheDir(fileType: Int, storageId: Long): Path {
        return Path(getCacheStoreRoot(fileType), "bitmap_cache/bitmap_${fileType}/${storageId}")
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

    fun toBitMap(scope: CoroutineScope, uri: MultiplatformFile, maxWidth: Int = -1, maxHeight: Int = -1, onImageResult: (Int, ImageBitmap?)->Unit) {
        scope.launch(Dispatchers.IO) {
            val (scaleFactor, bitmap) = BitMapCachePool.toBitMap(uri, maxWidth, maxHeight)
            withContext(Dispatchers.Main) {
                onImageResult(scaleFactor, bitmap)
            }
        }
    }

    fun toBitMap(image: MultiplatformFile, maxWidth: Int = -1, maxHeight: Int = -1): Pair<Int, ImageBitmap?> {
        val `is` = image.inputStream ?: return -1 to null
        val scaleFactor = `is`.use { `is`->
            // 第一步：仅解码边界，获取图片原始宽高
            val (outWidth, outHeight) = uriToImageSize(`is`)
            // 第二步：计算缩放比例（避免图片过大导致 OOM）
            calculateScaleFactor(outWidth, outHeight, maxWidth, maxHeight)
        }
//        println("scale factor: $scaleFactor")
        val bitmap = `is`.use { `is`->
//            Log.d("TAG", "loadImage failed: $image, $scaleFactor, $image")
            // 第三步：按缩放比例解码图片
            uriToImageBitmap(`is`, scaleFactor)
        }
        return scaleFactor to bitmap
    }

    fun loadImage(
        fileInfo: FileInfo,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        creator: () -> ImageBitmap?,
    ): Pair<Int, ImageBitmap?> {
        val scaleFactor = if (fileInfo.fileType == FileType.HTML) {
            2
        } else {
            calculateScaleFactor(fileInfo.intrinsicWidth, fileInfo.intrinsicHeight, maxWidth, maxHeight)
        }
//        println("scale factor: $scaleFactor")
        val bitmap =  getBitMapCachePool(fileInfo.fileType).put(fileInfo.storageId, scaleFactor, creator)
        return scaleFactor to bitmap
    }

    fun loadImage(
        fileInfo: FileInfo,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
    ): Pair<Int, ImageBitmap?> {
        val image = fileInfo.getFileUri() ?: return -1 to null
        val id = fileInfo.storageId.takeIf { it != DataBase.INVALID_ID } ?: return -1 to null
        val scaleFactor =  if (fileInfo.intrinsicWidth > 0 && fileInfo.intrinsicHeight > 0) {
            calculateScaleFactor(fileInfo.intrinsicWidth, fileInfo.intrinsicHeight, maxWidth, maxHeight)
        } else {
            val `is` = image.inputStream
            `is`?.use { `is`->
                // 第一步：仅解码边界，获取图片原始宽高
                val (outWidth, outHeight) = uriToImageSize(`is`)
                // 第二步：计算缩放比例（避免图片过大导致 OOM）
                calculateScaleFactor(outWidth, outHeight, maxWidth, maxHeight)
            } ?: -1
        }
//        println("scale factor: $scaleFactor")
        val bitmap = getBitMapCachePool(fileInfo.fileType).put(id, scaleFactor) {
            val `is` = fileInfo.getFileUri()?.inputStream ?: return@put null
            `is`.use { `is`->
                uriToImageBitmap(`is`, scaleFactor)
            }
        }
        return scaleFactor to bitmap
    }
}

expect fun uriToImageBitmap(`is`: RawSource, scaleFactor: Int): ImageBitmap?
expect fun uriToImageBitmap(`is`: RawSource): ImageBitmap?
expect fun uriToImageSize(`is`: RawSource): Pair<Int, Int>
expect fun writeImage(bitmap: ImageBitmap, os: RawSink)
