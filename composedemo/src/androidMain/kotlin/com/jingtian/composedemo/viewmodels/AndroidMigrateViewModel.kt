package com.jingtian.composedemo.viewmodels

import android.os.SystemClock
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import javax.crypto.Cipher

actual class AuthParams(val cipher: Cipher? = null)

actual class AuthSuccess(val result: BiometricPrompt.AuthenticationResult): AuthResult()
actual class AuthFailed: AuthResult()
actual class AuthError(val errorCode: Int, val errString: CharSequence): AuthResult()

@Composable
actual fun authViewModel(): AuthViewModel {
    return viewModel(factory = AndroidMigrateViewModel.factory)
}

class AndroidMigrateViewModel : AuthViewModel() {
    companion object {
        val factory = viewModelFactory { initializer<AuthViewModel> { AndroidMigrateViewModel() } }
        private val sIsPasswordChecked = mutableStateOf(false)
    }
    val isPasswordChecked: MutableState<Boolean> get() = sIsPasswordChecked
    private val currentBiometricPrompt: MutableList<BiometricPrompt> = mutableListOf()

    class AuthenticationCallbackWrapper(val callback: (AuthResult)->Unit, var onAuthFinished: ()->Unit = {}) : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            callback(AuthFailed())
            onAuthFinished()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            callback(AuthSuccess(result))
            onAuthFinished()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            callback(AuthError(errorCode, errString))
            onAuthFinished()
        }
    }

    inner class Authenticator(
        val promptCreator: (BiometricPrompt.AuthenticationCallback)->BiometricPrompt,
        private val promptInfo: BiometricPrompt.PromptInfo
    ) {
        fun doAuth(cipher: Cipher?, callback : (AuthResult)->Unit) {
            val callbackWrapper = AuthenticationCallbackWrapper(callback)
            val prompt = promptCreator(callbackWrapper)
            callbackWrapper.onAuthFinished = {
                currentBiometricPrompt.remove(prompt)
            }
            currentBiometricPrompt.add(prompt)
            if (cipher != null) {
                prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } else {
                prompt.authenticate(promptInfo)
            }
        }

        fun doAuth(callback : (AuthResult)->Unit) {
            val callbackWrapper = AuthenticationCallbackWrapper(callback)
            val prompt = promptCreator(callbackWrapper)
            callbackWrapper.onAuthFinished = {
                currentBiometricPrompt.remove(prompt)
            }
            currentBiometricPrompt.add(prompt)
            prompt.authenticate(promptInfo)
        }
    }

    private fun cancelAllBioPrompt() {
        currentBiometricPrompt.forEach {
            it.cancelAuthentication()
        }
        currentBiometricPrompt.clear()
    }

    fun onPause() {
        cancelAllBioPrompt()
    }

    fun onStop() {
        cancelAllBioPrompt()
    }

    lateinit var bioticAuth: Authenticator
    override fun doAuth(authParams: AuthParams, onAuthResult: (AuthResult)->Unit) {
        val cipher = authParams.cipher
        if (cipher != null) {
            bioticAuth.doAuth(cipher, onAuthResult)
        } else {
            doAuth(onAuthResult)
        }
    }

    override fun doAuth(onAuthResult: (AuthResult) -> Unit) {
        bioticAuth.doAuth(onAuthResult)
    }
}