package com.jingtian.composedemo.main.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.deleteRecursively
import com.jingtian.composedemo.utils.getDBStorageRootDir
import com.jingtian.composedemo.utils.getFileCacheStorageRootDir
import com.jingtian.composedemo.utils.getFileStorageRootDir
import com.jingtian.composedemo.viewmodels.SingletonViewModel
import com.jingtian.composedemo.viewmodels.singletonViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun settingsViewModel() = SettingsViewModel

object SettingsViewModel : SingletonViewModel() {

    private const val COUNTDOWN_COUNT = 3

    var snackBarMessage = mutableStateOf<String?>(null)

    private var currentMessageDelayJob: Job? = null

    fun sendMessage(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            currentMessageDelayJob?.cancel()
            currentMessageDelayJob = viewModelScope.launch(Dispatchers.Main) {
                snackBarMessage.value = message
                withContext(Dispatchers.IO) {
                    delay(2000)
                }
                snackBarMessage.value = null
                currentMessageDelayJob = null
            }
        }
    }

    private suspend fun kill() {
        for (countDown in (1 .. COUNTDOWN_COUNT).reversed()) {
            for (i in 1..5) {
                sendMessage("清空成功 $countDown${".".repeat(i)}")
                delay(200)
            }
        }
        throw Exception()
    }

    fun clearStorage() {
        CoroutineUtils.runIOTask({
            getFileStorageRootDir().deleteRecursively()
            getFileCacheStorageRootDir().deleteRecursively()
            getDBStorageRootDir().deleteRecursively()
        }, onFailure = {
            sendMessage("清空失败")
        }) {
            kill()
        }
    }

    fun clearCache() {
        CoroutineUtils.runIOTask({
            getFileCacheStorageRootDir().deleteRecursively()
        }, onFailure = {
            sendMessage("清空失败")
        }) {
            kill()
        }
    }
}