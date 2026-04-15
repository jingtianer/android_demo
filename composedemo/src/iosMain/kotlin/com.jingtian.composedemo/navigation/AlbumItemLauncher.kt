package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.utils.delete
import com.jingtian.composedemo.utils.deleteRecursively
import com.jingtian.composedemo.utils.exists
import com.jingtian.composedemo.utils.globalWorkDir
import com.jingtian.composedemo.utils.isDirectory
import com.jingtian.composedemo.utils.isFile
import com.jingtian.composedemo.utils.mkdirs
import kotlinx.io.files.Path

class AlbumItemLauncher : IAlbumItemLauncher {
    companion object {
        private val linkDir = Path(globalWorkDir, "tmp/links")
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
            }
        }
    }
    private fun openUrl(fileName: String, file: FileInfo) {
        runCatching {
            val f = file.getFileUri()?.file ?: return
        }
    }

    private fun openFile(fileName: String, file: FileInfo) {
        runCatching {
            if (file.fileType == FileType.RegularFile) {
//                Desktop.getDesktop().browseFileDirectory(file.getFileUri()?.file ?: return@runCatching)
            } else {
//                val f = file.getFileUri()?.file ?: return
//                var path = f.toPath()
//                var depth = 0
//                while (path.exists() && Files.isSymbolicLink(path) && depth <= 6) {
//                    path = Files.readSymbolicLink(path)
//                    depth++
//                }
//                Desktop.getDesktop().open(path.toFile())
            }
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