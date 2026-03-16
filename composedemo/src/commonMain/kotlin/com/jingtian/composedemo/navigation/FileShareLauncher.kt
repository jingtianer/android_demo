package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.jingtian.composedemo.dao.model.FileInfo
import com.jingtian.composedemo.dao.model.relation.AlbumItemRelation
import com.jingtian.composedemo.multiplatform.MultiplatformFile

interface IFileShareLauncher {
    fun launch(file: MultiplatformFile)
}
@Composable
expect fun rememberFileShare(onResult: (Long)->Unit): MutableState<IFileShareLauncher>