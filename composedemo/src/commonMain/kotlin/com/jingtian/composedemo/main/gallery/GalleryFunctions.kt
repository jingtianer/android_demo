package com.jingtian.composedemo.main.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
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
    MOVE("移动", { getPainter(DrawableIcon.DrawableMove) });
    companion object {
        // 0 -> 添加, 导入, 修改相册名称
        // 1 -> 编辑, 删除, 移动
        // >1 -> 删除, 移动
        val albumFunctions = listOf(RENAME, ADD, IMPORT).map { it to it }
        val itemFunctions = listOf(EDIT, DELETE, MOVE).map { it to it }
        val batchFunctions = listOf(DELETE, MOVE).map { it to it }
    }
}