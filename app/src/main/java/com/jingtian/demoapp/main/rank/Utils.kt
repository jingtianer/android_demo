package com.jingtian.demoapp.main.rank

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.jingtian.demoapp.R
import com.jingtian.demoapp.main.IOUtils
import com.jingtian.demoapp.main.IOUtils.readAndBlock
import com.jingtian.demoapp.main.IOUtils.writeInt
import com.jingtian.demoapp.main.Quadruple
import com.jingtian.demoapp.main.StorageUtil
import com.jingtian.demoapp.main.TimeTracer
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.partitionIndexed
import com.jingtian.demoapp.main.rank.dao.RankDatabase
import com.jingtian.demoapp.main.rank.model.DateTypeConverter
import com.jingtian.demoapp.main.rank.model.ModelItemComment
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.ModelRankUser
import com.jingtian.demoapp.main.rank.model.RankItemImage
import com.jingtian.demoapp.main.rank.model.RankItemImageTypeConverter
import com.jingtian.demoapp.main.rank.model.RankItemRankTypeConverter
import com.jingtian.demoapp.main.tryForEach
import com.jingtian.demoapp.main.widget.StarRateView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Collections
import java.util.Date
import java.util.LinkedList
import java.util.PriorityQueue
import java.util.TreeMap
import java.util.TreeSet
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

object Utils {

    object Share {
        private const val MaskReadImage: Byte = 1
        private const val MaskReadComment: Byte = 2
        private const val MaskReadUserImage: Byte = 3
        private const val MaskReadUserJson: Byte = 4
        private const val MaskReadRankJson: Byte = 5

        private var processingRead = AtomicLong(0)

        fun readShareRankItemList(uri: Uri): List<ModelRankItem> {
            return TimeTracer.trace("readShareRankItemList") {
                realReadShareRankItemList(uri)
            }
        }

        private fun realReadShareRankItemList(uri: Uri): List<ModelRankItem> {
            if (processingRead.get() != 0L) {
                return listOf()
            }
            processingRead.incrementAndGet()
            val jsons = ConcurrentLinkedQueue<String>()
            val imagesConcurrentHashMap: ConcurrentHashMap<Long, RankItemImage> = ConcurrentHashMap()
            val comments = ConcurrentLinkedQueue<String>()
            val users = ConcurrentLinkedQueue<String>()
            val producers = HashMap<Pair<Byte, Long>, CoroutineUtils.Producer<Pair<Int, Int>, ByteArray>>()
            val blockingQueue = LinkedBlockingQueue<Any>()
            app.contentResolver.openInputStream(uri)?.let { `is` ->
                ZipInputStream(`is`).use { zis ->
                    var nextEntry = zis.nextEntry
                    while (nextEntry != null) {
                        val nameSplit = nextEntry.name.split("-")
                        if (nameSplit.size < 3) {
                            Log.d("TAG", "realReadShareRankItemList: unexpected fileName = ${nextEntry.name}")
                            zis.closeEntry()
                            nextEntry = zis.nextEntry
                            continue
                        }
                        val (taskCode, index, blockIndex, end) = try {
                            Quadruple(nameSplit[0].toByte(), nameSplit[1].toLong(), nameSplit[2].toInt(), nameSplit.getOrNull(3)?.toInt() ?: 0)
                        } catch (ignore : Exception) {
                            zis.closeEntry()
                            nextEntry = zis.nextEntry
                            continue
                        }
                        val producer = producers.getOrPut(taskCode to index) {
                            Utils.CoroutineUtils.startProduce<Pair<Int, Int>, ByteArray>(16).ioProducer { queue->
                                var poll = queue.poll()
                                val blockIdHeap = PriorityQueue<Pair<Pair<Int, Int>, ByteArray>> { a, b->
                                    a.first.first - b.first.first
                                }
                                val bos = IOUtils.FastByteArrayOutputStream()
                                var nextBlockId = 0
                                var endBlockId = Int.MAX_VALUE
                                while (poll != null) {
                                    val (pollInfo, byteArray) = poll
                                    val (blockId, end) = pollInfo
                                    if (end == 1) {
                                        endBlockId = blockId
                                    }
                                    if (blockId == nextBlockId) {
                                        bos.write(byteArray)
                                        nextBlockId++
                                        var peek = blockIdHeap.peek()
                                        while (peek != null && peek.first.first == nextBlockId) {
                                            blockIdHeap.poll()
                                            bos.write(peek.second)
                                            nextBlockId++
                                            peek = blockIdHeap.peek()
                                        }
                                    } else {
                                        blockIdHeap.add(poll)
                                    }
                                    if (nextBlockId >= endBlockId) {
                                        break
                                    }
                                    poll = queue.poll()
                                }
                                var peek = blockIdHeap.peek()
                                while (peek != null && peek.first.first == nextBlockId) {
                                    blockIdHeap.poll()
                                    bos.write(peek.second)
                                    nextBlockId++
                                    peek = blockIdHeap.peek()
                                }
                                when(taskCode) {
                                    MaskReadImage, MaskReadUserImage -> {
                                        val imageBytes = bos.toByteArray()
                                        imagesConcurrentHashMap[index] = Utils.DataHolder.ImageStorage.storeImage(imageBytes)
                                    }
                                    MaskReadComment -> {
                                        comments.add(bos.toByteArray().decodeToString())
                                    }
                                    MaskReadUserJson -> {
                                        users.add(bos.toByteArray().decodeToString())
                                    }
                                    MaskReadRankJson -> {
                                        jsons.add(bos.toByteArray().decodeToString())
                                    }
                                }
                                blockingQueue.offer(Any())
                            }.build()
                        }
                        producer.offer(blockIndex to end, zis.readBytes())
                        Log.d("Producer", "readShareRankItemList: offer producer $taskCode $index $blockIndex")
                        zis.closeEntry()
                        nextEntry = zis.nextEntry
                    }
                }
            }
            // 通知producers完成，没有更多数据
            producers.forEach { it.value.complete() }

            // 等待producers完成
            repeat(producers.size) {
                blockingQueue.take()
            }
            val images = HashMap<Long, RankItemImage>().apply {
                putAll(imagesConcurrentHashMap)
            }
            jsons.poll()?.let { json->
                Utils.DataHolder.toModelRankItemList(json, images)?.let { list ->
                    val success = try {
                        Utils.DataHolder.rankDB.rankItemDao().insertAll(list).map { it != -1L }
                    } catch (ignore : Exception) {
                        BooleanArray(list.size) { false }.toList()
                    }
                    val (successList, failedList) = list.partitionIndexed { index, _ -> success[index] }
                    val filteredList = successList.sortedByDescending { it.score }
                    Utils.CoroutineUtils.runIOTask({
                        processingRead.incrementAndGet()
                        users.tryForEach { userJson ->
                            val userList = Utils.DataHolder.toModelUserList(userJson, images)
                            val success = try {
                                Utils.DataHolder.rankDB.rankUserDao().insertAll(userList).map { it != -1L }
                            } catch (ignore : Exception) {
                                BooleanArray(userList.size) { false }.toList()
                            }
                            userList
                                .filterIndexed { index, _ -> !success[index] }
                                .tryForEach { Utils.DataHolder.ImageStorage.delete(it.image.id) }
                        }
                        comments.tryForEach { commentJson ->
                            val commentList =
                                Utils.DataHolder.toModelItemCommentList(commentJson, images)
                            Utils.DataHolder.rankDB.rankCommentDao().insertAll(commentList)
                        }
                        processingRead.decrementAndGet()
                    }) {}
                    Utils.CoroutineUtils.runIOTask({
                        failedList.tryForEach { Utils.DataHolder.ImageStorage.delete(it.image.id) }
                    }) {}
                    processingRead.decrementAndGet()
                    return filteredList
                }
            }
            processingRead.decrementAndGet()
            return listOf()
        }

        fun asyncClearShareDir() {
            Utils.CoroutineUtils.runIOTask({
                val shareDir = File(app.filesDir, "share")
                if (!shareDir.exists()) {
                    return@runIOTask
                }
                if (shareDir.isFile) {
                    shareDir.delete()
                    return@runIOTask
                }
                shareDir.listFiles()?.forEach {
                    it.delete()
                }
            }) {}
        }
        fun startShare(rankName: String, data: List<ModelRankItem>): Uri {
            return TimeTracer.trace("startShare") {
                realStartShare(rankName, data)
            }
        }

        private fun realStartShare(rankName: String, data: List<ModelRankItem>): Uri {
            val shareDir = File(app.filesDir, "share")
            if (!shareDir.exists()) {
                shareDir.mkdir()
            } else if (shareDir.exists() && shareDir.isFile) {
                shareDir.delete()
                shareDir.mkdir()
            }
            val filter: (RankItemImage) -> Boolean = {
                it.id != -1L && it.image != Uri.EMPTY
            }
            val bufferSize = DEFAULT_BUFFER_SIZE * 2
            val queueBuilder = Utils.CoroutineUtils.startProduce<Quadruple<Byte, Long, Int, Int>, ByteArray>()
            data.filter { filter(it.image) }.forEachIndexed { index, item ->
//                MaskReadImage to index
                queueBuilder.ioProducer { queue->
                    app.contentResolver.openInputStream(item.image.image)?.use {
                        var blockCnt = 0
                        val buffer = ByteArray(bufferSize)
                        var len = it.readAndBlock(buffer, 0, bufferSize)
                        while (len > 0) {
                            queue.offer(Quadruple(MaskReadImage, item.image.id, blockCnt, 0), buffer.copyOf())
                            len = it.readAndBlock(buffer, 0, bufferSize)
                            blockCnt++
                        }
                        queue.offer(Quadruple(MaskReadImage, item.image.id, blockCnt, 1), byteArrayOf())
                    }
                    queue.complete()
                }
            }
            data.forEachIndexed { index, item ->
//                MaskReadComment to index
                queueBuilder.ioProducer { queue->
                    val commentList = Utils.DataHolder.rankDB.rankCommentDao().getAllComment(item.itemName)
                    val commentJson = Utils.DataHolder.modelItemCommentJson(commentList)
                    queue.offer(Quadruple(MaskReadComment, index.toLong(), 0, 1), commentJson.encodeToByteArray())
                    queue.complete()
                }
            }
//            MaskReadRankJson to 1
            queueBuilder.ioProducer { queue->
                val rankItemJson = Utils.DataHolder.modelRankItem2Json(data).encodeToByteArray()
                queue.offer(Quadruple(MaskReadRankJson, 0L, 0, 1), rankItemJson)
                queue.complete()
            }
            val userList = Utils.DataHolder.rankDB.rankUserDao().getAllUser()
            userList.forEachIndexed { index, user ->
//                MaskReadUserImage to index
                queueBuilder.ioProducer { queue->
                    if (filter(user.image)) {
                        app.contentResolver.openInputStream(user.image.image)?.use {
                            var blockCnt = 0
                            val buffer = ByteArray(bufferSize)
                            var len = it.readAndBlock(buffer, 0, bufferSize)
                            while (len > 0) {
                                queue.offer(Quadruple(MaskReadUserImage, user.image.id, blockCnt, 0), buffer.copyOf())
                                len = it.readAndBlock(buffer, 0, bufferSize)
                                blockCnt++
                            }
                            queue.offer(Quadruple(MaskReadUserImage, user.image.id, blockCnt, 1), buffer.copyOf())
                        }
                    }
                    queue.complete()
                }
            }
//            MaskReadUserJson to 1
            queueBuilder.ioProducer { queue->
                val modelUserJson = Utils.DataHolder.modelUser2Json(userList).encodeToByteArray()
                queue.offer(Quadruple(MaskReadUserJson, 0, 0, 1), modelUserJson)
                queue.complete()
            }
            val queue = queueBuilder.build()
            val file = File.createTempFile("share-${rankName}", ".zip", shareDir)
            ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zos->
                while (true) {
                    val (key, byteArray) = queue.poll() ?: break
                    val (taskCode, index, blockIndex, end) = key
                    val zipEntry = ZipEntry("$taskCode-$index-$blockIndex-$end")
                    zos.putNextEntry(zipEntry)
                    zos.write(byteArray)
                    zos.closeEntry()
                }
            }
            return FileProvider.getUriForFile(
                app,
                app.packageName + ".fileprovider",
                file
            )
        }
    }

    object DataHolder {

        var userName by StorageUtil.StorageNullableString(
            app.getSharedPreferences("rank_user_info", Context.MODE_PRIVATE),
            "user_name",
            null
        )

        val rankDB = Room.databaseBuilder(app, RankDatabase::class.java, "rank_db")
            .addTypeConverter(DateTypeConverter())
            .addTypeConverter(RankItemImageTypeConverter())
            .addTypeConverter(RankItemRankTypeConverter())
            .allowMainThreadQueries()
            .build()

        object ImageStorage {

            private val sp = app.getSharedPreferences("rank-image-store", Context.MODE_PRIVATE)
            private const val RANK_IMAGE_PREFIX = "rank_image_"
            private const val RANK_IMAGE_STORE_DIR = "rank_image"
            private var id by StorageUtil.SynchronizedProperty(
                StorageUtil.StorageLong(sp, "image-id", 0L)
            )

            private val imageCache = HashMap<Long, Uri>()

            fun getImage(id: Long): Uri? {
                if (id == -1L) {
                    Uri.EMPTY
                }
                synchronized(this) {
                    if (imageCache.containsKey(id)) {
                        return imageCache[id] ?: Uri.EMPTY
                    }
                }
                val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
                val storageFile = File(storeDir, RANK_IMAGE_PREFIX + id)
                return if (storageFile.exists()) {
                    storageFile.toUri()
                } else {
                    null
                }
            }

            fun delete(id: Long) {
                synchronized(this) {
                    if (imageCache.containsKey(id)) {
                        imageCache.remove(id)
                    }
                }
                val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
                val storageFile = File(storeDir, RANK_IMAGE_PREFIX + id)
                if (storageFile.exists()) {
                    storageFile.delete()
                }
            }

            fun Uri.safeToFile(): File? {
                return if ("file".equals(scheme, ignoreCase = true)) {
                    path?.let { File(it) }
                } else {
                    null
                }
            }

            fun storeImage(oldId: Long, uri: Uri): Long {
                if (uri == Uri.EMPTY) {
                    return -1
                }
                val id = if (oldId >= this.id || oldId < 0) {
                    synchronized(this) { this.id++ }
                } else {
                    oldId
                }
                val storageFile = getStoreFile(id)
                if (uri.safeToFile()?.absolutePath?.equals(storageFile.absolutePath) != true) {
                    if (storageFile.exists()) {
                        storageFile.delete()
                    }
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        innerStoreImage(id, input, storageFile)
                    }
                }
                return id
            }

            fun storeImage(uri: Uri): Long {
                if (uri == Uri.EMPTY) {
                    return -1
                }
                val id = synchronized(this) {
                    this.id++
                }
                val storageFile = getStoreFile(id)
                storageFile.delete()
                app.contentResolver.openInputStream(uri)?.use { input ->
                    innerStoreImage(id, input, storageFile)
                }
                return id
            }

            fun storeImage(imageBytes: ByteArray): RankItemImage {
                val id = synchronized(this) {
                    this.id++
                }

                val storageFile = getStoreFile(id)
                storageFile.delete()
                val raf = RandomAccessFile(storageFile, "rw")
                val channel = raf.channel
                val fileSize = imageBytes.size.toLong()
                val mappedBuffer: MappedByteBuffer = channel.map(
                    FileChannel.MapMode.READ_WRITE,
                    0,
                    fileSize
                )
                mappedBuffer.put(imageBytes)
                Utils.CoroutineUtils.runIOTaskLowPriority({
                    raf.use { }
                })
                return RankItemImage(id, storageFile.toUri())
            }

            private fun getStoreFile(id: Long): File {
                val storeDir = File(app.filesDir, RANK_IMAGE_STORE_DIR)
                if (!storeDir.exists()) {
                    storeDir.mkdirs()
                } else if (storeDir.isFile) {
                    storeDir.delete()
                    storeDir.mkdirs()
                }
                return File(storeDir, RANK_IMAGE_PREFIX + id)
            }

            private fun innerStoreImage(
                id: Long,
                input: InputStream,
                storageFile: File
            ): RankItemImage {
                storageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                val uri = storageFile.toUri()
                imageCache[id] = uri
                return RankItemImage(id, uri)
            }
        }

        class UriConverter(private val images: HashMap<Long, RankItemImage>) :
            TypeAdapter<RankItemImage>() {
            override fun write(out: JsonWriter, value: RankItemImage) {
                out.value(value.id)
            }

            override fun read(`in`: JsonReader?): RankItemImage {
                `in`?.nextLong()?.let { id ->
                    images[id]?.let { image ->
                        return image
                    }
                }
                return RankItemImage()
            }
        }

        private val rankItemGson: (HashMap<Long, RankItemImage>) -> Gson = { images ->
            GsonBuilder()
                .registerTypeAdapter(Date::class.java, DateTypeConverter())
                .registerTypeAdapter(RankItemImage::class.java, UriConverter(images))
                .create()
        }

        private val modelRankListType = object : TypeToken<List<ModelRank>>() {}
        private val modelRankItemListType = object : TypeToken<List<ModelRankItem>>() {}
        private val modelRankItemCommentListType = object : TypeToken<List<ModelItemComment>>() {}
        private val modelRankUserListType = object : TypeToken<List<ModelRankUser>>() {}

        fun modelUser2Json(any: List<ModelRankUser>): String {
            return rankItemGson(hashMapOf()).toJson(any)
        }

        fun toModelUserList(
            any: String,
            images: HashMap<Long, RankItemImage>
        ): List<ModelRankUser> {
            return rankItemGson(images).fromJson(any, modelRankUserListType.type)
        }

        fun modelItemCommentJson(any: List<ModelItemComment>): String {
            return rankItemGson(hashMapOf()).toJson(any)
        }

        fun toModelItemCommentList(
            any: String,
            images: HashMap<Long, RankItemImage>
        ): List<ModelItemComment> {
            return rankItemGson(images).fromJson(any, modelRankItemCommentListType.type)
        }

        fun modelRankItem2Json(any: List<ModelRankItem>): String {
            return rankItemGson(hashMapOf()).toJson(any)
        }

        fun toModelRankList(json: String): List<ModelRank>? {
            return try {
                rankItemGson(hashMapOf()).fromJson(json, modelRankListType.type) as List<ModelRank>
            } catch (ignore: Exception) {
                null
            }
        }

        fun modelRank2Json(any: List<ModelRank>): String {
            return rankItemGson(hashMapOf()).toJson(any)
        }

        fun toModelRankItemList(
            json: String,
            images: HashMap<Long, RankItemImage>
        ): List<ModelRankItem>? {
            return try {
                rankItemGson(images).fromJson(
                    json,
                    modelRankItemListType.type
                ) as List<ModelRankItem>
            } catch (ignore: Exception) {
                null
            }
        }
    }

    object RecyclerViewUtils {
        class FixNestedScroll(
            private val view: View,
            private val orientation: Int
        ) : View.OnTouchListener {
            private var initX = 0f
            private var initY = 0f
            private var scrolled = false
            private var touchSlop = ViewConfiguration.get(view.context).scaledTouchSlop

            private val Float.intSign: Int
                get() = when {
                    this > 0 -> {
                        1
                    }

                    this < 0 -> {
                        -1
                    }

                    else -> {
                        0
                    }
                }

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        initX = event.x
                        initY = event.y
                        scrolled = false
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!scrolled) {
                            val dx = initX - event.x
                            val dy = initY - event.y
                            val absDx = abs(dx)
                            val absDy = abs(dy)
                            if (orientation == RecyclerView.HORIZONTAL && absDx > absDy && view.canScrollHorizontally(
                                    dx.intSign
                                )
                            ) {
                                scrolled = true
                            } else if (orientation == RecyclerView.VERTICAL && absDy > absDx && view.canScrollVertically(
                                    dy.intSign
                                )
                            ) {
                                scrolled = true
                            } else if (absDy > touchSlop && absDy > touchSlop) {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                }
                return false
            }

        }
    }

    fun StarRateView.commonConfig() {
        updateStarConfig(
            false,
            5,
            3f.dp,
            ResourcesCompat.getDrawable(resources, R.drawable.star_high_lighted, null),
            ResourcesCompat.getDrawable(resources, R.drawable.star, null),
        )
    }

    fun StarRateView.commonScrollableConfig() {
        updateStarConfig(
            true,
            5,
            3f.dp,
            ResourcesCompat.getDrawable(resources, R.drawable.star_high_lighted, null),
            ResourcesCompat.getDrawable(resources, R.drawable.star, null),
        )
    }

    object CoroutineUtils {
        private val globalScope = CoroutineScope(Dispatchers.Main + Job())

        class LowPriorityThreadFactory : ThreadFactory {
            private val group = ThreadGroup("LowPriorityGroup")

            override fun newThread(r: Runnable): Thread {
                val thread = Thread(group, r)
                thread.priority = Thread.MIN_PRIORITY
                thread.isDaemon = true
                return thread
            }
        }

        private val lowPriorityDispatcher: CoroutineDispatcher = Executors.newFixedThreadPool(2, LowPriorityThreadFactory())
            .asCoroutineDispatcher()

        fun <T> runIOTaskLowPriority(block: Callable<T>, callback: (T) -> Unit = {}): Job {
            return globalScope.launch {
                val ret = withContext(lowPriorityDispatcher) {
                    block.call()
                }
                withContext(Dispatchers.Main) {
                    callback.invoke(ret)
                }
            }
        }

        fun <T> runIOTask(block: Callable<T>, callback: (T) -> Unit = {}): Job {
            return globalScope.launch {
                val ret = withContext(Dispatchers.IO) {
                    block.call()
                }
                withContext(Dispatchers.Main) {
                    callback.invoke(ret)
                }
            }
        }

        class Producer<K, T>(private val capacity: Int, private var taskCnt: Int) {
            private val lock: ReentrantLock = ReentrantLock()

            private val notEmpty: Condition = lock.newCondition()

            private val notFull: Condition = lock.newCondition()

            private val queue = LinkedList<Pair<K, T>>()

            private fun notAllComplete(): Boolean {
                return taskCnt > 0
            }

            fun complete() {
                lock.lock()
                try {
                    taskCnt--
//                    Log.d("Producer", "complete, $taskCnt")
                    if (!notAllComplete()) {
//                        Log.d("Producer", "complete, notify all $taskCnt")
                        notEmpty.signalAll()
                    }
                } finally {
                    lock.unlock()
                }
            }

            fun poll(): Pair<K, T>? {
                lock.lock()
                var get: Pair<K, T>?
                try {
                    get = queue.pollFirst()
                    while (get == null) {
                        if (!notAllComplete()) {
                            break
                        }
                        notEmpty.await()
                        get = queue.pollFirst()
                    }
                    notFull.signal()
//                    Log.d("Producer", "poll: $get")
                } finally {
                    lock.unlock()
                }
                return get
            }

            fun offer(k: K, t: T) {
                lock.lock()
                try {
                    var size = queue.size
                    while (size >= capacity) {
                        notFull.await()
                        size = queue.size
                    }
//                    Log.d("Producer", "offer: $k, $t")
                    queue.addLast(k to t)
                    notEmpty.signal()
                } finally {
                    lock.unlock()
                }
            }
        }

        class ProducerBuilder<K, T>(
            private val capacity: Int
        ) {
            private val taskList = mutableListOf<Pair<CoroutineContext, (Producer<K, T>) -> Unit>>()
            fun producer(
                context: CoroutineContext,
                block: (Producer<K, T>) -> Unit
            ): ProducerBuilder<K, T> {
                taskList.add(context to block)
                return this
            }

            fun ioProducer(
                block: (Producer<K, T>) -> Unit
            ): ProducerBuilder<K, T> {
                return producer(Dispatchers.IO, block)
            }

            fun build() : Producer<K, T> {
                val producer = Producer<K, T>(capacity, taskList.size)
                for ((context, block) in taskList) {
                    globalScope.launch {
                        withContext(context) {
                            block(producer)
                        }
                    }
                }
                taskList.clear()
                return producer
            }
        }

        fun <K, T> startProduce(capacity : Int = 1024) = ProducerBuilder<K, T>(capacity)

        fun Context.activityLifecycleLaunch(
            context: CoroutineContext = globalScope.coroutineContext,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            block: suspend CoroutineScope.() -> Unit
        ) {
            val baseActivity = app.activityStack[this]
            if (baseActivity != null) {
                baseActivity.lifecycleScope.launch(context, start, block)
            } else {
                globalScope.launch(context, start, block)
            }
        }
    }
}