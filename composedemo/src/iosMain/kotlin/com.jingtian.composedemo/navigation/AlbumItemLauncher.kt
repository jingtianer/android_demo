package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.copyTo
import com.jingtian.composedemo.utils.createNewFile
import com.jingtian.composedemo.utils.delete
import com.jingtian.composedemo.utils.deleteRecursively
import com.jingtian.composedemo.utils.exists
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.utils.globalWorkDir
import com.jingtian.composedemo.utils.isDirectory
import com.jingtian.composedemo.utils.isFile
import com.jingtian.composedemo.utils.mkdirs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentInteractionController
import platform.UIKit.UIDocumentInteractionControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

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

        fun FileInfo.browse(fileName: String) {
            runCatching {
                val uri = this.getFileUri() ?: return@runCatching
                // iOS 端直接通过 UIApplication 打开本地文件 / 链接
                val url = NSURL.fileURLWithPath(uri.path)
                UIApplication.sharedApplication().openURL(url)
            }
        }
    }
    private fun openUrl(fileName: String, file: FileInfo) {
        runCatching {
//            println("openFile: file:${file.fileType.name}, ${file.getFileUri()?.file}")
            val f = file.getFileUri()?.file ?: return@runCatching
            val url = NSURL.fileURLWithPath(f.toString())
//            println("openFile: url:$url")
            UIApplication.sharedApplication().openURL(url)
        }
    }

    private suspend fun openFile(fileName: String, file: FileInfo) {
//        println("openFile: file:${file.fileType.name}, ${file.getFileUri()}, ${file.getFileUri()?.file}")
        val filePath = withContext(Dispatchers.IO) {
            file.getFileUri()?.file ?: return@withContext null
        } ?: return
//        println("openFile: file: $filePath")

        // 1. 创建文档控制器
        val docController = UIDocumentInteractionController()

        docController.setURL(NSURL.fileURLWithPath(filePath.toString()))

        docController.delegate = OpenFileDelegate()

        // 2. ✅ 关键：手动指定 UTI（文件类型）
//        docController.UTI = when(file.fileType) {
//            FileType.RegularFile -> {
//                ""
//            }
//            FileType.VIDEO -> {
//                "public.movie"
//            }
//            FileType.IMAGE -> {
//                "public.image"
//            }
//            FileType.AUDIO -> {
//                "public.audio"
//            }
//            FileType.HTML -> {
//                "public.plain-text"
//            }
//        }

//        println("uti=${docController.UTI}")

        // 3. 弹出系统预览/打开菜单
        docController.presentPreviewAnimated(true)
    }

    override suspend fun launch(fileName: String, fileInfo: FileInfo) {
        when(fileInfo.fileType) {
            FileType.HTML -> openUrl(fileName, fileInfo)
            else -> openFile(fileName, fileInfo)
        }
    }

    override suspend  fun launch(albumItemRelation: AlbumItemRelation) {
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

private class OpenFileDelegate : NSObject(), UIDocumentInteractionControllerDelegateProtocol {

    override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController {
        return UIApplication.sharedApplication.keyWindow?.rootViewController!!
    }
}
