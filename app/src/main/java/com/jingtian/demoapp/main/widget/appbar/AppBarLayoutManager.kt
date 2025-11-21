package com.jingtian.demoapp.main.widget.appbar

import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.State
import java.util.LinkedList
import kotlin.math.max
import kotlin.math.min

class AppBarLayoutManager(private val adapter: IAppBarAdapter): RecyclerView.LayoutManager() {

    companion object {
        private const val DEBUG = false
        private const val TAG = "jingtian"
    }
    
    private val layoutInfo = LayoutInfo()

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun canScrollHorizontally(): Boolean {
        return false
    }

    override fun canScrollVertically(): Boolean {
        return true
    }

    override fun computeVerticalScrollOffset(state: State): Int {
        return layoutInfo.offset
    }

    override fun computeVerticalScrollRange(state: State): Int {
        val size = layoutInfo.lastVisibleOffset - layoutInfo.firstVisibleOffset
        if (size == 0) {
            return 0
        }
        return (boundsBottom - realBoundsTop) / size * (itemCount - layoutInfo.lastVisibleOffset)
    }

    override fun onLayoutChildren(recycler: Recycler, state: State) {
        detachAndScrapAttachedViews(recycler)
        adjustViews(recycler)
    }

//    fun isScrollable(position: Int): Boolean {
//        return layoutInfo.isScrollableInfoList[position]
//    }

    private fun MutableMap<Int, ItemData>.getAndMeasureViewForPosition(position: Int): ItemData? {
        val viewCacheItem = this[position]
        if (viewCacheItem != null) {
            this.remove(position)
            return viewCacheItem
        }
        return null
    }

    private fun MutableMap<Int, ItemData>.cacheViewList(recycler: Recycler): IntArray {
        val anchorInViewList = IntArray(2) { Int.MAX_VALUE }
        layoutInfo.viewList.forEach { info->
            if (info.updateInfo(recycler)) {
                this[info.position] = info
                if (info.isScrollable) {
                    anchorInViewList[0] = min(anchorInViewList[0], info.position)
                } else {
                    anchorInViewList[1] = min(anchorInViewList[1], info.position)
                }
            }
        }
        layoutInfo.viewList.clear()
        return anchorInViewList
    }

    private fun MutableMap<Int, ItemData>.cacheNoScrollViewList(recycler: Recycler): IntArray {
        val anchorNoScrollViewList = IntArray(2) { -1 }
        layoutInfo.noScrollList.forEach { info->
            if (info.updateInfo(recycler)) {
                this[info.position] = info
                if (info.isScrollable) {
                    anchorNoScrollViewList[0] = max(anchorNoScrollViewList[0], info.position)
                } else {
                    anchorNoScrollViewList[1] = max(anchorNoScrollViewList[1], info.position)
                }
            }
        }
        layoutInfo.noScrollList.clear()
        return anchorNoScrollViewList
    }

    private fun Recycler.unsafeGetMeasuredItemData(position: Int) : ItemData {
        return ItemData(this.getViewForPosition(position), position).measure()
    }

    private fun checkNoScrollViews(recycler: Recycler, noScrollEnd: Int, viewCache: MutableMap<Int, ItemData>) {
        var lastItem: ItemData? = null
        for (position in 0 until noScrollEnd) {
            if (!adapter.getScrollMode(position)) {
                val cachedItem = viewCache.getAndMeasureViewForPosition(position)
                val itemData = if (cachedItem != null) {
                    if (lastItem == null) {
                        cachedItem.offset = 0
                    }
                    cachedItem.below(lastItem)
                } else {
                    recycler.unsafeGetMeasuredItemData(position).below(lastItem)
                }
                layoutInfo.visibleItems.put(position, itemData)
                layoutInfo.noScrollList.addLast(itemData)
                lastItem = itemData
            }
        }
    }

    private fun fillViewList(recycler: Recycler, scrollStart: Int, viewCache: MutableMap<Int, ItemData>) {
        if (scrollStart.isValidPosition()) {
            val noScrollLast = layoutInfo.noScrollList.lastOrNull()
            val cachedItem = viewCache.getAndMeasureViewForPosition(scrollStart)
            val lastItem = if (cachedItem != null) {
//                if (cachedItem.topHasMoreSpace()) {
//                    cachedItem.offset = 0
//                    cachedItem.below(noScrollLast)
//                }
                cachedItem
            } else {
                recycler.unsafeGetMeasuredItemData(scrollStart).below(noScrollLast)
            }
            layoutInfo.viewList.addLast(lastItem)
            var currentItem: ItemData = lastItem
            while (currentItem.bottomHasMoreSpace()) {
                val position = currentItem.position.nextPosition().takeIf { it.isValidPosition() } ?: break
                val itemData = viewCache.getAndMeasureViewForPosition(position)?.below(currentItem)
                    ?: recycler.unsafeGetMeasuredItemData(position).below(currentItem)
                layoutInfo.viewList.addLast(itemData)
                layoutInfo.visibleItems.put(position, itemData)
                currentItem = itemData
            }
        }
    }

    private fun adjustViews(recycler: Recycler) {
        val viewCache: HashMap<Int, ItemData> = HashMap()
        layoutInfo.visibleItems.clear()
        logD { "adjustViews: $this" }
        val anchorInViewList = viewCache.cacheViewList(recycler)
        val anchorNoScrollViewList = viewCache.cacheNoScrollViewList(recycler)
        val noScrollEnd: Int = (min(anchorInViewList[1], anchorInViewList[0]) - 1).takeIf { it < itemCount } ?: anchorNoScrollViewList[1].takeIf { it < itemCount } ?: 0
        val scrollStart: Int = if (anchorInViewList[0] >= itemCount) noScrollEnd + 1 else anchorInViewList[0]

        logD { "adjustViews: noScrollEnd=$noScrollEnd, scrollStart=$scrollStart" }
        logD { "adjustViews: $this, \nviewCache=${viewCache.values.toTypedArray().contentDeepToString()}" }
        checkNoScrollViews(recycler, noScrollEnd, viewCache)

        logD { "adjustViews: $this, \nviewCache=${viewCache.values.toTypedArray().contentDeepToString()}" }
        fillViewList(recycler, scrollStart, viewCache)

        logD { "adjustViews: $this, \niewCache=${viewCache.values.toTypedArray().contentDeepToString()}" }
        val delta = fillBottomGap(viewCache, recycler)
        viewCache.clear()

        addViews(delta)
    }

    private fun fillBottomGap(viewCache: MutableMap<Int, ItemData>, recycler: Recycler): Int {
        val delta: Int = (layoutInfo.viewList.lastOrNull())?.bottomMoreSpace()?.takeIf { it > 0 } ?: 0
        var firstItem = layoutInfo.viewList.firstOrNull() ?: run {
            val lastNoScroll = layoutInfo.noScrollList.removeLastOrNull()
            if (lastNoScroll != null) {
                layoutInfo.viewList.addLast(lastNoScroll)
            }
            lastNoScroll
        } ?: return 0
        while (firstItem.topHasMoreSpace(-delta)) {
            var prevItem = layoutInfo.visibleItems.get(firstItem.position.lastPosition(), null)
            if (prevItem != null && !prevItem.isScrollable) {
                layoutInfo.noScrollList.removeLast()
                prevItem.above(firstItem)
                firstItem = prevItem
            } else {
                val position = firstItem.position.lastPosition().takeIf { it.isValidPosition() } ?: break
                val viewCacheItem = viewCache.getAndMeasureViewForPosition(position)
                prevItem = viewCacheItem?.above(firstItem)
                    ?: recycler.unsafeGetMeasuredItemData(position).above(firstItem)
                firstItem = prevItem
            }
            layoutInfo.viewList.addFirst(firstItem)
            layoutInfo.visibleItems[firstItem.position] = firstItem
        }
        logD { "adjustViews: $this" }
        return delta
    }

    private fun addViews(delta : Int) {
        for (info in layoutInfo.viewList) {
            info.requestLayout(delta).addView()
        }
        for (info in layoutInfo.noScrollList) {
            info.requestLayout().addView()
        }
    }

    private fun moveVisibleUp(dy: Int, recycler: Recycler) {
        val viewList = layoutInfo.viewList
        val visibleItems = layoutInfo.visibleItems
        logD { "moveVisibleUp: dy=$dy\n$this" }
        val ite = viewList.listIterator()
        while (ite.hasNext()) {
            val info = ite.next()
            if (!info.isScrollable && info.shouldMoveToTopView(dy)) {
                ite.remove()
                info.appendToNoScrollViewList()
//                logD(TAG, "moveOut = $info moveVisibleUp: dy=$dy\n$this")
            } else if (info.shouldMoveOut(dy)) {
                ite.remove()
                detachAndScrapView(info.view, recycler)
                visibleItems.remove(info.position)
            } else {
                info.offset -= dy
            }
        }
    }

    private fun moveVisibleDown(dy: Int, recycler: Recycler) {
        val viewList = layoutInfo.viewList
        val visibleItems = layoutInfo.visibleItems
        logD { "moveVisibleDown: dy=$dy\n$this" }
        val ite = viewList.iterator()
        while (ite.hasNext()) {
            val info = ite.next()
            if (info.shouldMoveOut(dy)) {
                ite.remove()
                detachAndScrapView(info.view, recycler)
                visibleItems.remove(info.position)
            } else {
                info.offset -= dy
            }
        }
    }

    override fun scrollVerticallyBy(dy: Int, recycler: Recycler?, state: State?): Int {
        val noScrollList = layoutInfo.noScrollList
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
            layoutInfo.offset += it
            for (info in noScrollList) {
                info.reattach()
            }
            logD { "scrollVerticallyBy: dy=$dy, ret=$it" }
        }
    }

    private fun fillUp(dy: Int, recycler: Recycler): Int {
        val viewList = layoutInfo.viewList
        val noScrollList = layoutInfo.noScrollList
        val visibleItems = layoutInfo.visibleItems
        logD { "fillUp: dy=$dy\n$this" }
        var lastItem = viewList.lastOrNull() ?: noScrollList.lastOrNull() ?: return 0
        while (lastItem.bottomHasMoreSpace()) {
            val viewLast = viewList.lastOrNull()
            val noScrollLast = noScrollList.lastOrNull()
            val anchorItem = if (viewLast != null && noScrollLast != null) {
                if (viewLast.position > noScrollLast.position) {
                    viewLast
                } else {
                    noScrollLast
                }
            } else viewLast ?: noScrollLast ?: break
            lastItem = anchorItem.fetchDown(recycler) ?: break
            lastItem.addView()
            lastItem.requestLayout()
            viewList.addLast(lastItem)
            visibleItems[lastItem.position] = lastItem
        }
        logD { "fillUp: dy=$dy\n$this" }
        val offset = lastItem.bottomMoreSpace().takeIf { it > 0 } ?: 0
        for (info in layoutInfo.viewList) {
            info.offset += offset
            info.applyOffset()
        }
        logD { "fillUp: dy=$dy\n$this" }
        return dy - offset
    }

    private fun fillDown(dy: Int, recycler: Recycler): Int {
        val viewList = layoutInfo.viewList
        val noScrollList = layoutInfo.noScrollList
        val visibleItems = layoutInfo.visibleItems
        var firstItem = layoutInfo.viewList.firstOrNull() ?: run {
            val lastNoScroll = layoutInfo.noScrollList.removeLast()
            if (lastNoScroll != null) {
                viewList.addLast(lastNoScroll)
            }
            lastNoScroll
        } ?: return 0
        while (firstItem.topHasMoreSpace()) {
            var prevItem = visibleItems.get(firstItem.position.lastPosition(), null)
            if (prevItem != null && !prevItem.isScrollable) {
                noScrollList.removeLast()
                prevItem.offset = firstItem.offset - prevItem.measuredHeight
                firstItem = prevItem
            } else {
                prevItem = firstItem.fetchUp(recycler) ?: break
                prevItem.addView()
                prevItem.offset = firstItem.offset - prevItem.measuredHeight
                firstItem = prevItem
                firstItem.requestLayout()
            }
            viewList.addFirst(firstItem)
            visibleItems[firstItem.position] = firstItem
        }
        logD { "fillDown: dy=$dy\n$this" }
        val offset = firstItem.topMoreSpace().takeIf { it > 0 } ?: 0
        for (info in layoutInfo.viewList) {
            info.offset -= offset
            info.applyOffset()
        }
        logD { "fillDown: dy=$dy\n$this" }
        return dy + offset
    }



    private fun ItemData.appendToNoScrollViewList() {
        val lastItemData = layoutInfo.noScrollList.lastOrNull()
        layoutInfo.noScrollList.addLast(this)
        if (lastItemData == null) {
            this.offset = 0
        } else {
            this.offset = lastItemData.bottom
        }
    }
    
    class LayoutInfo {
        val viewList : LinkedList<ItemData> = LinkedList()
        val noScrollList : LinkedList<ItemData> = LinkedList()
        val visibleItems : SparseArray<ItemData> = SparseArray()
        var offset: Int = 0
        var firstVisiblePosition: Int = 0
        var firstVisibleOffset: Int = 0
        var lastVisiblePosition: Int = 0
        var lastVisibleOffset: Int = 0
    }

    private var oldWidth = 0
    private var oldHeight = 0

    override fun getWidth(): Int {
        super.getWidth().takeIf { it > 0 }?.let {
            oldWidth = it
        }
        return oldWidth
    }

    override fun getHeight(): Int {
        super.getHeight().takeIf { it > 0 }?.let {
            oldHeight = it
        }
        return oldHeight
    }

    private val boundsLeft
        get() = paddingStart
    private val realBoundsTop
        get() = paddingTop
    private val boundsTop
        get() = paddingTop + (layoutInfo.noScrollList.lastOrNull()?.bottom ?: 0)
    private val boundsRight
        get() = width - paddingEnd
    private val boundsBottom
        get() = height - paddingBottom

    fun Int.nextPosition(): Int = (this + 1)
    fun Int.lastPosition(): Int = (this - 1)

    inner class ItemData(var view: View, var position: Int, initOffset: Int = 0) {
        var offset: Int = initOffset
            set(value) {
                pendingOffset += value - field
                field = value
            }

        private var pendingOffset = 0

        val isScrollable: Boolean
            get() = adapter.getScrollMode(position)

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

//        fun isVisible(): Boolean {
//            return !shouldMoveOut()
//        }

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
            val nextIndex = this.position.lastPosition().takeIf { it.isValidPosition() } ?: return null
            return recycler.unsafeGetMeasuredItemData(nextIndex).above(this)
        }

        fun fetchDown(recycler: Recycler): ItemData? {
            val nextIndex = this.position.nextPosition().takeIf { it.isValidPosition() } ?: return null
            return recycler.unsafeGetMeasuredItemData(nextIndex).below(this)
        }

        val bottom: Int
            get() {
                return offset + measuredHeight
            }

        fun requestLayout(delta: Int = 0): ItemData {
            layoutDecoratedWithMargins(view,
                0,
                offset + delta,
                measuredWidth,
                offset + delta + measuredHeight
            )
            pendingOffset = 0
            return this
        }

        fun applyOffset() {
            if (pendingOffset != 0) {
                view.offsetTopAndBottom(pendingOffset)
                pendingOffset = 0
            }
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
            return "${this.javaClass.simpleName}{position=${position}, offset=${offset}, height=${measuredHeight}, view=${view.hashCode()}, bottom=$bottom}"
        }

        fun reattach() {
            removeView(view)
            addView()
            requestLayout()
        }

        fun measure(): ItemData {
            measureChildWithMargins(view, 0, 0)
            return this
        }

        fun addView(): ItemData {
            addView(view)
//            logD { "addView $this, ${Exception().stackTraceToString()}" }
            return this
        }

        fun updateInfo(recycler: Recycler): Boolean {
            if (position.isValidPosition()) {
                position = getPosition(view)
                if (position.isValidPosition()) {
//                    view = recycler.getViewForPosition(position)
                    measure()
                    return true
                }
            }
            return false
        }
    }

    private fun Int.isValidPosition() : Boolean {
        return this in 0 until itemCount
    }

    private fun <E> MutableCollection<E>.deepToString(sep: String = ",\n\t"): String {
        return this.joinToString(
            separator = sep,
            prefix = "[\n\t",
            postfix = "\n]",
        ) { it.toString() }
    }

    override fun toString(): String {
        return "noScrollList=${layoutInfo.noScrollList.deepToString()}\n" +
                "viewList=${layoutInfo.viewList.deepToString()}"
    }
    
    private fun logD(msg: ()->String) {
        if (DEBUG) {
            Log.d(TAG, msg.invoke())
        }
    }
}