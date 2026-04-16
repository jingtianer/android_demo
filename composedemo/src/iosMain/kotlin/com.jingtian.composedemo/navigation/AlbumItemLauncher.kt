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
        docController.presentPreviewAnimated(true)
    }

    override suspend fun launch(fileName: String, fileInfo: FileInfo) {
        when(fileInfo.fileType) {
            FileType.HTML -> openFile(fileName, fileInfo)
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

class OpenFileDelegate : NSObject(), UIDocumentInteractionControllerDelegateProtocol {

    override fun documentInteractionControllerViewControllerForPreview(controller: UIDocumentInteractionController): UIViewController {
        return UIApplication.sharedApplication.keyWindow?.rootViewController!!
    }
}
