package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

actual class AuthParams
actual class AuthSuccess: AuthResult()
actual class AuthFailed: AuthResult()
actual class AuthError: AuthResult()

@Composable
actual fun authViewModel(): AuthViewModel = viewModel { AuthViewModelImpl() }

class AuthViewModelImpl : AuthViewModel() {
    override fun doAuth(authParams: AuthParams, onAuthResult: (AuthResult)->Unit) {
        onAuthResult(AuthSuccess())
    }

    override fun doAuth(onAuthResult: (AuthResult)->Unit) {
        onAuthResult(AuthSuccess())
    }
}