package com.jingtian.composedemo.viewmodels

import android.os.SystemClock
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import javax.crypto.Cipher

class AndroidMigrateViewModel : ViewModel() {
    val isMigrationFinished = mutableIntStateOf(0)
    var isPasswordChecked = mutableStateOf(false)
    private val currentBiometricPrompt: MutableList<BiometricPrompt> = mutableListOf()

    class AuthenticationCallbackWrapper(val callback: AuthenticationCallback, var onAuthFinished: ()->Unit = {}) : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            callback.onAuthenticationFailed()
            onAuthFinished()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            callback.onAuthenticationSucceeded(result)
            onAuthFinished()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            callback.onAuthenticationError(errorCode, errString)
            onAuthFinished()
        }
    }

    inner class Authenticator(
        val promptCreator: (BiometricPrompt.AuthenticationCallback)->BiometricPrompt,
        private val promptInfo: BiometricPrompt.PromptInfo
    ) {
        fun doAuth(cipher: Cipher?, callback : BiometricPrompt.AuthenticationCallback) {
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

        fun doAuth(callback : BiometricPrompt.AuthenticationCallback) {
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
}