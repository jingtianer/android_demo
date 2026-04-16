package com.jingtian.composedemo.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.jingtian.composedemo.multiplatform.WeakRef
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemPathSeparator
import kotlin.math.min


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

fun <K, V : Any> SnapshotStateMap<K, WeakRef<V>>.getOrPutRef(key: K, default: ()->V): V {
    return get(key)?.get() ?: default().also {
        put(key, WeakRef(it))
    }
}

@Composable
fun <T> MutableState<T>.observeAsState() = remember { this }

val Path.isFile get() = SystemFileSystem.metadataOrNull(this)?.isRegularFile ?: throw Exception("attr is null exists=${exists()}, $this")
val Path.isDirectory get() = SystemFileSystem.metadataOrNull(this)?.isDirectory ?: throw Exception("attr is null exists=${exists()}, $this")
fun Path.delete() = SystemFileSystem.delete(this, false)
fun Path.exists() = SystemFileSystem.exists(this)
fun Path.mkdir() = SystemFileSystem.createDirectories(this, true)
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

fun Path.mkdirs() = SystemFileSystem.createDirectories(this, true)

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
                os.write(buffer, 0, readCnt)
            }
        }
        os.flush()
    }
}

val Path.extension: String get() {
    return name.run {
        val index = lastIndexOf(".")
        if (index >= 1 && index < length - 1) {
            substring(index + 1)
        } else {
            ""
        }
    }
}

private fun Path.split(): List<String> {
    val split = this.toString().split(SystemPathSeparator)
//    println("split: this:$this, raw=${split.toTypedArray().contentDeepToString()}, $SystemPathSeparator")
    if (split.isEmpty()) {
        return split
    }
    var startIndex = 0
    var endIndex = split.size - 1
    while (startIndex < split.size && split[startIndex].isEmpty()) {
        startIndex++
    }
    while (endIndex > 0 && split[endIndex].isEmpty()) {
        endIndex--
    }
    if (startIndex <= endIndex && endIndex < split.size) {
        return split.subList(startIndex, endIndex + 1)
    }
    return listOf()

}

fun Path.relativeTo(parent: Path): Path {
//    println("relativeTo: this:$this, parent=$parent")
    if (!this.isAbsolute || !parent.isAbsolute) {
        return this
    }
    val path = this.split()
    val parentPath = parent.split()
//    println("relativeTo: split this:${path.toTypedArray().contentDeepToString()}, parent=${parentPath.toTypedArray().contentDeepToString()}")
    val len = min(path.size, parentPath.size)
    var index = 0
    while (index < len && path[index] == parentPath[index]) {
        index++
    }
    return Path(".",  *path.subList(index, path.size).toTypedArray()).apply {
//        println("relativeTo: result=$this")
    }
}