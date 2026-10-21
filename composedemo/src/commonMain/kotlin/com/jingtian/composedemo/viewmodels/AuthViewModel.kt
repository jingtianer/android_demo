package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel

expect class AuthParams
open class AuthResult

expect class AuthSuccess: AuthResult
expect class AuthFailed: AuthResult
expect class AuthError: AuthResult

@Composable
expect fun authViewModel(): AuthViewModel

abstract class AuthViewModel : ViewModel() {
    abstract fun doAuth(authParams: AuthParams, onAuthResult: (AuthResult)->Unit)
    abstract fun doAuth(onAuthResult: (AuthResult)->Unit)
}