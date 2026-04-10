package com.jingtian.composedemo.main.gallery

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.jingtian.composedemo.base.resources.*

actual fun platformExtraAlbumFunctions(selectCount: Int) : List<GalleryFunctions> = listOf()


@Composable
actual fun RowScope.PlatformGalleryFunctionView(platformExtra: GalleryStateHolder, func: GalleryFunctions, onClick: ()->Unit) {}