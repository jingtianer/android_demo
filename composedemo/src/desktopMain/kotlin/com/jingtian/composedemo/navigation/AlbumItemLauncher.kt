package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.utils.globalWorkDir
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import kotlin.io.path.exists

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

//        fun FileInfo.tmpLinkFile(fileName: String, defaultExtension: String = ""): Path? {
//            val originalFile = this.getFileUri()?.file ?: return null
//            val linkedFile = Files.createLink(Path(linkDir.path, fileName + "_tmplink_${Random.nextULong()}.${extension ?: defaultExtension}"), Path(originalFile.parentFile.path, originalFile.name))
//            linkedFile.toFile().deleteOnExit()
//            return originalFile.toPath()
//        }

        fun FileInfo.browse(fileName: String) {
            runCatching {
                Desktop.getDesktop().browse(this.getFileUri()?.file?.toURI() ?: return)
            }
        }
    }
    private fun openUrl(fileName: String, file: FileInfo) {
        runCatching {
            val f = file.getFileUri()?.file ?: return
            var path = f.toPath()
            var depth = 0
            while (path.exists() && Files.isSymbolicLink(path) && depth <= 6) {
                path = Files.readSymbolicLink(path)
                depth++
            }
            Desktop.getDesktop().browse(path.toUri())
        }
    }

    private fun openFile(fileName: String, file: FileInfo) {
        runCatching {
            if (file.fileType == FileType.RegularFile) {
                Desktop.getDesktop().browseFileDirectory(file.getFileUri()?.file ?: return@runCatching)
            } else {
                val f = file.getFileUri()?.file ?: return
                var path = f.toPath()
                var depth = 0
                while (path.exists() && Files.isSymbolicLink(path) && depth <= 6) {
                    path = Files.readSymbolicLink(path)
                    depth++
                }
                Desktop.getDesktop().open(path.toFile())
            }
        }
    }

    override suspend fun launch(fileName: String, fileInfo: FileInfo) {
        when(fileInfo.fileType) {
            FileType.HTML -> openUrl(fileName, fileInfo)
            else -> openFile(fileName, fileInfo)
        }
    }

    override suspend fun launch(albumItemRelation: AlbumItemRelation) {
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