package com.jingtian.composedemo.main.gallery

import androidx.annotation.DrawableRes
import com.jingtian.composedemo.R

enum class GalleryFunctions(val functionName: String, @DrawableRes val resId: Int) {
    ADD("添加", R.drawable.add),
    IMPORT("导入", R.drawable.import_icon),
    RENAME("重命名", R.drawable.edit_normal),
    EDIT("编辑", R.drawable.edit_normal),
    DELETE("删除", R.drawable.delete),
    EXIT("退出", R.drawable.exit),
    SELECT_ALL("全选", R.drawable.select_all),
    SELECT_NONE("全不选", R.drawable.select_none),
    MOVE("移动", R.drawable.move);
    companion object {
        // 0 -> 添加, 导入, 修改相册名称
        // 1 -> 编辑, 删除, 移动
        // >1 -> 删除, 移动
        val albumFunctions = listOf(RENAME, ADD, IMPORT).map { it to it }
        val itemFunctions = listOf(EDIT, DELETE, MOVE).map { it to it }
        val batchFunctions = listOf(DELETE, MOVE).map { it to it }
    }
}