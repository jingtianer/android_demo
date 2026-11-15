@file:OptIn(ExperimentalComposeUiApi::class)

package com.jingtian.composedemo.main.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
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
    abstract var shouldDisableBackEvents: Boolean
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
    override var shouldDisableBackEvents: Boolean
        get() = impl.shouldDisableBackEvents
        set(value) {
            impl.shouldDisableBackEvents = value
        }
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

        // 返回键
        override var shouldDisableBackEvents by mutableStateOf(false)
        override val willHandleBackEvents: Boolean get() = backStack.size > 1
        // 动画
        val transitionState = SeekableTransitionState(currentBackStackEntry)
        var finalEnterTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> EnterTransition>({ EnterTransition.None })
        var finalExitTransition by mutableStateOf<AnimatedContentTransitionScope<NavRouter?>.() -> ExitTransition>({ ExitTransition.None })

        fun updateTransition(isPop: Boolean, isPredictive: Boolean, swipeEdge: Int) {
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
            updateTransition(isPop = isPop, isPredictive = false, swipeEdge = swipeEdge)
            withFrameMillis {  }
            transitionState.animateTo(initialStackTopEntry)
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

    val scope = rememberCoroutineScope()
    PredictiveBackHandler(enabled = willHandleBackEvents && !shouldDisableBackEvents) { backFlow ->
        try {
            backFlow.collect { event ->
                swipeEdge = event.swipeEdge
                updateTransition(isPop = true, isPredictive = true, swipeEdge = event.swipeEdge)
                if (event.swipeEdge != swipeEdge) {
                    withFrameMillis {  }
                }
                backProgress = event.progress
                val backProgressSnapshot = backProgress
                val secondLastBackStackEntrySnapshot = secondLastBackStackEntry
                scope.launch {
                    transitionState.seekTo(backProgressSnapshot, secondLastBackStackEntrySnapshot)
                }
            }
            backProgress = 1f
            val secondLastBackStackEntrySnapShot = secondLastBackStackEntry
            scope.launch {
                transitionState.animateTo(secondLastBackStackEntrySnapShot)
            }
            backStack.removeLastOrNull()
        } catch (e: CancellationException) {
            backProgress = 1f
            val currentBackStackEntrySnapshot = currentBackStackEntry
            val swipeEdgeSnapshot = swipeEdge
            scope.launch {
                updateTransition(isPop = false, isPredictive = true, swipeEdge = swipeEdgeSnapshot)
                withFrameMillis {  }
                transitionState.animateTo(currentBackStackEntrySnapshot)
            }
            throw e
        } finally {

        }
    }

//    BackPressHandler(isEnable = willHandleBackEvents && !shouldDisableBackEvents) {
//        pop()
//        willHandleBackEvents && !shouldDisableBackEvents
//    }

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