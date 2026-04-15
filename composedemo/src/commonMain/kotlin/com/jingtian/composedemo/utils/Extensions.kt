package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.node.WeakReference
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem


fun CharSequence.splitBy(predicate: (Char) -> Boolean): List<String> {
    val sb = StringBuilder()
    val resultList = mutableListOf<String>()
    for (char in this) {
        if (predicate(char)) {
            if (sb.isNotEmpty()) {
                resultList.add(sb.toString())
                sb.clear()
            }
            continue
        } else {
            sb.append(char)
        }
    }
    if (sb.isNotEmpty()) {
        resultList.add(sb.toString())
    }
    return resultList
}

fun CharSequence.splitByWhiteSpace() = splitBy(Char::isWhitespace)

fun <K, V : Any> SnapshotStateMap<K, WeakReference<V>>.getOrPutRef(key: K, default: ()->V): V {
    return get(key)?.get() ?: default().also {
        put(key, WeakReference(it))
    }
}

@Composable
fun <T> MutableState<T>.observeAsState() = remember { this }

val Path.isFile get() = SystemFileSystem.metadataOrNull(this)?.isRegularFile ?: false
val Path.isDirectory get() = SystemFileSystem.metadataOrNull(this)?.isDirectory ?: false
fun Path.delete() {
    SystemFileSystem.delete(this)
}
fun Path.exists() = SystemFileSystem.exists(this)
fun Path.mkdir() = SystemFileSystem.createDirectories(this)
fun Path.deleteRecursively() {
    if (isFile) {
        SystemFileSystem.delete(this)
    } else if (isDirectory) {
        SystemFileSystem.list(this).forEach { child->
            child.deleteRecursively()
        }
        SystemFileSystem.delete(this)
    }
}

fun Path.mkdirs() = SystemFileSystem.createDirectories(this)

fun Path.createNewFile() {
    if (SystemFileSystem.exists(this)) {
        return
    }
    SystemFileSystem.sink(this).close()
}

fun RawSource.copyTo(dest: Path) {
    val buffer = ByteArray(2048)
    SystemFileSystem.sink(dest).buffered().use { os->
        this.buffered().use { `is`->
            while (true) {
                val readCnt = `is`.readAtMostTo(buffer)
                if (readCnt <= 0) {
                    break
                }
                os.write(buffer, readCnt)
            }
        }
        os.flush()
    }
}

fun <T> synchronized(lock: Mutex, action: () -> T): T {
    return runBlocking {
        lock.withLock(action = action)
    }
}

val Path.extension: String get() {
    return name.run {
        val index = lastIndexOf(".")
        if (index > 1) {
            substring(index)
        } else {
            ""
        }
    }
}