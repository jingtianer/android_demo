package com.jingtian.composedemo

import androidx.collection.objectListOf
import androidx.compose.runtime.Composable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.jingtian.composedemo.base.BaseActivity

class MainActivity : BaseActivity() {
    @Composable
    override fun content() = Main()
}



@OptIn(ExperimentalMaterial3Api::class)
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