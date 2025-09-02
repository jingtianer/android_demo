package com.jingtian.demoapp.main.rank.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.ProvidedTypeConverter
import androidx.room.Relation
import androidx.room.TypeConverter
import com.jingtian.demoapp.R
import com.jingtian.demoapp.main.app
import com.jingtian.demoapp.main.rank.Utils
import com.jingtian.demoapp.main.rank.dao.RankModelItemDao
import kotlin.math.max
import kotlin.math.min

data class RankItemImage(
    var id: Long = -1, var image: Uri = Uri.EMPTY
) {
    fun loadImage(imageView: ImageView, maxWidth: Int = -1, maxHeight: Int = -1) {
        if (image == Uri.EMPTY) {
            return
        }
        Utils.CoroutineUtils.runIOTask({
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true // 只获取尺寸，不加载像素
            }
            val scaleFactor =  app.contentResolver.openInputStream(image)?.use { `is`->
                // 第一步：仅解码边界，获取图片原始宽高
                BitmapFactory.decodeStream(`is`, null, options)
                // 第二步：计算缩放比例（避免图片过大导致 OOM）
                calculateScaleFactor(options.outWidth, options.outHeight, maxWidth, maxHeight)
            } ?: -1
            Log.d("TAG", "loadImage: $id, $scaleFactor, $image")
            return@runIOTask Utils.DataHolder.ImagePool.put(id, scaleFactor) {
                app.contentResolver.openInputStream(image)?.use { `is`->
                    Log.d("TAG", "loadImage failed: $id, $scaleFactor, $image")
                    // 第三步：按缩放比例解码图片
                    options.apply {
                        inJustDecodeBounds = false // 实际加载像素
                        inSampleSize = scaleFactor // 缩放比例（2的倍数，如 2=1/2 大小，4=1/4 大小）
                        inPreferredConfig = Bitmap.Config.RGB_565 // 可选：使用 RGB_565 节省内存（比 ARGB_8888 节省一半）
                    }
                    BitmapFactory.decodeStream(`is`, null, options)
                }
            }
        }) { bitMap->
            if (bitMap != null) {
                imageView.setImageBitmap(bitMap)
            } else {
                imageView.setImageResource(R.drawable.load_failed)
            }
        }
    }

    fun isValid(): Boolean {
        return image != Uri.EMPTY && id != -1L
    }

    private fun calculateScaleFactor(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var widthScaleFactor = 1
        var heightScaleFactor = 1
        if (maxWidth > 0) {
            while (originalWidth / widthScaleFactor > maxWidth) {
                widthScaleFactor *= 2 // 每次翻倍缩放
            }
        }
        if (maxHeight > 0) {
            while (originalHeight / heightScaleFactor > maxHeight) {
                heightScaleFactor *= 2 // 每次翻倍缩放
            }
        }
        return max(1,  max(widthScaleFactor,heightScaleFactor) / 2)
    }
}

@ProvidedTypeConverter
class RankItemImageTypeConverter {
    @TypeConverter
    fun toRankItemImage(id: Long): RankItemImage {
        return Utils.DataHolder.ImageStorage.getImage(id)?.let {
            RankItemImage(id, it)
        } ?: RankItemImage()
    }

    @TypeConverter
    fun toLong(rankItemImage: RankItemImage): Long {
        return rankItemImage.id
    }
}


enum class RankItemRankType(val index: Int = -1, val r: Int, val g: Int, val b: Int, val a: Int = 255) {
    NONE(-1, 0, 0, 0), 夯(0, 0xef, 0x2b, 0x03), 顶尖(1, 0xff, 0xb6, 0x18), 人上人(2, 0x39, 0x8d, 0xff), NPC(3, 0xf9, 0xf5, 0xf2), 史(4, 0xa1, 0xbd, 0xb1)
}

@ProvidedTypeConverter
class RankItemRankTypeConverter {
    @TypeConverter
    fun toRankItemImage(id: Int): RankItemRankType {
        return when(id) {
            RankItemRankType.夯.ordinal -> {
                RankItemRankType.夯
            }
            RankItemRankType.顶尖.ordinal -> {
                RankItemRankType.顶尖
            }
            RankItemRankType.人上人.ordinal -> {
                RankItemRankType.人上人
            }
            RankItemRankType.NPC.ordinal -> {
                RankItemRankType.NPC
            }
            RankItemRankType.史.ordinal -> {
                RankItemRankType.史
            }
            else -> {
                RankItemRankType.NONE
            }
        }
    }

    @TypeConverter
    fun toLong(rankItemImage: RankItemRankType): Int {
        return rankItemImage.ordinal
    }
}

@Entity(
    tableName = RankModelItemDao.TABLE_NAME, foreignKeys = [ForeignKey(
        entity = ModelRank::class,
        parentColumns = arrayOf("rankName"),
        childColumns = arrayOf("rankName"),
        onDelete = ForeignKey.CASCADE
    )], indices = [Index("rankName"), Index("score")]
)
data class ModelRankItem(
    @PrimaryKey var itemName: String = "",
    var rankName: String = "",
    var score: Float = 0f,
    var desc: String = "",
    var rankType: RankItemRankType = RankItemRankType.夯,
    var image: RankItemImage = RankItemImage(),
) {
    companion object {
        fun ModelRankItem.isValid(): Boolean {
            return itemName.isNotEmpty()
        }
    }
}

class RelationRankAndItem(
    @Embedded val rank: ModelRank,

    @Relation(
        parentColumn = "rankName",
        entityColumn = "itemName",
        entity = ModelRankItem::class,
    ) val list: List<ModelRankItem>
)