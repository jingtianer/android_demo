package com.jingtian.composedemo.base

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import com.jingtian.composedemo.main.gallery.GalleryStateHolder

@Composable
expect fun GalleryStateHolder.BackPressHandler(drawerState: DrawerState, enterEditMode: Boolean, onBackPressed: ()->Unit)