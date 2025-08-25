package com.jingtian.demoapp.main.rank

import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jingtian.demoapp.main.rank.model.ModelRank
import com.jingtian.demoapp.main.rank.model.ModelRankItem
import kotlin.math.abs
import kotlin.math.sign

object Utils {
    object DataHolder {
        val fakeData = listOf<ModelRank>(
            ModelRank(
                "吃货榜",
                listOf(
                    ModelRankItem(
                        "111",
                        4.2f,
                        "aaa",
                        listOf(),
                    ),
                    ModelRankItem(
                        "222",
                        1.2f,
                        "aaa",
                        listOf()
                    ),
                    ModelRankItem(
                        "333",
                        5f,
                        "aaa",
                        listOf()
                    )
                )
            ), ModelRank(
                "哈哈榜",
                listOf(
                    ModelRankItem(
                        "111-1",
                        4f,
                        "aaa",
                        listOf()
                    ),
                    ModelRankItem(
                        "222-1",
                        2f,
                        "aaa",
                        listOf()
                    ),
                    ModelRankItem(
                        "333-1",
                        3f,
                        "aaa",
                        listOf()
                    )
                )
            )
        )
    }

    object RecyclerViewUtils {
        class FixNestedScroll(
            private val recyclerView: RecyclerView,
            private val orientation: Int
        ) : View.OnTouchListener {
            private var initX = 0f
            private var initY = 0f
            private var scrolled = false
            private var touchSlop = ViewConfiguration.get(recyclerView.context).scaledTouchSlop

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
                            if (orientation == RecyclerView.HORIZONTAL && absDx > absDy && recyclerView.canScrollHorizontally(
                                    dx.intSign
                                )
                            ) {
                                scrolled = true
                            } else if (orientation == RecyclerView.VERTICAL && absDy > absDx && recyclerView.canScrollVertically(
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
}