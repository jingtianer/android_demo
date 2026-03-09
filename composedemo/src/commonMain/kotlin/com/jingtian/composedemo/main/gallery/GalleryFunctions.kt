package com.jingtian.composedemo.main.gallery

import demoapp.composedemo.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

enum class GalleryFunctions(val functionName: String, val resId: DrawableResource) {
    ADD("添加", Res.drawable.add),
    IMPORT("导入", Res.drawable.import_icon),
    RENAME("重命名", Res.drawable.edit_normal),
    EDIT("编辑", Res.drawable.edit_normal),
    DELETE("删除", Res.drawable.delete),
    EXIT("退出", Res.drawable.exit),
    SELECT_ALL("全选", Res.drawable.select_all),
    SELECT_NONE("全不选", Res.drawable.select_none),
    MOVE("移动", Res.drawable.move);
    companion object {
        // 0 -> 添加, 导入, 修改相册名称
        // 1 -> 编辑, 删除, 移动
        // >1 -> 删除, 移动
        val albumFunctions = listOf(RENAME, ADD, IMPORT).map { it to it }
        val itemFunctions = listOf(EDIT, DELETE, MOVE).map { it to it }
        val batchFunctions = listOf(DELETE, MOVE).map { it to it }
    }
}