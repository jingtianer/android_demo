package com.jingtian.composedemo

import androidx.collection.mutableObjectListOf
import androidx.collection.objectListOf
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.AppThemeTextField
import com.jingtian.composedemo.base.BaseActivity

class MainActivity : BaseActivity() {
    @Composable
    override fun content() = Main()
}



@Preview
@Composable
fun Main() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val rememberScope = rememberCoroutineScope()
}

object Drawer {
    class DrawerItem(val name: String)
    private val drawerList = objectListOf<DrawerItem>()
}