package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.resources.*

enum class GalleryFunctions(val functionName: String, val iconProvider: @Composable () -> Painter) {
    ADD("添加", { getPainter(DrawableIcon.DrawableAdd) }),
    IMPORT("导入", { getPainter(DrawableIcon.DrawableImportIcon) }),
    RENAME("重命名", { getPainter(DrawableIcon.DrawableEditNormal) }),
    EDIT("编辑", { getPainter(DrawableIcon.DrawableEditNormal) }),
    DELETE("删除", { getPainter(DrawableIcon.DrawableDelete) }),
    EXIT("退出", { getPainter(DrawableIcon.DrawableExit) }),
    SELECT_ALL("全选", { getPainter(DrawableIcon.DrawableSelectAll) }),
    SELECT_NONE("全不选", { getPainter(DrawableIcon.DrawableSelectNone) }),
    MOVE("移动", { getPainter(DrawableIcon.DrawableMove) }),
    IMPORT_CIFS("远程", { getPainter(DrawableIcon.DrawableCIFS) });
    companion object {
        // 0 -> 添加, 导入, 修改相册名称
        // 1 -> 编辑, 删除, 移动
        // >1 -> 删除, 移动
        val albumFunctions = listOf(RENAME, ADD, IMPORT)
        val itemFunctions = listOf(EDIT, DELETE, MOVE)
        val batchFunctions = listOf(DELETE, MOVE)
    }
}

expect fun platformExtraAlbumFunctions() : List<GalleryFunctions>

@Composable
expect fun RowScope.PlatformGalleryFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit)