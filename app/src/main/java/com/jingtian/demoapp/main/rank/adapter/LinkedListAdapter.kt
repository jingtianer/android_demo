package com.jingtian.demoapp.main.rank.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.jingtian.demoapp.main.base.BaseAdapter
import com.jingtian.demoapp.main.base.BaseViewHolder
import com.jingtian.demoapp.main.dp
import com.jingtian.demoapp.main.rank.activity.RankItemActivity
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import com.jingtian.demoapp.main.rank.model.RankItemRankType
import kotlin.math.max

class LinkedListAdapter(private val context: Context) : BaseAdapter<ModelRankItem?>() {
    private val dataMappedList = HashMap<Int, ArrayList<ModelRankItem>>()
    private var maxTypeCount = 0
    private var typeNum = RankItemRankType.entries.size - 1

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_EMPTY = 1
        private const val ITEM_SIZE = 150f
        private const val TAG = "LinkedListAdapter"
    }

    inner class EmptyHolder : BaseViewHolder<ModelRankItem?>(View(context)) {
        init {
            itemView.layoutParams = GridLayoutManager.LayoutParams(ITEM_SIZE.dp.toInt(), GridLayoutManager.LayoutParams.MATCH_PARENT)
        }
        override fun onBind(data: ModelRankItem?, position: Int) {

        }
    }

    inner class ImageHolder(private val parent: ViewGroup) : BaseViewHolder<ModelRankItem?>(ImageView(context)) {
        private val image: ImageView = itemView as ImageView
        init {
            itemView.layoutParams = GridLayoutManager.LayoutParams(ITEM_SIZE.dp.toInt(), GridLayoutManager.LayoutParams.MATCH_PARENT)
            image.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        override fun onBind(data: ModelRankItem?, position: Int) {
            data ?: return
            data.image.loadImage(image, maxWidth = ITEM_SIZE.dp.toInt())
            parent.post {
//                itemView.layoutParams.height = parent.height / typeNum
//                image.layoutParams.height = parent.height / typeNum
//                itemView.layoutParams = itemView.layoutParams
//                image.layoutParams = image.layoutParams
            }
            itemView.setOnClickListener {
                RankItemActivity.startActivity(
                    context,
                    rankName = data.rankName,
                    rankItemName = data.itemName,
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<ModelRankItem?> {
        return when(viewType) {
            TYPE_NORMAL -> {
                ImageHolder(parent)
            }
            else -> {
                EmptyHolder()
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<ModelRankItem?>, position: Int) {
        val type = (position % typeNum)
        val pos = (position / typeNum)
        Log.d(TAG, "onBindViewHolder: type=$type, pos=$pos, position=$position, holder=${holder.javaClass.simpleName}, maxTypeCount=$maxTypeCount")

        val data = dataMappedList[type]?.getOrNull(pos) ?: return
        holder.onBind(data, position)
    }

    override fun getItemViewType(position: Int): Int {
        val type = (position % typeNum)
        val pos = (position / typeNum)
        val count = dataMappedList[type]?.size ?: 0
        return if (pos >= count) {
            TYPE_EMPTY
        } else {
            TYPE_NORMAL
        }
    }

    override fun setDataList(list: List<ModelRankItem?>) {
        for (item in list) {
            item ?: continue
            val index = item.rankType.index
            if (index < 0) {
                continue
            }
            val subDataList = dataMappedList.getOrPut(index) {
                ArrayList()
            }
            subDataList.add(item)
            maxTypeCount = max(maxTypeCount, subDataList.size)
        }
        super.setDataList(list)
    }

    override fun appendAll(data: List<ModelRankItem?>) {
        super.appendAll(data)
    }

    override fun append(data: ModelRankItem?) {
        super.append(data)
    }

    override fun getItemCount(): Int {
        return maxTypeCount * typeNum
    }
}