package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.jingtian.composedemo.base.JumpOnPurposeMarkHolder
import com.jingtian.composedemo.multiplatform.logD
import com.jingtian.composedemo.viewmodels.AuthGlobalState.MIN_ON_STOP_DO_AUTH_TIME_GAP
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

expect class AuthParams
open class AuthResult
object AuthRejected: AuthResult()
object AuthSkip: AuthResult()
expect class AuthSuccess: AuthResult
expect class AuthFailed: AuthResult
expect class AuthError: AuthResult

expect fun authViewModel(): AuthViewModel

private object AuthGlobalState {
    const val MIN_ON_STOP_DO_AUTH_TIME_GAP = 30 * 1000L

}
abstract class AuthViewModel : SingletonViewModel(), DefaultLifecycleObserver {
    private val lockedState = mutableStateOf(true)
    val isLocked: State<Boolean> = lockedState
    val isVisible = mutableStateOf(false)
    val isAuthed = mutableStateOf(false)
    private val lastOnStopAuthTimeStamp = mutableLongStateOf(-1)
    private var isAppEnterBackground = false
    private var isDoingAuth = false
    fun lock() {
        if (isAppEnterBackground) {
            return
        }
        lockedState.value = true
    }
    override fun onPause(owner: LifecycleOwner) {
        isAppEnterBackground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppEnterBackground = true
    }

    override fun onResume(owner: LifecycleOwner) {
        isAppEnterBackground = false
    }
    override fun onStart(owner: LifecycleOwner) {
        isAppEnterBackground = false
    }

    private fun checkState(checkLock: Boolean = false): Boolean {
        if (isAppEnterBackground) {
            return false
        }
        if (isDoingAuth) {
            return false
        }
        if (checkLock && isLocked.value) {
            return false
        }
        return true
    }

    fun doAuth(authParams: AuthParams, onAuthResult: (AuthResult)->Unit) {
        if (!checkState(checkLock = true)) {
            onAuthResult(AuthRejected)
            return
        }
        doAuthImplWrapper(doAuthImpl = { doAuthImpl(authParams, it) }, onAuthResult)
    }

    @OptIn(ExperimentalTime::class)
    fun doAuth(onAuthResult: (AuthResult)->Unit) {
        if (!checkState(checkLock = true)) {
            onAuthResult(AuthRejected)
            return
        }
        val timeCost = !(lastOnStopAuthTimeStamp.value != -1L && Clock.System.now().toEpochMilliseconds() - lastOnStopAuthTimeStamp.value <= MIN_ON_STOP_DO_AUTH_TIME_GAP)
        val shouldDoAuth = timeCost
        if (!shouldDoAuth) {
            onAuthResult(AuthSkip)
            return
        }
        doAuthImplWrapper(doAuthImpl = { doAuthImpl(it) }, onAuthResult)
    }

    @OptIn(ExperimentalTime::class)
    fun login(onAuthResult: (AuthResult)->Unit) {
        if (!checkState()) {
            onAuthResult(AuthRejected)
            return
        }
        val isJumpOnPurpose = JumpOnPurposeMarkHolder.isJumpOnPurpose.getAndSet(false)
        val isAppLockedSnapshot = isLocked.value
        val timeCost = !(lastOnStopAuthTimeStamp.value != -1L && Clock.System.now().toEpochMilliseconds() - lastOnStopAuthTimeStamp.value <= MIN_ON_STOP_DO_AUTH_TIME_GAP)
        val shouldDoAuth = isAppLockedSnapshot || (!isAuthed.value && !isJumpOnPurpose && timeCost)
        if (!shouldDoAuth) {
            isAuthed.value = true
            isVisible.value = true
            lockedState.value = false
            onAuthResult(AuthSkip)
            return
        }
        doAuthImplWrapper(doAuthImpl = { doAuthImpl(it) }, onAuthResult)
    }

    @OptIn(ExperimentalTime::class)
    private inline fun doAuthImplWrapper(doAuthImpl: ((AuthResult)->Unit) -> Unit, crossinline onAuthResult: (AuthResult)->Unit) {
        JumpOnPurposeMarkHolder.isJumpOnPurpose.value = true
        isDoingAuth = true
        doAuthImpl { authResult->
            isDoingAuth = false
            when(authResult) {
                is AuthSuccess -> {
                    lastOnStopAuthTimeStamp.value = Clock.System.now().toEpochMilliseconds()
                    isVisible.value = true
                    isAuthed.value = true
                    lockedState.value = false
                }
            }
            onAuthResult(authResult)
        }
    }

    protected abstract fun doAuthImpl(onAuthResult: (AuthResult)->Unit)
    protected abstract fun doAuthImpl(authParams: AuthParams, onAuthResult: (AuthResult)->Unit)
    abstract fun cancelAllBioPrompt()
}