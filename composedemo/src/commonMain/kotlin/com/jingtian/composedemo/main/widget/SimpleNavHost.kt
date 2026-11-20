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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.PredictiveBackHandler
import androidx.compose.ui.graphics.Color
import com.jingtian.composedemo.multiplatform.logD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    val content: @Composable (NavEntry) -> Unit
) {
    var callback: (suspend () -> Unit)? by mutableStateOf(null)
}

class NavMapBuilder internal constructor(private val navMap: MutableMap<NavRouter, NavEntry>) {
    fun composable(navRouter: NavRouter, content: @Composable (NavEntry) -> Unit) {
        navMap[navRouter] = NavEntry(content)
    }
}

typealias EnterTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> EnterTransition
typealias ExitTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> ExitTransition
typealias SizeTransitionCreator = AnimatedContentTransitionScope<NavRouter?>.(swipeEdge: Int) -> SizeTransform?

interface ISimpleNavSeekHandler {
    val target: NavRouter
    fun init()
    fun seekTo(progress: Float, swipeEdge: Int)
    fun finish()
    fun revoke()
}

interface AbstractSimpleNavHostController {
    val currentBackStack: List<NavRouter>
    val currentBackStackEntry: NavRouter?
    val secondLastBackStackEntry: NavRouter?
    val willHandleBackEvents: Boolean
    var extraRenderEntry: NavRouter?
    var shouldHandleBack: Boolean
    suspend fun navigateTo(router: NavRouter)
    suspend fun pop()
    fun seekHandler(router: NavRouter): ISimpleNavSeekHandler
}

class SimpleNavHostController(private val impl: SimpleNavHostControllerImpl = SimpleNavHostControllerImpl()) :
    AbstractSimpleNavHostController by impl {
    internal fun impl(
        initialDestination: NavRouter?,
        enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
        exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
        sizeTransition: (@JvmSuppressWildcards SizeTransitionCreator)? = null,
        mapBuilder: NavMapBuilder.() -> Unit
    ): SimpleNavHostControllerImpl {
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
        var backProgress by mutableFloatStateOf(0f)
        var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_RIGHT)
        var inPredictiveBack by mutableStateOf(false)
        var pendingTargetRouter by mutableStateOf<NavRouter?>(null)
        override var shouldHandleBack: Boolean by mutableStateOf(true)
        var isPop by mutableStateOf(false)

        // 返回键
        override val willHandleBackEvents: Boolean get() = backStack.size > 1 && shouldHandleBack

        // 动画
        val transitionState = SeekableTransitionState(currentBackStackEntry)
        var finalEnterTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> EnterTransition>(
            { EnterTransition.None })
        var finalExitTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> ExitTransition>(
            { ExitTransition.None })
        var finalSizeTransition by mutableStateOf<(AnimatedContentTransitionScope<NavRouter?>.() -> SizeTransform?)?>(
            null
        )

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
            finalSizeTransition = sizeTransition?.let { sizeTransition ->
                { sizeTransition(this, swipeEdge) }
            }
        }

        override suspend fun navigateTo(router: NavRouter) {
            navigateTo(router, withAnim = true)
        }

        private fun isPop(router: NavRouter, modifyStack: Boolean = true): Boolean {
            return if (backStack.contains(router)) {
                if (modifyStack) {
                    if (backStack.isNotEmpty() && backStack.last() != router) {
                        backStack.removeLast()
                    }
                }
                true
            } else {
                if (modifyStack) {
                    backStack.add(router)
                }
                false
            }
        }

        private var onBackJob: Job? by mutableStateOf(null)

        fun handleBackJob(scope: CoroutineScope) {
            val callback = navMap[currentBackStackEntry]?.callback
            if (callback != null && onBackJob == null) {
                onBackJob = scope.launch {
                    callback.invoke()
                }
                navMap[currentBackStackEntry]?.callback = null
            }
        }

        suspend fun joinBackJob() {
            onBackJob?.let {
                it.join()
                onBackJob = null
            }
        }

        suspend fun navigateTo(router: NavRouter, withAnim: Boolean) {
            if (withAnim) {
                withFrameMillis { }
                seekHandler(router).apply {
                    init()
                    finish()
                }
            } else {
                isPop(router, modifyStack = true)
            }
        }

        override suspend fun pop() {
            navigateTo(secondLastBackStackEntry ?: return)
        }

        var currentSeekHandler: ISimpleNavSeekHandler? = null

        override fun seekHandler(router: NavRouter): ISimpleNavSeekHandler {
            currentSeekHandler?.let {
                return it.takeIf { it.target == router } ?: EmptySimpleNavSeekHandler(router)
            }
            if (currentBackStack == router) {
                return EmptySimpleNavSeekHandler(router)
            }
            val isPop = if (isPop(router, modifyStack = false)) {
                true
            } else {
                isPop(router, modifyStack = true)
                false
            }
            val handler = SimpleNavSeekHandler(isPop, router).also {
                currentSeekHandler = it
            }
            return handler
        }

        private class EmptySimpleNavSeekHandler(override val target: NavRouter) :
            ISimpleNavSeekHandler {
            override fun init() = Unit
            override fun seekTo(progress: Float, swipeEdge: Int) = Unit
            override fun finish() = Unit
            override fun revoke() = Unit

            override fun toString(): String {
                return "EmptySimpleNavSeekHandler"
            }
        }

        private inner class SimpleNavSeekHandler(
            private val isPop: Boolean,
            override val target: NavRouter
        ) : ISimpleNavSeekHandler {
            override fun init() {
                if (currentSeekHandler !== this@SimpleNavSeekHandler) {
                    return
                }
                this@SimpleNavHostControllerImpl.isPop = isPop
                inPredictiveBack = true
                pendingTargetRouter = target
            }

            override fun seekTo(progress: Float, swipeEdge: Int) {
                if (currentSeekHandler !== this@SimpleNavSeekHandler) {
                    return
                }
                this@SimpleNavHostControllerImpl.backProgress = progress
                this@SimpleNavHostControllerImpl.swipeEdge = swipeEdge
            }

            override fun finish() {
                if (currentSeekHandler !== this@SimpleNavSeekHandler) {
                    return
                }
                inPredictiveBack = false
                pendingTargetRouter = target
//                currentSeekHandler = null
            }

            override fun revoke() {
                if (currentSeekHandler !== this@SimpleNavSeekHandler) {
                    return
                }
                if (!isPop) {
                    if (backStack.lastOrNull() == target) {
                        backStack.removeLast()
                    }
                }
                inPredictiveBack = false
                pendingTargetRouter = null
//                currentSeekHandler = null
            }

            override fun toString(): String {
                return "SimpleNavSeekHandler isPop=$isPop, target=$target, inPredictiveBack=$inPredictiveBack"
            }
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
    modifier: Modifier,
    simpleNavHostController: SimpleNavHostController = rememberSimpleNavHostController(),
    initialDestination: NavRouter? = null,
    scrimColor: Color = DrawerDefaults.scrimColor,
    enterTransition: (@JvmSuppressWildcards EnterTransitionCreator) = { EnterTransition.None },
    exitTransition: (@JvmSuppressWildcards ExitTransitionCreator) = { ExitTransition.None },
    sizeTransition: (@JvmSuppressWildcards SizeTransitionCreator)? = null,
    mapBuilder: NavMapBuilder.() -> Unit = {},
) {
    val controller =
        remember(initialDestination, enterTransition, exitTransition, sizeTransition, mapBuilder) {
            simpleNavHostController.impl(
                initialDestination, enterTransition, exitTransition, sizeTransition, mapBuilder
            )
        }
    controller.SimpleNavHost(modifier = modifier, scrimColor = scrimColor)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SimpleNavHostController.SimpleNavHostControllerImpl.SimpleNavHost(
    modifier: Modifier,
    scrimColor: Color,
) {
    val transition = rememberTransition(transitionState, label = "entry")
    val scope = rememberCoroutineScope()

    PredictiveBackHandler(enabled = willHandleBackEvents) { backFlow ->
        val secondLastBackStackEntry = secondLastBackStackEntry
        if (secondLastBackStackEntry != null) {
            try {
                backFlow.collect { event ->
                    val seekHandler = seekHandler(secondLastBackStackEntry)
                    if (!inPredictiveBack) {
                        seekHandler.init()
                        handleBackJob(scope)
                    }
                    seekHandler.seekTo(event.progress, event.swipeEdge)
                }
                seekHandler(secondLastBackStackEntry).finish()
            } catch (e: CancellationException) {
                seekHandler(secondLastBackStackEntry).revoke()
            } finally {

            }
        } else {
            backFlow.collect { event ->
            }
        }
    }

    LaunchedEffect(backProgress, pendingTargetRouter, inPredictiveBack, swipeEdge, isPop) {
        if (!inPredictiveBack) {
            return@LaunchedEffect
        }
        updateTransition(isPop = isPop, swipeEdge = swipeEdge)
        // Update transition progress safely (same guard against stale enabled state).
        transitionState.seekTo(backProgress, pendingTargetRouter)
    }
    LaunchedEffect(currentBackStackEntry, pendingTargetRouter, inPredictiveBack, swipeEdge, isPop) {
        if (inPredictiveBack) {
            return@LaunchedEffect
        }
        updateTransition(isPop, swipeEdge)
        val pendingTargetRouterSnapshot = pendingTargetRouter
        if (isPop) {
            handleBackJob(scope)
            joinBackJob()
        }
        if (transitionState.currentState != pendingTargetRouterSnapshot && pendingTargetRouterSnapshot != null) {
//            logD("seekHandler") {
//                "LaunchedEffect, !inPredictiveBack currentState!=pendingTargetRouterSnapshot backProgress=$backProgress, pendingTargetRouter=$pendingTargetRouter"
//            }
            transitionState.animateTo(pendingTargetRouterSnapshot)
            navigateTo(pendingTargetRouterSnapshot, withAnim = false)
            pendingTargetRouter = null
            currentSeekHandler = null
        } else {
            if (pendingTargetRouterSnapshot != null) {
//                logD("seekHandler") {
//                    "LaunchedEffect, !inPredictiveBack finish currentState==pendingTargetRouterSnapshot backProgress=$backProgress, pendingTargetRouter=$pendingTargetRouter"
//                }
                transitionState.animateTo(pendingTargetRouterSnapshot)
                navigateTo(pendingTargetRouterSnapshot, withAnim = false)
                pendingTargetRouter = null
                currentSeekHandler = null
                return@LaunchedEffect
            }
//            logD("seekHandler") {
//                "LaunchedEffect, !inPredictiveBack revoke currentState==pendingTargetRouterSnapshot backProgress=$backProgress, pendingTargetRouter=$pendingTargetRouter"
//            }
            transitionState.seekTo(
                backProgress,
                currentSeekHandler?.target ?: currentBackStackEntry
            )

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
//                        logD("seekHandler") {
//                            "LaunchedEffect, !inPredictiveBack revoke value=$value"
//                        }
                    }
                    if (value == 0f) {
                        // Once we animate to the start, we need to snap to the right state.
                        transitionState.seekTo(0f)
                        transitionState.snapTo(currentBackStackEntry)
//                        logD("seekHandler") {
//                            "LaunchedEffect, !inPredictiveBack revoke value=$value, snapTo"
//                        }
                    }
                }
            }
            currentSeekHandler = null
        }
    }
    transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            ContentTransform(
                initialContentExit = finalExitTransition.invoke(this),
                targetContentEnter = finalEnterTransition.invoke(this),
                sizeTransform = finalSizeTransition?.invoke(this),
                targetContentZIndex = backStack.indexOf(targetState) / backStack.size.toFloat()
            )
        }) { state ->
        if (extraRenderEntry != null) {
            navMap[extraRenderEntry]?.let {
                it.content.invoke(it)
            }
        }
        navMap[state]?.let {
            it.content.invoke(it)
        } ?: return@AnimatedContent
        if (state != null && state == secondLastBackStackEntry && transitionState.targetState != transitionState.currentState) {
            val targetIsSelf = transitionState.targetState == state
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    scrimColor,
                    alpha = if (targetIsSelf) 1 - transitionState.fraction else transitionState.fraction
                )
            }
        }
    }
}