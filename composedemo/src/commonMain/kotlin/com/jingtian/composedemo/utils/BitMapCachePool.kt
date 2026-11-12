package com.jingtian.composedemo.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.jingtian.composedemo.multiplatform.WeakRef
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.newReentrantLock
import com.jingtian.composedemo.multiplatform.use
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.RawSink
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

class ListWithLock<T> {
    val lock = newReentrantLock()
    val list: MutableList<T> = mutableListOf()
    fun read(): List<T> = list
    @OptIn(ExperimentalContracts::class)
    suspend inline fun <R> write(block: (MutableList<T>) -> R): R {
        contract {
            callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        }
        return lock.use {
            block(list)
        }
    }
}

object BitMapCachePool {
    private val overallImagePool: MutableMap<FileType, BitMapCache> = mutableMapOf()
    private val lock = newReentrantLock()

    private const val SCALE_FACTOR_GET_HIGHEST = -1
    private const val SCALE_FACTOR_GET_HIGHEST_AND_PUT = -2

    private suspend fun getBitMapCachePool(fileType: FileType): BitMapCache {
        return lock.use {
            overallImagePool.getOrPut(fileType) {
                BitMapCache(fileType)
            }
        }
    }

    class BitMapCache(private val fileType: FileType) {
        private val imagePool: MutableMap<Long, WeakRef<ListWithLock<Pair<Int, ImageBitmap>>>> =
            mutableMapOf()
        private val lock = newReentrantLock()

        private suspend fun getQueue(id: Long): ListWithLock<Pair<Int, ImageBitmap>> {
            return lock.use {
                ensureCacheDir(fileType.value, id)
                imagePool.getOrPutRef(id) {
                    ListWithLock()
                }
            }
        }

        private fun getIfNotNull(
            queue: ListWithLock<Pair<Int, ImageBitmap>>,
            index: Int
        ): Pair<Int, ImageBitmap>? {
            queue.read().getOrNull(index)?.let { last ->
                return last
            }
            return null
        }

        suspend fun put(
            id: Long,
            scaleFactor: Int = -1,
            bitmapCreator: suspend () -> ImageBitmap?
        ): ImageBitmap? {
            val queue = getQueue(id)
            return queue.write { list ->
                if (scaleFactor == SCALE_FACTOR_GET_HIGHEST_AND_PUT) {
                    getIfNotNull(queue, 0)?.let { (scaleFactor, bitmap) ->
                        if (scaleFactor <= 1) {
                            return@write bitmap
                        }
                    }
                    return@write bitmapCreator()?.also {
                        list.add(0, 1 to it)
                    }
                }
                if (scaleFactor == SCALE_FACTOR_GET_HIGHEST) {
                    getIfNotNull(queue, 0)?.let { (scaleFactor, bitmap) ->
//                        println("bitmapCache: id=$id, type=${fileType.value}, invalid scaleFactor, read any")
                        return@write bitmap
                    }
//                    println("bitmapCache: id=$id, type=${fileType.value}, invalid scaleFactor, create")
                    return@write bitmapCreator()
                }
                val insertPos = list.binarySearch {
                    scaleFactor - it.first
                }
                val finalInsertPos = if (insertPos >= 0) {
                    val cachedBitmap = list.run {
                        val item = getOrNull(insertPos)
                        val result = item?.second
                        if (result == null && item != null) {
                            list.removeAt(insertPos)
                        }
                        result
                    }
                    if (cachedBitmap != null) {
//                        println("bitmapCache: id=$id, type=${fileType.value}, scaleFactor=$scaleFactor, mem cache")
                        return@write cachedBitmap
                    }
                    insertPos
                } else {
                    -insertPos - 1
                }
                val cacheFile = getCacheFile(fileType.value, id, scaleFactor)
                if (cacheFile.exists()) {
                    val bitmap = runCatching {
                        uriToImageBitmap(cacheFile, 1)
                    }.getOrNull()
                    if (bitmap != null) {
                        list.add(finalInsertPos, scaleFactor to bitmap)
//                        println("bitmapCache: id=$id, type=${fileType.value}, scaleFactor=$scaleFactor, disk cache")
                        return@write bitmap
                    }
                }
                val bitmap = bitmapCreator()
                if (bitmap != null) {
                    list.add(finalInsertPos, scaleFactor to bitmap)
                    if (scaleFactor > 1) {
                        CoroutineUtils.runIOTask({
                            SystemFileSystem.sink(cacheFile).use {
                                writeImage(bitmap, it)
                            }
                        }, onFailure = { e ->
//                            println("bitmapCache: id=$id, type=${fileType.value}, scaleFactor=$scaleFactor, cache fail\n$e")
                        })
                    } else {
                        cacheFile.delete()
                    }
                }
//                println("bitmapCache: id=$id, type=${fileType.value}, scaleFactor=$scaleFactor, create: $bitmap")
                return@write bitmap
            }
        }

        suspend fun clear(id: Long) {
            val queue = getQueue(id)
            queue.write {
                it.clear()
                getCacheDir(fileType.value, id).deleteRecursively()
            }
        }
    }

    private fun getCacheStoreRoot(fileType: Int): Path {
        return if (fileType == FileType.HTML.value) getFileStorageRootDir() else getFileCacheStorageRootDir()
    }

    private fun getCacheFile(fileType: Int, storageId: Long, scaleFactor: Int): Path {
        return Path(getCacheDir(fileType, storageId), "$scaleFactor").ensureFile()
    }

    private fun Path.ensureFile(): Path {
        deleteIfNotFile()
        return this
    }

    private fun ensureCacheDir(fileType: Int, storageId: Long): Path {
        return getCacheDir(fileType, storageId).apply { ensureDir() }
    }

    private fun getCacheDir(fileType: Int, storageId: Long): Path {
        return Path(getCacheStoreRoot(fileType), "bitmap_cache/bitmap_${fileType}", "$storageId")
    }

    suspend fun invalid(id: Long, fileType: FileType) {
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
        return max(1, max(widthScaleFactor, heightScaleFactor) / 2)
    }

    fun toBitMap(
        scope: CoroutineScope,
        uri: MultiplatformFile,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        onImageResult: (Int, ImageBitmap?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val (scaleFactor, bitmap) = BitMapCachePool.toBitMap(uri, maxWidth, maxHeight)
            withContext(Dispatchers.Main) {
                onImageResult(scaleFactor, bitmap)
            }
        }
    }

    suspend fun toBitMap(
        image: MultiplatformFile,
        maxWidth: Int = -1,
        maxHeight: Int = -1
    ): Pair<Int, ImageBitmap?> {
        val scaleFactor = run {
            // 第一步：仅解码边界，获取图片原始宽高
            val (outWidth, outHeight) = image.imageRatio()
            // 第二步：计算缩放比例（避免图片过大导致 OOM）
            calculateScaleFactor(outWidth, outHeight, maxWidth, maxHeight)
        }
//        println("scale factor: $scaleFactor")
        val bitmap = image.imageThumbnail(scaleFactor)
        return scaleFactor to bitmap
    }
    suspend fun saveThumbnail(
        fileInfo: FileInfo,
        creator: suspend () -> ImageBitmap?,
    ): Pair<Int, ImageBitmap?> {
        val pool = getBitMapCachePool(fileInfo.fileType)
        val bitmap = pool.put(fileInfo.storageId, 1, creator)
        return 1 to bitmap
    }

    suspend fun loadThumbnail(
        fileInfo: FileInfo,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
        creator: suspend () -> ImageBitmap?,
    ): Pair<Int, ImageBitmap?> {
        val pool = getBitMapCachePool(fileInfo.fileType)
        val bitmap = pool.put(fileInfo.storageId, SCALE_FACTOR_GET_HIGHEST_AND_PUT, creator)
        if (bitmap == null) {
            return 1 to creator()
        }
        val scaleFactor = calculateScaleFactor(
            bitmap.width,
            bitmap.height,
            maxWidth,
            maxHeight
        )
        if (scaleFactor == 1) {
            return 1 to bitmap
        }
        return scaleFactor to pool.put(fileInfo.storageId, scaleFactor) {
            bitmap.scale(
                calculateScaleFactor(
                    bitmap.width,
                    bitmap.height,
                    maxWidth,
                    maxHeight
                )
            )
        }
    }

    private fun ImageBitmap.scale(
        scaleFactor: Int,
        config: ImageBitmapConfig = ImageBitmapConfig.Argb8888
    ): ImageBitmap {
        if (scaleFactor <= 1) return this

        val targetW = (width / scaleFactor).coerceAtLeast(1)
        val targetH = (height / scaleFactor).coerceAtLeast(1)

        val outBitmap = ImageBitmap(targetW, targetH, config)
        val canvas = Canvas(outBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            // 缩小图片时，Medium（双线性插值）在画质与 CPU 性能之间平衡最好，避免 High 带来的严重卡顿
            filterQuality = FilterQuality.Medium
        }

        // 关键修复：使用 drawImageRect 将完整的 srcSize 映射缩放到 dstSize
        canvas.drawImageRect(
            image = this,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(targetW, targetH),
            paint = paint
        )
        return outBitmap
    }

    suspend fun loadImage(
        fileInfo: FileInfo,
        maxWidth: Int = -1,
        maxHeight: Int = -1,
    ): Pair<Int, ImageBitmap?> {
        val image = fileInfo.getFileUri() ?: return -1 to null
        val id = fileInfo.storageId.takeIf { it != DataBase.INVALID_ID } ?: return -1 to null
        // 第一步：仅解码边界，获取图片原始宽高
        val (outWidth, outHeight) = image.imageRatio()
        // 第二步：计算缩放比例（避免图片过大导致 OOM）
        val scaleFactor = calculateScaleFactor(outWidth, outHeight, maxWidth, maxHeight)
        val bitmap = getBitMapCachePool(fileInfo.fileType).put(id, scaleFactor) {
            fileInfo.getFileUri()?.imageThumbnail(scaleFactor)
        }
        return scaleFactor to bitmap
    }
}

expect fun uriToImageBitmap(path: Path, scaleFactor: Int): ImageBitmap?
expect fun writeImage(bitmap: ImageBitmap, os: RawSink)
