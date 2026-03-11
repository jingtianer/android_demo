package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.globalWorkDir
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.random.Random
import kotlin.random.nextULong

class AlbumItemLauncher : IAlbumItemLauncher {
    companion object {
        private val linkDir = File(globalWorkDir, "tmp/links")
        init {
            if (linkDir.exists()) {
                if (linkDir.isFile) {
                    linkDir.delete()
                } else if (linkDir.isDirectory) {
                    linkDir.deleteRecursively()
                }
            }
            linkDir.mkdirs()
        }

        fun File.tmpLinkFileAs(extension: String): Path {
            val linkedFile = Files.createLink(Path(linkDir.path, this.name + "_link_${Random.nextULong()}.${extension}"), Path(this.parentFile.path, this.name))
            linkedFile.toFile().deleteOnExit()
            return linkedFile
        }

        fun browse(file: File, extension: String) {
            runCatching {
                val linkedFile = file.tmpLinkFileAs(extension)
                Desktop.getDesktop().browse(linkedFile.toUri())
            }
        }
    }
    private fun openUrl(file: File, extension: String?) {
        browse(file, extension ?: "html")
    }

    private fun openFile(file: File, extension: String?) {
        runCatching {
            Desktop.getDesktop().open(file.tmpLinkFileAs(extension ?: file.extension).toFile())
        }
    }

    override fun launch(fileInfo: FileInfo) {
        val file = fileInfo.getFileUri()?.file ?: return
        when(fileInfo.fileType) {
            FileType.HTML -> openUrl(file, fileInfo.extension)
            else -> openFile(file, fileInfo.extension)
        }
    }
}

@Composable
actual fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher> {
    return remember {
        mutableStateOf(AlbumItemLauncher())
    }
}