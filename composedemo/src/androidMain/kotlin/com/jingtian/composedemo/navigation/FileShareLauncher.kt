package com.jingtian.composedemo.navigation

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.jingtian.composedemo.base.app
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.ensureDirExist
import com.jingtian.composedemo.utils.ensureFile
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random
import kotlin.random.nextUInt

class FileShareLauncher(val context: Context) : IFileShareLauncher {
    companion object {
        private val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let {
            File(it, "shareDb")
        }

        init {
            downloadDir?.ensureDirExist()
        }
    }

    override fun launch(file: MultiplatformFile) {
        val sharedFile = file.file
        val uri = if (sharedFile != null) {
            FileProvider.getUriForFile(
                app,
                app.packageName + ".fileprovider",
                sharedFile
            )
        } else {
            (file as MultiplatformFileImpl).uri
        }
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "*/*"
        }

        CoroutineUtils.runIOTask({
            val shareFile: File? = downloadDir?.let { downloadDir->
                file.fileName?.let { shareFileName->
                    val shareFile = File(downloadDir, shareFileName)
                    file.inputStream?.use { `is`->
                        shareFile.ensureFile()
                        FileOutputStream(shareFile).use { os->
                            `is`.copyTo(os)
                            os.flush()
                        }
                        return@use shareFile
                    }
                }
            }
            return@runIOTask shareFile
        }, callback = { path: File?->
            path?.let {
                Toast.makeText(app, "已保存到${path.path}", Toast.LENGTH_SHORT).show()
            }
        })
        context.startActivity(Intent.createChooser(shareIntent, "分享文件"))
    }
}
@Composable
actual fun rememberFileShare(onResult: (Long)->Unit): MutableState<IFileShareLauncher> {
    val context: Context = LocalContext.current
    return remember {
        mutableStateOf(FileShareLauncher(context))
    }
}