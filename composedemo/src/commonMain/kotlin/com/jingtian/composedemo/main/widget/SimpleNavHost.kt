@file:OptIn(ExperimentalComposeUiApi::class)

package com.jingtian.composedemo.main.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.graphics.Color
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

class NavMapBuilder internal constructor(private val navMap: MutableMap<NavRouter, NavEntry>) {
    fun composable(navRouter: NavRouter, content: @Composable () -> Unit) {
        navMap[navRouter] = NavEntry(content)
    }
}

typealias EnterTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> EnterTransition
typealias ExitTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> ExitTransition
typealias SizeTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> SizeTransform?

interface AbstractSimpleNavHostController {
    val currentBackStack: List<NavRouter>
    val currentBackStackEntry: NavRouter?
    val secondLastBackStackEntry: NavRouter?
    val willHandleBackEvents: Boolean
    var extraRenderEntry: NavRouter?
    var shouldHandleBack: Boolean
    suspend fun navigateTo(router: NavRouter)
    suspend fun pop()
}

class SimpleNavHostController(private val impl: SimpleNavHostControllerImpl = SimpleNavHostControllerImpl()) : AbstractSimpleNavHostController by impl {
    internal fun impl(
        initialDestination: NavRouter?,
        enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
        exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
        sizeTransition: (@JvmSuppressWildcards SizeTransitionCreator)? = null,
        mapBuilder: NavMapBuilder.() -> Unit
    ) : SimpleNavHostControllerImpl {
        impl.update(initialDestination, enterTransition, exitTransition, sizeTransition, mapBuilder)
        return impl
    }

    class SimpleNavHostControllerImpl(
        initialDestination: NavRouter? = null,
        private var enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
        private var exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
        private var sizeTransition: (@JvmSuppressWildcards SizeTransitionCreator)? = null,
        mapBuilder: NavMapBuilder.() -> Unit = {}
    ) : AbstractSimpleNavHostController {
        // 返回栈
        val backStack = mutableStateListOf<NavRouter>()
        override val currentBackStack: List<NavRouter> get() = this.backStack
        val navMap = mutableStateMapOf<NavRouter, NavEntry>()
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
        var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_RIGHT)
        var inPredictiveBack by mutableStateOf(false)
        var pendingTargetRouter by mutableStateOf<NavRouter?>(null)
        override var shouldHandleBack: Boolean by mutableStateOf(true)

        // 返回键
        override val willHandleBackEvents: Boolean get() = backStack.size > 1 && shouldHandleBack
        // 动画
        val transitionState = SeekableTransitionState(currentBackStackEntry)
        var finalEnterTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> EnterTransition>({ EnterTransition.None })
        var finalExitTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> ExitTransition>({ ExitTransition.None })
        var finalSizeTransition by mutableStateOf<(AnimatedContentTransitionScope<NavRouter?>.() -> SizeTransform?)?>(null)

        override var extraRenderEntry: NavRouter? by mutableStateOf(null)

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
            finalSizeTransition = sizeTransition?.let { sizeTransition->
                { sizeTransition(this, swipeEdge) }
            }
        }

        override suspend fun navigateTo(router: NavRouter) {
            navigateTo(router, withAnim = true)
        }

        suspend fun navigateTo(router: NavRouter, withAnim: Boolean) {
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
            if (withAnim) {
                val finalStackTopEntry = currentBackStackEntry
                updateTransition(isPop = isPop, swipeEdge = swipeEdge)
                withFrameMillis {  }
                transitionState.snapTo(initialStackTopEntry)
                transitionState.animateTo(finalStackTopEntry)
            }
        }

        override suspend fun pop() {
            navigateTo(secondLastBackStackEntry ?: return)
        }

        fun update(
            initialDestination: NavRouter?,
            enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
            exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
            sizeTransition: (@JvmSuppressWildcards SizeTransitionCreator)? = null,
            mapBuilder: NavMapBuilder.() -> Unit
        ) {
            this.exitTransition = exitTransition
            this.enterTransition = enterTransition
            this.sizeTransition = sizeTransition
            navMap.clear()
            navMap.also {
                mapBuilder.invoke(NavMapBuilder(it))
            }

            backStack.removeAll { !navMap.contains(it) }

            if (initialDestination != null && backStack.firstOrNull() != initialDestination) {
                if (backStack.contains(initialDestination)) {
                    backStack.remove(initialDestination)
                }
                backStack.add(0, initialDestination)
            }
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
    scrimColor: Color = DrawerDefaults.scrimColor,
    enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
    exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
    sizeTransition: (@JvmSuppressWildcards SizeTransitionCreator)? = null,
    mapBuilder: NavMapBuilder.() -> Unit = {},
) {
    val controller = remember(initialDestination, enterTransition, exitTransition, sizeTransition, mapBuilder) {
        simpleNavHostController.impl(
            initialDestination, enterTransition, exitTransition, sizeTransition, mapBuilder
        )
    }
    controller.SimpleNavHost(scrimColor = scrimColor)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SimpleNavHostController.SimpleNavHostControllerImpl.SimpleNavHost(
    scrimColor: Color,
) {
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
                pendingTargetRouter = secondLastBackStackEntry
                inPredictiveBack = false
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
        LaunchedEffect(currentBackStackEntry, pendingTargetRouter) {
            val pendingTargetRouterSnapshot = pendingTargetRouter
            if (transitionState.currentState != currentBackStackEntry) {
                transitionState.animateTo(currentBackStackEntry)
            } else if (transitionState.currentState != pendingTargetRouterSnapshot && pendingTargetRouterSnapshot != null) {
                transitionState.animateTo(pendingTargetRouter)
                navigateTo(pendingTargetRouterSnapshot, withAnim = false)
                pendingTargetRouter = null
            } else {
                if (pendingTargetRouterSnapshot != null) {
                    transitionState.animateTo(pendingTargetRouterSnapshot)
                    navigateTo(pendingTargetRouterSnapshot, withAnim = false)
                    pendingTargetRouter = null
                    return@LaunchedEffect
                }
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
            sizeTransform = finalSizeTransition?.invoke(this),
            targetContentZIndex = backStack.indexOf(targetState) / backStack.size.toFloat()
        )
    }) { state ->
        if (extraRenderEntry != null) {
            navMap[extraRenderEntry]?.content?.invoke()
        }
        navMap[state]?.content?.invoke() ?: return@AnimatedContent
        if (state != null && state == secondLastBackStackEntry && transitionState.targetState != transitionState.currentState) {
            val targetIsSelf = transitionState.targetState == state
            Canvas(Modifier.fillMaxSize()) { drawRect(scrimColor, alpha = if (targetIsSelf) 1 - transitionState.fraction else transitionState.fraction) }
        }
    }
}