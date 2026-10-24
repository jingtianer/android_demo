package com.jingtian.composedemo.viewmodels

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jingtian.composedemo.multiplatform.logD
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
import platform.posix.alloca

actual class AuthParams
actual class AuthSuccess: AuthResult()
actual class AuthFailed: AuthResult()
actual class AuthError(val error: NSError): AuthResult()

@Composable
actual fun authViewModel(): AuthViewModel = viewModel { AuthViewModelImpl() }

class AuthViewModelImpl : AuthViewModel() {
    override fun doAuth(authParams: AuthParams, onAuthResult: (AuthResult)->Unit) {
        doAuth(onAuthResult)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun doAuth(onAuthResult: (AuthResult)->Unit) {
        val context = LAContext()
        context.localizedCancelTitle = "取消"
        context.localizedFallbackTitle = "输入密码"
        val canAuth = memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val canAuth = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, errorPtr.ptr)
            logD("doAuth") {
                "canAuth=$canAuth, err=${errorPtr.value?.code()}"
            }
            canAuth && errorPtr.value == null
        }

        if (!canAuth) {
            onAuthResult(AuthSuccess())
        } else {
            logD("doAuth") {
                "startAuth"
            }
            context.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthentication,
                localizedReason = "识别是你本人后继续",
                reply = { success, err->
                    dispatch_async(dispatch_get_main_queue()) {
                        logD("doAuth") {
                            "authReply: success=$success, err=${err?.code()}"
                        }
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
}