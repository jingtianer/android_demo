package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
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

        fun FileInfo.tmpLinkFile(fileName: String, defaultExtension: String = ""): Path? {
            val originalFile = this.getFileUri()?.file ?: return null
            val linkedFile = Files.createLink(Path(linkDir.path, fileName + "_tmplink_${Random.nextULong()}.${extension ?: defaultExtension}"), Path(originalFile.parentFile.path, originalFile.name))
            linkedFile.toFile().deleteOnExit()
            return linkedFile
        }

        fun FileInfo.browse(fileName: String) {
            runCatching {
                val linkedFile = this.tmpLinkFile(fileName, defaultExtension = "html") ?: return@runCatching
                Desktop.getDesktop().browse(linkedFile.toUri())
            }
        }
    }
    private fun openUrl(fileName: String, file: FileInfo) {
        file.browse(fileName)
    }

    private fun openFile(fileName: String, file: FileInfo) {
        runCatching {
            Desktop.getDesktop().open(file.tmpLinkFile(fileName)?.toFile() ?: return)
        }
    }

    override fun launch(fileName: String, fileInfo: FileInfo) {
        when(fileInfo.fileType) {
            FileType.HTML -> openUrl(fileName, fileInfo)
            else -> openFile(fileName, fileInfo)
        }
    }

    override fun launch(albumItemRelation: AlbumItemRelation) {
        val fileInfo = albumItemRelation.fileInfo
        val name = albumItemRelation.albumItem.itemName
        launch(name, fileInfo)
    }
}

@Composable
actual fun rememberAlbumLauncher(onResult: (Long)->Unit): MutableState<IAlbumItemLauncher> {
    return remember {
        mutableStateOf(AlbumItemLauncher())
    }
}