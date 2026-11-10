package com.jingtian.composedemo.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.jingtian.composedemo.base.BaseActivity
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.FileType.*
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.FileLinkProvider
import com.jingtian.composedemo.viewmodels.AndroidMigrateViewModel
import com.jingtian.composedemo.viewmodels.AuthViewModel
import com.jingtian.composedemo.viewmodels.authViewModel
import com.jingtian.composedemo.web.WebViewActivity
import java.io.File

class MainActivity : BaseActivity() {
    companion object {
        private const val MEDIA_PERMISSION_REQUEST_CODE = 1001
    }
    private val _viewModel: AuthViewModel = authViewModel()

    private val viewModel: AndroidMigrateViewModel get() = _viewModel as AndroidMigrateViewModel

    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = { callback: BiometricPrompt.AuthenticationCallback ->
            BiometricPrompt(this, executor, callback)
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("验证是你本人")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        viewModel.bioticAuth = AndroidMigrateViewModel.Authenticator(biometricPrompt, promptInfo)
        super.onCreate(savedInstanceState)
//        migrateData()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MEDIA_PERMISSION_REQUEST_CODE) {
        }
    }

    private fun checkAndRequestMediaPermissions() {
        // 判断是否已有所有需要的权限
        val allPermissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                MEDIA_PERMISSION_REQUEST_CODE
            )
        } else {

        }
    }
//    private val viewModel: AndroidMigrateViewModel by viewModels()
//    private fun migrateData() {
//        CoroutineUtils.runIOTask({
//            val targetDir = getFileStorageRootDir()
//            val privateDir = app.filesDir
//            if (targetDir == privateDir) {
//                return@runIOTask
//            }
//            copyDir(privateDir, targetDir)
//            privateDir.deleteRecursively()
//        }, onFailure = { e->
//            e.printStackTrace()
//            viewModel.isMigrationFinished.intValue = 1
//        }) {
//            viewModel.isMigrationFinished.intValue = 1
//        }
//    }


//    @Composable
//    override fun Content() {
//        val isMigrateFinish by remember { viewModel.isMigrationFinished }
//        if (isMigrateFinish > 0) {
//            Main()
//        } else {
//            LaunchedEffect(Unit) {
//                Toast.makeText(this@MainActivity, "正在迁移", Toast.LENGTH_SHORT)
//            }
//        }
//    }

    @Composable
    override fun Content() {
        PasswordGuardedMain {
            checkAndRequestMediaPermissions()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }

    override fun shouldFitSystemBars(): Boolean = false
}

suspend fun systemFallbackIntent(context: Context, fileName: String, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType.mimeType
    val originFileUri = fileInfo.getFileUri()
    val originFile = originFileUri?.file()
    val mediaUri: Uri = if (originFile != null) {
        val tmpLinkFile = FileLinkProvider.get(fileName, fileInfo)?.toFile()
        if (tmpLinkFile != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tmpLinkFile
            )
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(originFile.toString())
            )
        }
    } else {
        (originFileUri as MultiplatformFileImpl?)?.getUri()
    } ?: return null
    return Intent(Intent.ACTION_VIEW).apply {
        // 设置Uri和媒体类型
        setDataAndType(mediaUri, mediaType)
        // 关键：授予系统应用访问该Uri的权限
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // 适配Android 12+的前台服务权限（可选，避免部分播放器启动失败）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

suspend fun webIntent(context: Context, fileName: String, fileInfo: FileInfo) : Intent {
    return Intent(context, WebViewActivity::class.java).apply {
        putExtra(WebViewActivity.KEY_WEB_URI, (fileInfo.getFileUri() as? MultiplatformFileImpl)?.getUri())
        putExtra(WebViewActivity.KEY_STORAGE_ID, fileInfo.storageId)
        putExtra(WebViewActivity.KEY_WEB_URI_EXTENSION, fileInfo.extension ?: "html")
    }
}

suspend fun playIntent(context: Context, fileName: String, fileInfo: FileInfo): Intent? {
    val mediaType = fileInfo.fileType
    return when (mediaType) {
        HTML -> {
            webIntent(context, fileName, fileInfo)
        }

        IMAGE, VIDEO, AUDIO, RegularFile -> systemFallbackIntent(context, fileName, fileInfo)
    }
}
