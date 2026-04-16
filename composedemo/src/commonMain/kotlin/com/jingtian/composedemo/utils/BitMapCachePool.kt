package com.jingtian.composedemo.utils

import androidx.annotation.IntRange
import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.multiplatform.WeakRef
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.newReentrantLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.math.max

class ListWithLock<T> {
    private val lock = newReentrantLock()
    private val list: MutableList<T> = mutableListOf()
    fun read(): List<T> = list
    fun <R> write(block: (MutableList<T>) -> R): R = lock.use {
        block(list)
    }
}

object BitMapCachePool {
    private val overallImagePool: MutableMap<FileType, BitMapCache> = mutableMapOf()
    private val lock = newReentrantLock()

    private fun getBitMapCachePool(fileType: FileType): BitMapCache {
        return lock.use {
            overallImagePool.getOrPut(fileType) {
                BitMapCache(fileType)
            }
        }
    }
    class BitMapCache(private val fileType: FileType) {
        private val imagePool: MutableMap<Long, ListWithLock<Pair<Int, WeakRef<ImageBitmap>>>> = mutableMapOf()
        private val lock = newReentrantLock()

        private fun getQueue(id: Long): ListWithLock<Pair<Int, WeakRef<ImageBitmap>>> {
            return lock.use {
                imagePool.getOrPut(id) {
                    ListWithLock()
                }
            }
        }

        private fun getIfNotNull(queue: ListWithLock<Pair<Int, WeakRef<ImageBitmap>>>, index: Int): Pair<Int, ImageBitmap>? {
            queue.read().getOrNull(index)?.let { last->
                last.second.get()?.let { bitmap ->
                    return last.first to bitmap
                }
            }
            return null
        }

        fun put(id: Long, scaleFactor: Int = -1, bitmapCreator: () -> ImageBitmap?): ImageBitmap? {
            val queue = getQueue(id)
            return queue.write { list->
                if (scaleFactor == -1) {
                    getIfNotNull(queue, 0)?.let { (scaleFactor, bitmap)->
                        println("bitmapCache: id=$id, type=${fileType.value}, invalid scaleFactor, read any")
                        return@write bitmap
                    }
                    println("bitmapCache: id=$id, type=${fileType.value}, invalid scaleFactor, create")
                    return@write bitmapCreator()
                }
                val insertPos = list.binarySearch {
                    scaleFactor - it.first
                }
                val finalInsertPos = if (insertPos >= 0) {
                    val cachedBitmap = list.run {
                        val item = getOrNull(insertPos)
                        val result = item?.second?.get()
                        if (result == null && item != null) {
                            list.removeAt(insertPos)
                        }
                        result
                    }
                    if (cachedBitmap != null) {
                        println("bitmapCache: id=$id, type=${fileType.value}, scaleFactor=$scaleFactor, mem cache")
                        return@write cachedBitmap
                    }
                    insertPos
                } else {
                    -insertPos-1
                }
                val cacheFile = getCacheFile(fileType.value, id, scaleFactor)
                if (cacheFile.exists()) {
                    val bitmap = runCatching {
                        SystemFileSystem.source(cacheFile).use {
                            uriToImageBitmap(it, scaleFactor)
                        }
                    }.getOrNull()
                    if (bitmap != null) {
                        list.add(finalInsertPos, scaleFactor to WeakRef(bitmap))
                        println("bitmapCache: id=$id, type=${fileType.value}, scaleFactor=$scaleFactor, disk cache")
                        return@write bitmap
                    }
                }
                val bitmap = bitmapCreator()
                if (bitmap != null) {
                    list.add(finalInsertPos, scaleFactor to WeakRef(bitmap))
                    if (scaleFactor > 1) {
                        cacheFile.createNewFile()
                        CoroutineUtils.runIOTask({
                            SystemFileSystem.sink(cacheFile).use {
                                writeImage(bitmap, it)
                            }
                        })
                    } else {
                        cacheFile.delete()
                    }
                }
                println("bitmapCache: id=$id, type=${fileType.value}, scaleFactor=$scaleFactor, create: $bitmap")
                return@write bitmap
            }
        }

        fun clear(id: Long) {
            val queue = getQueue(id)
            queue.write {
                it.clear()
                getCacheDir(fileType.value, id).takeIf { it.exists() }?.deleteRecursively()
            }
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
        runCatching {
            if (exists()) {
                if (isFile) {
                    delete()
                    mkdirs()
                }
            } else {
                mkdir()
            }
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
        getCacheDir(fileType, storageId).ensureDir()
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
        val scaleFactor = image.inputStream?.use { `is`->
            // 第一步：仅解码边界，获取图片原始宽高
            val (outWidth, outHeight) = uriToImageSize(`is`)
            // 第二步：计算缩放比例（避免图片过大导致 OOM）
            calculateScaleFactor(outWidth, outHeight, maxWidth, maxHeight)
        } ?: 1
//        println("scale factor: $scaleFactor")
        val bitmap = image.inputStream?.use { `is`->
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
            image.inputStream?.use { `is`->
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
