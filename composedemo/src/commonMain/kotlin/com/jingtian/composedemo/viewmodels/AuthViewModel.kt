package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.atomicfu.atomic

expect class AuthParams
open class AuthResult

expect class AuthSuccess: AuthResult
expect class AuthFailed: AuthResult
expect class AuthError: AuthResult

@Composable
expect fun authViewModel(): AuthViewModel

abstract class AuthViewModel : ViewModel() {
    companion object {
        private val sIsAppLocked = atomic(false)
    }
    val isAppLocked = sIsAppLocked
    abstract fun doAuth(authParams: AuthParams, onAuthResult: (AuthResult)->Unit)
    abstract fun doAuth(onAuthResult: (AuthResult)->Unit)
}