package com.jingtian.composedemo.viewmodels

import android.os.SystemClock
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.ViewModel
import javax.crypto.Cipher

class AndroidMigrateViewModel : ViewModel() {
    val isMigrationFinished = mutableIntStateOf(0)

    class Authenticator(
        val promptCreator: (BiometricPrompt.AuthenticationCallback)->BiometricPrompt,
        private val promptInfo: BiometricPrompt.PromptInfo
    ) {
        fun doAuth(cipher: Cipher?, callback : BiometricPrompt.AuthenticationCallback) {
            val prompt = promptCreator(callback)
            if (cipher != null) {
                prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } else {
                prompt.authenticate(promptInfo)
            }
        }
        fun doAuth(callback : BiometricPrompt.AuthenticationCallback) {
            val prompt = promptCreator(callback)
            prompt.authenticate(promptInfo)
        }
    }

    lateinit var bioticAuth: Authenticator
}