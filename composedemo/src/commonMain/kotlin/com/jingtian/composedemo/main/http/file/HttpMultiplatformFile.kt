package com.jingtian.composedemo.main.http.file

import androidx.compose.ui.graphics.ImageBitmap
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.main.http.api.HttpServerApi
import com.jingtian.composedemo.main.http.api.api
import com.jingtian.composedemo.main.http.dao.HttpServerEntity
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.utils.copyTo
import com.jingtian.composedemo.utils.ensureFileAndParentExists
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.utils.io.core.toByteArray
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class HttpMultiplatformFile(
    val httpServerEntity: HttpServerEntity,
    val albumItemRelation: AlbumItemRelation,
    val api: HttpServerApi = httpServerEntity.api(),
) : MultiplatformFile {
    companion object {
        private val tempFileStore = Path(getFileCacheStorageRootDir(), "http_cache")
    }
    override suspend fun fileName(): String? {
        return albumItemRelation.albumItem.itemName
    }

    override suspend fun isHidden(): Boolean {
        return false
    }

    override suspend fun mediaType(): FileType {
        return albumItemRelation.fileInfo.fileType
    }

    override suspend fun extension(): String {
        return albumItemRelation.fileInfo.filePath.split(".").lastOrNull() ?: ""
    }

    override suspend fun inputStream(): RawSource? {
        return SystemFileSystem.source(initFile())
    }

    override suspend fun fileStoreInputStream(): RawSource? {
        val buffer = Buffer()
        buffer.write(api.getFile(albumItemRelation.fileInfo.filePath).toByteArray())
        return buffer
    }

    override suspend fun imageThumbnail(scaleFactor: Int): ImageBitmap? {
        return null
    }

    override suspend fun videoThumbnail(): ImageBitmap? {
        return null
    }

    override suspend fun audioThumbnail(): ImageBitmap? {
        return null
    }

    override suspend fun imageRatio(): Pair<Int, Int> {
        return 1 to 1
    }

    lateinit var realFile: Path

    private suspend fun initFile(): Path {
        if (::realFile.isInitialized) {
            return realFile
        }
        val tempFileStore = Path(tempFileStore, "${albumItemRelation.albumItem.albumId}", "${albumItemRelation.albumItem.itemId}", "${albumItemRelation.albumItem.fileId}")
        tempFileStore.ensureFileAndParentExists()
        api.getFileSource(albumItemRelation.fileInfo.filePath)?.buffered()?.copyTo(tempFileStore)
        realFile = tempFileStore
        return tempFileStore
    }

    override suspend fun file(): Path? {
        return initFile()

    }

    override suspend fun path(): String {
        return api.getFile(albumItemRelation.fileInfo.filePath)
    }

    override suspend fun platformFile(): PlatformFile? {
        return PlatformFile(initFile())
    }
}