package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.utils.globalWorkDir
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
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

        fun browse(file: File) {
            runCatching {
                val linkedFile = Files.createLink(Path(linkDir.path, file.name + "_link_${Random.nextULong()}.html"), Path(file.parentFile.path, file.name))
                linkedFile.toFile().deleteOnExit()
                Desktop.getDesktop().browse(linkedFile.toUri())
            }
        }
    }
    private fun openUrl(file: File) {
        browse(file)
    }

    private fun openFile(file: File) {
        runCatching {
            Desktop.getDesktop().open(file)
        }
    }

    override fun launch(fileInfo: FileInfo) {
        val file = fileInfo.getFileUri()?.file ?: return
        when(fileInfo.fileType) {
            FileType.HTML -> openUrl(file)
            else -> openFile(file)
        }
    }
}

@Composable
actual fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher> {
    return remember {
        mutableStateOf(AlbumItemLauncher())
    }
}