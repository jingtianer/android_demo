@file:OptIn(ExperimentalComposeUiApi::class)

package com.jingtian.composedemo.main.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jingtian.composedemo.base.BackPressHandler
import com.jingtian.composedemo.multiplatform.logD
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmSuppressWildcards

object BackEventCompat {
    const val EDGE_LEFT = 0
    const val EDGE_RIGHT = 1
}

open class NavRouter {
    override fun toString(): String {
        return this::class.simpleName.toString()
    }
}

class NavEntry internal constructor(
    val content: @Composable () -> Unit
)

class NavMapBuilder internal constructor(private val navMap: HashMap<NavRouter, NavEntry>) {
    fun composable(navRouter: NavRouter, content: @Composable () -> Unit) {
        navMap[navRouter] = NavEntry(content)
    }
}

typealias EnterTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> EnterTransition
typealias ExitTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> ExitTransition

abstract class AbstractSimpleNavHostController {
    abstract val currentBackStack: List<NavRouter>
    abstract val currentBackStackEntry: NavRouter?
    abstract val secondLastBackStackEntry: NavRouter?
    abstract val willHandleBackEvents: Boolean
    abstract suspend fun navigateTo(router: NavRouter)
    abstract suspend fun pop()
}

class SimpleNavHostController : AbstractSimpleNavHostController() {
    private var impl by mutableStateOf<AbstractSimpleNavHostController>(SimpleNavHostControllerImpl())
    override val currentBackStack: List<NavRouter>
        get() = impl.currentBackStack
    override val currentBackStackEntry: NavRouter?
        get() = impl.currentBackStackEntry
    override val secondLastBackStackEntry: NavRouter?
        get() = impl.secondLastBackStackEntry
    override val willHandleBackEvents: Boolean
        get() = impl.willHandleBackEvents

    override suspend fun navigateTo(router: NavRouter) {
        impl.navigateTo(router)
    }

    override suspend fun pop() {
        impl.pop()
    }

    internal fun impl(
        initialDestination: NavRouter?,
        enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
        exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
        mapBuilder: NavMapBuilder.() -> Unit
    ) : SimpleNavHostControllerImpl {
        val impl = SimpleNavHostControllerImpl(initialDestination, enterTransition, exitTransition, mapBuilder)
        this.impl = impl
        return impl
    }

    class SimpleNavHostControllerImpl(
        initialDestination: NavRouter? = null,
        private val enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
        private val exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
        mapBuilder: NavMapBuilder.() -> Unit = {}
    ) : AbstractSimpleNavHostController() {
        // 返回栈
        val backStack = mutableStateListOf<NavRouter>()
        override val currentBackStack: List<NavRouter> get() = this.backStack
        val navMap = HashMap<NavRouter, NavEntry>()
        init {
            if (initialDestination != null) {
                backStack.add(initialDestination)
            }
            navMap.also {
                mapBuilder.invoke(NavMapBuilder(it))
            }
        }
        override val currentBackStackEntry get() = backStack.lastOrNull()
        override val secondLastBackStackEntry get() = backStack.getOrNull(backStack.size - 2)

        // 可预测返回
        var backProgress by mutableFloatStateOf(-1f)
        var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
        var inPredictiveBack by mutableStateOf(false)

        // 返回键
        override val willHandleBackEvents: Boolean get() = backStack.size > 1
        // 动画
        val transitionState = SeekableTransitionState(currentBackStackEntry)
        var finalEnterTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> EnterTransition>({ EnterTransition.None })
        var finalExitTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> ExitTransition>({ ExitTransition.None })

        fun updateTransition(isPop: Boolean, swipeEdge: Int) {
            finalEnterTransition = if (isPop) {
                { EnterTransition.None }
            } else {
                { enterTransition(this, swipeEdge) }
            }
            finalExitTransition = if (isPop) {
                { exitTransition(this, swipeEdge) }
            } else {
                { ExitTransition.None }
            }
        }

        override suspend fun navigateTo(router: NavRouter) {
            val initialStackTopEntry = backStack.lastOrNull()
            val isPop = if (backStack.contains(router)) {
                if (backStack.isNotEmpty() && backStack.last() != router) {
                    backStack.removeLast()
                }
                true
            } else {
                backStack.add(router)
                false
            }
            val finalStackTopEntry = backStack.lastOrNull()
            updateTransition(isPop = isPop, swipeEdge = swipeEdge)
            withFrameMillis {  }
            transitionState.snapTo(initialStackTopEntry)
            transitionState.animateTo(finalStackTopEntry)
        }

        override suspend fun pop() {
            navigateTo(secondLastBackStackEntry ?: return)
        }
    }
}

@Composable
fun rememberSimpleNavHostController(): SimpleNavHostController {
    return remember { SimpleNavHostController() }
}

@Composable
fun SimpleNavHost(
    simpleNavHostController: SimpleNavHostController = rememberSimpleNavHostController(),
    initialDestination: NavRouter? = null,
    enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
    exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
    mapBuilder: NavMapBuilder.() -> Unit = {},
) {
    val controller = remember {
        simpleNavHostController.impl(
            initialDestination, enterTransition, exitTransition, mapBuilder
        )
    }
    controller.SimpleNavHost()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SimpleNavHostController.SimpleNavHostControllerImpl.SimpleNavHost() {
    val transition = rememberTransition(transitionState, label = "entry")

    PredictiveBackHandler(enabled = willHandleBackEvents) { backFlow ->
        try {
            backFlow.collect { event ->
                if (willHandleBackEvents) {
                    inPredictiveBack = true
                }
                backProgress = event.progress
                swipeEdge = event.swipeEdge
            }
            if (willHandleBackEvents) {
                inPredictiveBack = false
                backStack.removeLastOrNull()
            }
        } catch (e: CancellationException) {
            if (willHandleBackEvents) {
                inPredictiveBack = false
            }
        } finally {

        }
    }

    if (inPredictiveBack) {
        LaunchedEffect(backProgress) {
            updateTransition(isPop = true, swipeEdge = swipeEdge)
            // Update transition progress safely (same guard against stale enabled state).
            if (currentBackStack.size > 1) {
                val previousEntry = secondLastBackStackEntry
                transitionState.seekTo(backProgress, previousEntry)
            }
        }
    } else {
        LaunchedEffect(currentBackStackEntry) {
            if (transitionState.currentState != currentBackStackEntry) {
                transitionState.animateTo(currentBackStackEntry)
            } else {
                // convert from nanoseconds to milliseconds
                val totalDuration = transition.totalDurationNanos / 1000000
                // When the predictive back gesture is cancel, we need to manually animate
                // the SeekableTransitionState from where it left off, to zero and then
                // snapTo the final position.
                animate(
                    transitionState.fraction,
                    0f,
                    animationSpec = tween((transitionState.fraction * totalDuration).toInt())
                ) { value, _ ->
                    this@LaunchedEffect.launch {
                        if (value > 0) {
                            // Seek the original transition back to the currentState
                            transitionState.seekTo(value)
                        }
                        if (value == 0f) {
                            // Once we animate to the start, we need to snap to the right state.
                            transitionState.snapTo(currentBackStackEntry)
                        }
                    }
                }
            }
        }
    }

    transition.AnimatedContent(transitionSpec = {
        ContentTransform(
            initialContentExit = finalExitTransition.invoke(this),
            targetContentEnter = finalEnterTransition.invoke(this),
            targetContentZIndex = backStack.indexOf(targetState) / backStack.size.toFloat()
        )
    }) { state ->
        navMap[state]?.content?.invoke()
    }
}