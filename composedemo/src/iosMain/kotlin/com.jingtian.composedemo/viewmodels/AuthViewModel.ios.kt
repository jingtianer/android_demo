package com.jingtian.composedemo.viewmodels

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class AuthParams
actual class AuthSuccess: AuthResult()
actual class AuthFailed: AuthResult()
actual class AuthError(val error: NSError): AuthResult() {
    override fun toString(): String {
        return error.toString()
    }
}

actual fun authViewModel(): AuthViewModel = AuthViewModelImpl

object AuthViewModelImpl : AuthViewModel() {
    private val pendingAuthTask = mutableListOf<LAContext>()
    override fun doAuthImpl(authParams: AuthParams, onAuthResult: (AuthResult)->Unit) {
        doAuthImpl(onAuthResult)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun doAuthImpl(onAuthResult: (AuthResult)->Unit) {
        val context = LAContext()
        context.localizedCancelTitle = "取消"
        context.localizedFallbackTitle = "输入密码"
        val canAuth = memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val canAuth = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, errorPtr.ptr)
//            logD("doAuth") {
//                "canAuth=$canAuth, err=${errorPtr.value?.code()}"
//            }
            canAuth && errorPtr.value == null
        }

        if (!canAuth) {
            onAuthResult(AuthSuccess())
        } else {
//            logD("doAuth") {
//                "startAuth"
//            }
            pendingAuthTask.add(context)
            context.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthentication,
                localizedReason = "识别是你本人后继续",
                reply = { success, err->
                    dispatch_async(dispatch_get_main_queue()) {
                        pendingAuthTask.remove(context)
//                        logD("doAuth") {
//                            "authReply: success=$success, err=${err?.code()}"
//                        }
                        if (success || err == null) {
                            onAuthResult(AuthSuccess())
                        } else {
                            onAuthResult(AuthError(err))
                        }
                    }
                }
            )
        }
    }

    override fun cancelAllBioPrompt() {
        pendingAuthTask.forEach { it.invalidate() }
        pendingAuthTask.clear()
    }
}