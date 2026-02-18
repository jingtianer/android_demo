package com.jingtian.composedemo.utils

import android.graphics.Bitmap
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap

object BitMapCachePool {
    private val imagePool = ConcurrentHashMap<Long, ArrayList<Pair<Int, SoftReference<Bitmap?>>>>()

    private fun getQueue(id: Long):ArrayList<Pair<Int, SoftReference<Bitmap?>>> {
        return imagePool.getOrPut(id) { ArrayList() }
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
}