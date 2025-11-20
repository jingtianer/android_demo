package com.jingtian.demoapp.main.widget.appbar

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.State
import java.util.LinkedList
import java.util.TreeMap
import kotlin.math.max
import kotlin.math.min

class AppBarLayoutManager(private val parent: RecyclerView): RecyclerView.LayoutManager() {

    companion object {
        fun View.createDefaultLayoutParam(width: Int, height: Int, isScrollable: Boolean = true): AppBarLayoutParams {
            val layoutParams = layoutParams
            val lp = if (layoutParams == null) {
                AppBarLayoutParams(width, height)
            } else if (layoutParams is AppBarLayoutParams) {
                layoutParams
            } else {
                AppBarLayoutParams(layoutParams)
            }
            lp.isScrollable = isScrollable
            lp.width = width
            lp.height = height
            return lp
        }
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return AppBarLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams?): RecyclerView.LayoutParams {
        return AppBarLayoutParams(lp)
    }

    override fun generateLayoutParams(
        c: Context?,
        attrs: AttributeSet?
    ): RecyclerView.LayoutParams {
        return AppBarLayoutParams(c, attrs)
    }

    override fun canScrollHorizontally(): Boolean {
        return false
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun computeVerticalScrollOffset(state: State): Int {
        return LayoutInfo.offset
    }

    override fun computeVerticalScrollRange(state: State): Int {
        val size = LayoutInfo.lastVisibleOffset - LayoutInfo.firstVisibleOffset
        if (size == 0) {
            return 0
        }
        return (boundsBottom - realBoundsTop) / size * (itemCount - LayoutInfo.lastVisibleOffset)
    }

    override fun onLayoutChildren(recycler: Recycler, state: State) {
        detachAndScrapAttachedViews(recycler)
        if (LayoutInfo.inited) {
//            adjustViews(recycler)
//            layoutViews()
            adjustView1(recycler)
        } else {
            fill(recycler)
        }
    }

    private fun adjustViewList(recycler: Recycler) {
        val list = ArrayList(LayoutInfo.viewList)
        if (list.size > 0) {
            list[0].updateInfo(recycler)
            var index = 0
            while (index < list.size - 1) {
                val info = list[index]
                val next = list[index + 1]
                next.updateInfo(recycler)
                if (next.position != info.position + 1) {
                    val view = recycler.getViewForPosition(info.position + 1)
                    (view.layoutParams as AppBarLayoutParams).isItemChanged
                    addView(view)
                    list.add(index + 1, ItemData(view, info.position + 1))
                }
                index++
            }
        }
        LayoutInfo.viewList.clear()
        LayoutInfo.viewList.addAll(list)
    }

    private fun adjustNoScrollViewList(recycler: Recycler) {
        val list = ArrayList(LayoutInfo.noScrollList)
        list.getOrNull(0)?.updateInfo(recycler)
        var index = 0
        while (index < list.size - 1) {
            val info = list[index]
            val next = list[index + 1]
            next.updateInfo(recycler)
            var viewAdded = 0
            for (position in (info.position + 1) until next.position) {
                val view = recycler.getViewForPosition(position)
                if (!view.isScrollable) {
                    viewAdded++
                    list.add(index + viewAdded, ItemData(view, position))
                } else {
                    recycler.recycleView(view)
                }
            }
            index += viewAdded + 1
        }
        LayoutInfo.noScrollList.clear()
        LayoutInfo.noScrollList.addAll(list)
    }
//
//    fun isScrollable(position: Int): Boolean {
//        return LayoutInfo.isScrollableInfoList[position]
//    }

    private fun adjustView1(recycler: Recycler) {
        val viewCache: TreeMap<Int, ItemData> = TreeMap()
        var ite = LayoutInfo.viewList.iterator()
        val anchorInViewList = IntArray(2) { Int.MAX_VALUE }
        val anchorNoScrollViewList = IntArray(2) { -1 }
        LayoutInfo.visibleItems.clear()
        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        while (ite.hasNext()) {
            val info = ite.next()
            if (info.position.isValidPosition()) {
                info.updateInfo(recycler)
                if (info.position.isValidPosition()) {
                    viewCache[info.position] = info
                    if (info.isScrollable) {
                        anchorInViewList[0] = min(anchorInViewList[0], info.position)
                    } else {
                        anchorInViewList[1] = min(anchorInViewList[1], info.position)
                    }
                } else {
//                    removeAndRecycleView(info.view, recycler)
                }
            } else {
//                removeAndRecycleView(info.view, recycler)
            }
        }
        ite = LayoutInfo.noScrollList.iterator()
        while (ite.hasNext()) {
            val info = ite.next()
            if (info.position.isValidPosition()) {
                info.updateInfo(recycler)
                if (info.position.isValidPosition()) {
                    viewCache[info.position] = info
                    if (info.isScrollable) {
                        anchorNoScrollViewList[0] = max(anchorNoScrollViewList[0], info.position)
                    } else {
                        anchorNoScrollViewList[1] = max(anchorNoScrollViewList[1], info.position)
                    }
                } else {
//                    removeAndRecycleView(info.view, recycler)
                }
            } else {
//                removeAndRecycleView(info.view, recycler)
            }
        }
        LayoutInfo.noScrollList.clear()
        LayoutInfo.viewList.clear()
        var lastItem: ItemData? = null
        val noScrollEnd: Int = (min(anchorInViewList[1], anchorInViewList[0]) - 1).takeIf { it < itemCount } ?: anchorNoScrollViewList[1].takeIf { it < itemCount } ?: 0
        val scrollStart: Int = if (anchorInViewList[0] >= itemCount) noScrollEnd + 1 else anchorInViewList[0]
        Log.d("jingtian", "adjustView: noScrollEnd=$noScrollEnd, scrollStart=$scrollStart")
        for (position in 0 .. noScrollEnd) {
            val viewCacheItem = viewCache[position]
            if (viewCacheItem != null && !viewCacheItem.isScrollable) {
                LayoutInfo.noScrollList.addLast(viewCacheItem)
                if (lastItem == null) {
                    viewCacheItem.offset = 0
                }
                viewCacheItem.measure().below(lastItem)
                LayoutInfo.visibleItems.put(position, viewCacheItem)
                viewCache.remove(position)
                lastItem = viewCacheItem
            } else {
                val view = recycler.getViewForPosition(position)
                if (!view.isScrollable) {
                    val item = ItemData(view, position)
                        .measure()
                        .below(lastItem)
                    lastItem = item
                    LayoutInfo.visibleItems.put(position, item)
                    LayoutInfo.noScrollList.addLast(item)
                } else {
                    recycler.recycleView(view)
                }
            }
        }
        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        if (scrollStart.isValidPosition()) {
            val lastItem = viewCache[scrollStart]?.apply {
                measure()
                if (topHasMoreSpace()) {
                    LayoutInfo.noScrollList.lastOrNull()?.let {
                        below(it)
                    } ?: run {
                        offset = 0
                    }
                }
                viewCache.remove(scrollStart)
            } ?: ItemData(recycler.getViewForPosition(scrollStart), scrollStart)
                .measure()
                .below(LayoutInfo.noScrollList.lastOrNull())

            LayoutInfo.viewList.addLast(lastItem)
            var currentItem: ItemData = lastItem
            while (currentItem.bottomHasMoreSpace()) {
                val position = currentItem.position.nextPosition()
                if (!position.isValidPosition()) {
                    break
                }
                val viewCacheItem = viewCache[position]
                if (viewCacheItem != null && !viewCacheItem.isScrollable) {
                    LayoutInfo.viewList.addLast(viewCacheItem)
                    viewCacheItem.measure().below(lastItem)
                    currentItem = viewCacheItem
                    LayoutInfo.visibleItems.put(position, viewCacheItem)
                    viewCache.remove(position)
                } else {
                    val view = recycler.getViewForPosition(position)
                    val item = ItemData(view, position)
                        .measure()
                        .below(currentItem)
                    currentItem = item
                    LayoutInfo.viewList.addLast(item)
                    LayoutInfo.visibleItems.put(position, viewCacheItem)
                }
            }
        }
        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        fillBottomGap(viewCache, recycler)
        viewCache.clear()
        addViews()
    }

    private fun fillBottomGap(viewCache: MutableMap<Int, ItemData>, recycler: Recycler) {
        val delta: Int = LayoutInfo.viewList.lastOrNull()?.bottomMoreSpace()?.takeIf { it > 0 } ?: 0
        if (delta <= 0) {
            return
        }
        var firstItem = LayoutInfo.viewList.firstOrNull() ?: run {
            val lastNoScroll = LayoutInfo.noScrollList.removeLast()
            if (lastNoScroll != null) {
                LayoutInfo.viewList.addLast(lastNoScroll)
            }
            lastNoScroll
        } ?: return
        while (firstItem.topHasMoreSpace(-delta)) {
            var prevItem = LayoutInfo.visibleItems.get(firstItem.position.lastPosition(), null)
            if (prevItem != null && !prevItem.isScrollable) {
                LayoutInfo.noScrollList.removeLast()
                prevItem.offset = firstItem.offset - prevItem.measuredHeight
                firstItem = prevItem
            } else {
                val position = firstItem.position.lastPosition().takeIf { it.isValidPosition() } ?: break
                val viewCacheItem = viewCache[position]
                if (viewCacheItem != null) {
                    LayoutInfo.viewList.addFirst(viewCacheItem)
                    viewCacheItem.measure().above(firstItem)
                    prevItem = viewCacheItem
                    LayoutInfo.visibleItems.put(position, viewCacheItem)
                    viewCache.remove(position)
                } else {
                    val view = recycler.getViewForPosition(position)
                    val item = ItemData(view, position)
                        .measure()
                        .above(firstItem)
                    prevItem = item
                }
                firstItem = prevItem
            }
            LayoutInfo.viewList.addFirst(firstItem)
            LayoutInfo.visibleItems[firstItem.position] = firstItem
        }
        for (item in LayoutInfo.viewList) {
            item.offset += delta
        }
        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
    }

    private fun adjustViews(recycler: Recycler) {
        LayoutInfo.visibleItems.clear()
        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        adjustViewList(recycler)
        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        adjustNoScrollViewList(recycler)

        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        val noScrollLast = LayoutInfo.noScrollList.lastOrNull()?.position
        val viewListFirst = LayoutInfo.viewList.firstOrNull()?.position
        if (noScrollLast != null && viewListFirst != null) {
            for (position in ((noScrollLast + 1) until viewListFirst)) {
                val view = recycler.getViewForPosition(position)
                if (!view.isScrollable) {
                    LayoutInfo.noScrollList.addLast(ItemData(view, position))
                    addView(view)
                } else {
                    recycler.recycleView(view)
                }
            }
        }

        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        val checkOther = LayoutInfo.noScrollList.firstOrNull()?.position ?: 0
        for (position in (0 until checkOther).reversed()) {
            val view = recycler.getViewForPosition(position)
            if (!view.isScrollable) {
                LayoutInfo.noScrollList.addFirst(ItemData(view, position))
                addView(view)
            } else {
                recycler.recycleView(view)
            }
        }

        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        var lastItemData: ItemData? = null
        LayoutInfo.noScrollList.firstOrNull()?.offset = 0
        for (info in LayoutInfo.noScrollList) {
            measureChildWithMargins(info.view, 0, 0)
            info.below(lastItemData)
            LayoutInfo.visibleItems[info.position] = info
            lastItemData = info
        }
        lastItemData = null
        for (info in LayoutInfo.viewList) {
            measureChildWithMargins(info.view, 0, 0)
            info.below(lastItemData)
            LayoutInfo.visibleItems[info.position] = info
            lastItemData = info
        }
        Log.d("jingtian", "adjustViews: noScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
    }

    private fun initViewList(recycler: Recycler, position: Int) {
        val viewList = LayoutInfo.viewList
        val noScrollList = LayoutInfo.noScrollList
        val visibleItems = LayoutInfo.visibleItems
        viewList.clear()
        noScrollList.clear()
        visibleItems.clear()
        var itemData: ItemData = generateItemData(position, recycler) ?: return
        visibleItems[itemData.position] = itemData
        LayoutInfo.firstVisiblePosition = itemData.position
        LayoutInfo.lastVisiblePosition = itemData.position
        if (itemData.isScrollable) {
            viewList.addLast(itemData)
            while (itemData.bottomHasMoreSpace()) {
                LayoutInfo.lastVisibleOffset = itemData.position
                itemData = itemData.fetchDown(recycler) ?: return
                if (itemData.isScrollable) {
                    viewList.addLast(itemData)
                    visibleItems[itemData.position] = itemData
                    break
                }
                noScrollList.addLast(itemData)
                visibleItems[itemData.position] = itemData
            }
        } else {
            noScrollList.addLast(itemData)
        }
        while (itemData.bottomHasMoreSpace()) {
            itemData = itemData.fetchDown(recycler) ?: break
            LayoutInfo.lastVisibleOffset = itemData.position
            viewList.addLast(itemData)
            visibleItems[itemData.position] = itemData
        }
        LayoutInfo.inited = true
    }

    private fun addViews() {
        for (info in LayoutInfo.viewList) {
            info.addView()
            info.requestLayout()
        }

        for (info in LayoutInfo.noScrollList) {
            info.addView()
            info.requestLayout()
        }
    }

    private fun layoutViews() {
        for (info in LayoutInfo.viewList) {
            info.requestLayout()
        }

        for (info in LayoutInfo.noScrollList) {
            info.requestLayout()
        }
    }

    private fun fill(recycler: Recycler) {
        initViewList(recycler, LayoutInfo.firstVisiblePosition)

        layoutViews()
    }

    private fun moveVisibleUp(dy: Int, recycler: Recycler) {
        Log.d("jingtian", "moveVisibleUp: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        val ite = LayoutInfo.viewList.listIterator()
        while (ite.hasNext()) {
            val info = ite.next()
            if (!info.isScrollable && info.shouldMoveToTopView(dy)) {
                ite.remove()
                info.appendToNoScrollViewList()
                Log.d("jingtian", "moveOut = $info moveVisibleUp: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
            } else if (info.shouldMoveOut(dy)) {
                ite.remove()
                removeAndRecycleView(info.view, recycler)
                LayoutInfo.visibleItems.remove(info.position)
            } else {
                info.offset -= dy
            }
        }
    }

    private fun moveVisibleDown(dy: Int, recycler: Recycler) {
        Log.d("jingtian", "moveVisibleDown: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        val ite = LayoutInfo.viewList.iterator()
        while (ite.hasNext()) {
            val info = ite.next()
            if (info.shouldMoveOut(dy)) {
                ite.remove()
                removeAndRecycleView(info.view, recycler)
                LayoutInfo.visibleItems.remove(info.position)
            } else {
                info.offset -= dy
            }
        }
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler?, state: State?): Int {
        if (dy == 0 || recycler == null) {
            return 0
        }
        return if (dy > 0) {
            moveVisibleUp(dy, recycler)
            fillUp(dy, recycler)
        } else {
            moveVisibleDown(dy, recycler)
            fillDown(dy, recycler)
        }.also {
            LayoutInfo.offset += it
            for (info in LayoutInfo.noScrollList) {
                info.reattach(recycler)
            }
            Log.d("jingtian", "scrollVerticallyBy: dy=$dy, ret=$it")
        }
    }

    private fun fillUp(dy: Int, recycler: Recycler): Int {
        Log.d("jingtian", "fillUp: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        var lastItem = LayoutInfo.viewList.lastOrNull() ?: LayoutInfo.noScrollList.lastOrNull() ?: return 0
        while (lastItem.bottomHasMoreSpace()) {
            val viewLast = LayoutInfo.viewList.lastOrNull()
            val noScrollLast = LayoutInfo.noScrollList.lastOrNull()
            val anchorItem = if (viewLast != null && noScrollLast != null) {
                if (viewLast.position > noScrollLast.position) {
                    viewLast
                } else {
                    noScrollLast
                }
            } else viewLast ?: noScrollLast ?: break
            lastItem = anchorItem.fetchDown(recycler) ?: break
            lastItem.requestLayout()
            LayoutInfo.viewList.addLast(lastItem)
            LayoutInfo.visibleItems[lastItem.position] = lastItem
        }
        Log.d("jingtian", "fillUp: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        val offset = lastItem.bottomMoreSpace().takeIf { it > 0 } ?: 0
        for (info in LayoutInfo.viewList) {
            info.offset += offset
            info.applyOffset()
        }
        Log.d("jingtian", "fillUp: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        return dy - offset
    }

    private fun fillDown(dy: Int, recycler: Recycler): Int {
        var firstItem = LayoutInfo.viewList.firstOrNull() ?: run {
            val lastNoScroll = LayoutInfo.noScrollList.removeLast()
            if (lastNoScroll != null) {
                LayoutInfo.viewList.addLast(lastNoScroll)
            }
            lastNoScroll
        } ?: return 0
        while (firstItem.topHasMoreSpace()) {
            var prevItem = LayoutInfo.visibleItems.get(firstItem.position.lastPosition(), null)
            if (prevItem != null && !prevItem.isScrollable) {
                LayoutInfo.noScrollList.removeLast()
                prevItem.offset = firstItem.offset - prevItem.measuredHeight
                firstItem = prevItem
            } else {
                prevItem = firstItem.fetchUp(recycler) ?: break
                prevItem.offset = firstItem.offset - prevItem.measuredHeight
                firstItem = prevItem
                firstItem.requestLayout()
            }
            LayoutInfo.viewList.addFirst(firstItem)
            LayoutInfo.visibleItems[firstItem.position] = firstItem
        }
        Log.d("jingtian", "fillDown: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        val offset = firstItem.topMoreSpace().takeIf { it > 0 } ?: 0
        for (info in LayoutInfo.viewList) {
            info.offset -= offset
            info.applyOffset()
        }
        Log.d("jingtian", "fillDown: dy=$dy\nnoScrollList=${LayoutInfo.noScrollList.toArray().contentDeepToString()}\nviewList=${LayoutInfo.viewList.toArray().contentDeepToString()}")
        return dy + offset
    }



    private fun ItemData.appendToNoScrollViewList() {
        val lastItemData = LayoutInfo.noScrollList.lastOrNull()
        LayoutInfo.noScrollList.addLast(this)
        if (lastItemData == null) {
            this.offset = 0
        } else {
            this.offset = lastItemData.bottom
        }
    }

    object LayoutInfo {
        var inited = false
        val viewList : LinkedList<ItemData> = LinkedList()
        val noScrollList : LinkedList<ItemData> = LinkedList()
        val visibleItems : SparseArray<ItemData> = SparseArray()
        var offset: Int = 0
        var firstVisiblePosition: Int = 0
        var firstVisibleOffset: Int = 0
        var lastVisiblePosition: Int = 0
        var lastVisibleOffset: Int = 0
    }

    private val boundsLeft
        get() = paddingStart
    private val realBoundsTop
        get() = paddingTop
    private val boundsTop
        get() = paddingTop + (LayoutInfo.noScrollList.lastOrNull()?.bottom ?: 0)
    private val boundsRight
        get() = parent.measuredHeight - paddingEnd
    private val boundsBottom
        get() = parent.measuredHeight - paddingBottom

    fun Int.nextPosition(): Int = (this + 1)
    fun Int.lastPosition(): Int = (this - 1)

    private val View.isScrollable: Boolean
        get() = (this.layoutParams as? AppBarLayoutParams)?.isScrollable != false

    inner class ItemData(var view: View, var position: Int, initOffset: Int = 0) {
        var offset: Int = initOffset
            set(value) {
                pendingOffset += value - field
                field = value
            }

        private var pendingOffset = 0

        val isScrollable: Boolean
            get() = view.isScrollable

        val measuredWidth: Int
            get() = getDecoratedMeasuredWidth(view)
        val measuredHeight: Int
            get() = getDecoratedMeasuredHeight(view)

        fun shouldMoveOut(delta: Int = 0): Boolean {
            return (bottom + delta < boundsTop || offset + delta > boundsBottom) //  && isScrollable
        }

        fun shouldMoveToTopView(delta: Int): Boolean {
            return (offset - delta < boundsTop) // && !isScrollable
        }

        fun isVisible(): Boolean {
            return !shouldMoveOut()
        }

        fun topHasMoreSpace(delta: Int = 0, top: Int = boundsTop): Boolean {
            return offset - delta > top
        }

        fun bottomMoreSpace(delta: Int = 0, bottom: Int = boundsBottom): Int {
            return bottom - (offset - delta + measuredHeight)
        }

        fun topMoreSpace(delta: Int = 0, top: Int = boundsTop): Int {
            return offset - delta - top
        }

        fun bottomHasMoreSpace(delta: Int = 0, bottom: Int = boundsBottom): Boolean {
            return offset + measuredHeight - delta < bottom
        }

        fun fetchUp(recycler: Recycler): ItemData? {
            val nextIndex = this.position.lastPosition()
            return generateItemData(nextIndex, recycler) {
                this.offset - it.measuredHeight
            }
        }

        fun fetchDown(recycler: Recycler): ItemData? {
            val nextIndex = this.position.nextPosition()
            return generateItemData(nextIndex, recycler) {
                this.bottom
            }
        }

        val bottom: Int
            get() {
                return offset + measuredHeight
            }

        fun requestLayout(): ItemData {
            layoutDecoratedWithMargins(view,
                0,
                offset,
                measuredWidth,
                offset + measuredHeight
            )
            pendingOffset = 0
            return this
        }

        fun applyOffset() {
            view.offsetTopAndBottom(pendingOffset)
            pendingOffset = 0
        }

        fun below(itemData: ItemData?): ItemData {
            if (itemData != null) {
                offset = itemData.offset + itemData.measuredHeight
            }
            return this
        }

        fun above(itemData: ItemData?): ItemData {
            if (itemData != null) {
                this.offset = itemData.offset - this.measuredHeight
            }
            return this
        }

        override fun toString(): String {
            return "position=${position}, offset=${offset}, height=${measuredHeight}, view=${view.hashCode()}\n"
        }

        fun reattach(recycler: Recycler) {
            removeView(view)
            addView(view)
            measureChildWithMargins(view, 0, 0)
            requestLayout()
        }

        fun measure(): ItemData {
            measureChildWithMargins(view, 0, 0)
            return this
        }

        fun addView(): ItemData {
            addView(view)
            return this
        }
    }

    private fun ItemData.updateInfo(recycler: Recycler) {
        position = getPosition(view)
        if (position.isValidPosition()) {
            view = recycler.getViewForPosition(position)
        }
    }

    private fun Int.isValidPosition() : Boolean {
        return this in 0 until itemCount
    }

    private fun generateItemData(position: Int, recycler: Recycler, getOffset: (View)->Int = { 0 }): ItemData? {
        if (position.isValidPosition()) {
            val view = recycler.getViewForPosition(position)
            addView(view)
            measureChildWithMargins(view, 0, 0)
            return ItemData(view, position, getOffset(view))
        }
        return null
    }

    class AppBarLayoutParams : RecyclerView.LayoutParams {
        constructor(c: Context?, attrs: AttributeSet?) : super(c, attrs)
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: ViewGroup.MarginLayoutParams?) : super(source)
        constructor(source: ViewGroup.LayoutParams?) : super(source)
        constructor(source: RecyclerView.LayoutParams?) : super(source)

        var isScrollable: Boolean = true
    }
}