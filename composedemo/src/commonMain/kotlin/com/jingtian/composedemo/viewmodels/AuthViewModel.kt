package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

expect class AuthParams
open class AuthResult

expect class AuthSuccess: AuthResult
expect class AuthFailed: AuthResult
expect class AuthError: AuthResult

@Composable
expect fun authViewModel(): AuthViewModel

private object AuthGlobalState {
    val sIsAppLocked = mutableStateOf(false)
}
abstract class AuthViewModel : ViewModel() {
    val isLocked get() = AuthGlobalState.sIsAppLocked

    abstract fun doAuth(authParams: AuthParams, onAuthResult: (AuthResult)->Unit)
    abstract fun doAuth(onAuthResult: (AuthResult)->Unit)
}