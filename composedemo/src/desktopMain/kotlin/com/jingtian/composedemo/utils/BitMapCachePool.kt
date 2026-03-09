package com.jingtian.composedemo.utils

import androidx.annotation.IntRange
import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.SoftReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

actual fun uriToImageBitmap(`is`: InputStream, scaleFactor: Int): ImageBitmap? = null
actual fun uriToImageBitmap(`is`: InputStream): ImageBitmap? = null
actual fun uriToImageSize(`is`: InputStream): Pair<Int, Int> = 1 to 1
actual fun writeImage(bitmap: ImageBitmap, os: OutputStream) {}