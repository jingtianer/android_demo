import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.share.ShareUtils
import com.jingtian.composedemo.viewmodels.albumViewModel
import com.jingtian.composedemo.viewmodels.notifyChange
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

// 根目录，对应 python --directory，自行修改
private const val PORT = 8000
private const val METADATA_PATH = ".composeDemoMetadata"
private const val METADATA_FILE = ".metadata.json"
fun startServer() {
    val server = HttpServer.create(InetSocketAddress("::", PORT), 0)
    server.createContext("/", RouteHandler().apply {
        addHandler("/getFile", GetFileHandler())
        addHandler("/getAlbumList", GetAlbumListHandler())
        addHandler("/listDir", ListAlbumHandler())
        addHandler("/updateItem", UpdateItemHandler())
    })
    server.executor = null
    server.start()
}

private enum class HttpMethodType {
    GET, POST;
}

private abstract class RequestHandler(val methodType: HttpMethodType) {
    abstract fun handle(exchange: HttpExchange, params: Map<String, String>, ): Boolean
}

private class ListAlbumHandler() : RequestHandler(HttpMethodType.GET) {
    val json = Json { ignoreUnknownKeys = true }
    override fun handle(exchange: HttpExchange, params: Map<String, String>): Boolean {
        val albumId = params["albumId"]?.toLongOrNull()
        val offset = params["offset"]?.toLongOrNull()
        val limit = params["limit"]?.toLongOrNull() ?: 10L

        if (albumId == null || offset == null) {
            sendText(exchange, 404, "invalid param: rootPath: $albumId")
            return false
        }

        val itemList = runBlocking {
            DataBase.dbImpl.getAlbumItemDao().getAllAlbumItemWithExtraLimitN(albumId = albumId, limit = limit, offset = offset)
        }
        handleGetFileInfo(exchange, json.encodeToString(itemList))

        return true
    }

}



private fun handleGetFileInfo(exchange: HttpExchange, json: String) {
    val bytes = json.toByteArray(StandardCharsets.UTF_8)
    exchange.responseHeaders["Content-Type"] = "application/json;charset=utf-8"
    exchange.sendResponseHeaders(200, bytes.size.toLong())
    exchange.responseBody.write(bytes)
}

private class UpdateItemHandler() : RequestHandler(HttpMethodType.POST) {
    override fun handle(exchange: HttpExchange, params: Map<String, String>): Boolean {

        val fileInfo = ShareUtils.json.decodeFromStream<AlbumItemRelation>(exchange.requestBody)
        CoroutineUtils.runIOTask({
            val itemId = fileInfo.albumItem.itemId?.takeIf { it != -1L }
            if (itemId != null) {
                DataBase.dbImpl.getAlbumItemDao().updateAlbumItem(fileInfo.albumItem)
                DataBase.dbImpl.getLabelInfoDao().deleteAllLabel(itemId)
                DataBase.dbImpl.getLabelInfoDao().insertAllLabel(fileInfo.labelInfos)
                albumViewModel().albumItemListChange.notifyChange()
            }
        })
        handleGetFileInfo(exchange, "")

        return true
    }
}

private class GetAlbumListHandler() : RequestHandler(HttpMethodType.GET) {
    val json = Json { ignoreUnknownKeys = true }
    @OptIn(ExperimentalSerializationApi::class)
    override fun handle(exchange: HttpExchange, params: Map<String, String>): Boolean {
        try {
            val albumList =runBlocking {
                json.encodeToString(DataBase.dbImpl.getAlbumDao().getAllAlbum())
            }
            handleGetFileInfo(exchange, albumList)
        } catch (e: Exception) {
            sendText(exchange, 500, "error: getFileInfo: ${e.message}")
        }

        return true
    }
}

private class GetFileHandler() : RequestHandler(HttpMethodType.GET) {
    override fun handle(exchange: HttpExchange, params: Map<String, String>): Boolean {
        val relPath = params["path"]

        if (relPath.isNullOrEmpty()) {
            sendText(exchange, 404, "invalid param: path")
            return false
        }

        val targetFile = File(relPath)

        if (!targetFile.exists()) {
            sendText(exchange, 404, "File not found")
            return false
        }

        handleGetFile(exchange, targetFile)

        return true
    }

    /** /getFile?path=xxx 返回二进制流 */
    private fun handleGetFile(exchange: HttpExchange, file: File) {
        if (file.isDirectory) {
            sendText(exchange, 400, "path is directory")
            return
        }
        val mime = guessMime(file.name)
        exchange.responseHeaders["Content-Type"] = mime
        exchange.sendResponseHeaders(200, file.length())
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            var len: Int
            while (fis.read(buf).also { len = it } != -1) {
                exchange.responseBody.write(buf, 0, len)
            }
        }
    }

    private fun guessMime(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "html" -> "text/html;charset=utf-8"
            "txt" -> "text/plain;charset=utf-8"
            "json" -> "application/json;charset=utf-8"
            else -> "application/octet-stream"
        }
    }
}

private class RouteHandler : HttpHandler {
    private val handlers = hashMapOf<HttpMethodType, MutableMap<String, MutableList<RequestHandler>>>()
    fun addHandler(path: String, requestHandler: RequestHandler) {
        handlers.getOrPut(requestHandler.methodType) { hashMapOf() }.getOrPut(path) { arrayListOf() } .add(requestHandler)
    }
    override fun handle(exchange: HttpExchange) {
        try {
            val method = exchange.requestMethod.lowercase()
            val uri = exchange.requestURI
            val rawPath = uri.path
            val query = uri.query ?: ""
            val params = parseQuery(query)
            val methodType = when(method) {
                "get" -> {
                    HttpMethodType.GET
                }
                "post" -> {
                    HttpMethodType.POST
                }
                else -> {
                    null
                }
            } ?: throw IllegalArgumentException("Method: $method Not Allowed")

            val handlerList = handlers[methodType]?.get(rawPath)

            if (handlerList.isNullOrEmpty()) {
                throw RuntimeException("handler list for method: $method, path: $rawPath is empty")
            }

            for (handler in handlerList) {
                if (handler.handle(exchange, params)) {
                    return
                }
            }

            throw RuntimeException("no subscriber handle")
        } catch (e: IllegalArgumentException) {
            sendText(exchange, 405, "${e.message}")
        } catch (e: Exception) {
            sendText(exchange, 500, "internal error: ${e.message}")
        } finally {
            exchange.close()
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        query.split("&").forEach { pair ->
            val kv = pair.split("=", limit = 2)
            if (kv.size == 2) {
                val k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name())
                val v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name())
                map[k] = v
            }
        }
        return map
    }
}

private fun sendText(exchange: HttpExchange, code: Int, text: String) {
    val bytes = text.toByteArray(StandardCharsets.UTF_8)
    exchange.responseHeaders["Content-Type"] = "text/plain;charset=utf-8"
    exchange.sendResponseHeaders(code, bytes.size.toLong())
    exchange.responseBody.write(bytes)
}