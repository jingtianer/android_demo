package com.jingtian.composedemo.base.resources

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

enum class DrawableIcon {
    DrawableAppThemeAuto,
    DrawableAppThemeLight,
    DrawableAppThemeNight,
    DrawableCheck,
    DrawableUser,
    DrawableTrashBin,
    DrawableDrawer,
    DrawableEditNormal,
    DrawableDown,
    DrawableAdd,
    DrawableImportIcon,
    DrawableDelete,
    DrawableExit,
    DrawableSelectAll,
    DrawableSelectNone,
    DrawableMove,
    DrawableAddGreen,
    DrawableClose,
    DrawableUploadToCloud,
    DrawableMusic,
    DrawableChrome,
    DrawableFile,
    DrawableLoadFailed,
    DrawablePicIcon,
    DrawableVideoIcon,
    DrawableMusicIcon,
    DrawableWebIcon,
    DrawableDocIcon,
    DrawableEdit,
    DrawableCIFS,
}

@Composable
fun getPainter(icon: DrawableIcon): Painter {
    return when(icon) {
        DrawableIcon.DrawableAppThemeAuto -> getAppThemeAutoIcon()
        DrawableIcon.DrawableAppThemeLight -> getAppThemeLightIcon()
        DrawableIcon.DrawableAppThemeNight -> getAppThemeNightIcon()
        DrawableIcon.DrawableCheck -> getCheckIcon()
        DrawableIcon.DrawableUser -> getUserIcon()
        DrawableIcon.DrawableTrashBin -> getTrashBinIcon()
        DrawableIcon.DrawableDrawer -> getDrawerIcon()
        DrawableIcon.DrawableEditNormal -> getEditNormalIcon()
        DrawableIcon.DrawableDown -> getDownIcon()
        DrawableIcon.DrawableAdd -> getAddIcon()
        DrawableIcon.DrawableImportIcon -> getImportIcon()
        DrawableIcon.DrawableDelete -> getDeleteIcon()
        DrawableIcon.DrawableExit -> getExitIcon()
        DrawableIcon.DrawableSelectAll -> getSelectAllIcon()
        DrawableIcon.DrawableSelectNone -> getSelectNoneIcon()
        DrawableIcon.DrawableMove -> getMoveIcon()
        DrawableIcon.DrawableAddGreen -> getAddGreenIcon()
        DrawableIcon.DrawableClose -> getCloseIcon()
        DrawableIcon.DrawableUploadToCloud -> getUploadToCloudIcon()
        DrawableIcon.DrawableMusic -> getMusicIcon()
        DrawableIcon.DrawableChrome -> getChromeIcon()
        DrawableIcon.DrawableFile -> getFileIcon()
        DrawableIcon.DrawableLoadFailed -> getLoadFailedIcon()
        DrawableIcon.DrawablePicIcon -> getPicIcon()
        DrawableIcon.DrawableVideoIcon -> getVideoIcon()
        DrawableIcon.DrawableMusicIcon -> getMusicIconIcon()
        DrawableIcon.DrawableWebIcon -> getWebIcon()
        DrawableIcon.DrawableDocIcon -> getDocIcon()
        DrawableIcon.DrawableEdit -> getEditIcon()
        DrawableIcon.DrawableCIFS -> getCifsIcon()
    }
}

@Composable
expect fun getAppThemeAutoIcon(): Painter

@Composable
expect fun getAppThemeLightIcon(): Painter

@Composable
expect fun getAppThemeNightIcon(): Painter

@Composable
expect fun getCheckIcon(): Painter

@Composable
expect fun getUserIcon(): Painter

@Composable
expect fun getTrashBinIcon(): Painter

@Composable
expect fun getDrawerIcon(): Painter

@Composable
expect fun getEditNormalIcon(): Painter

@Composable
expect fun getDownIcon(): Painter

@Composable
expect fun getAddIcon(): Painter

@Composable
expect fun getImportIcon(): Painter

@Composable
expect fun getDeleteIcon(): Painter

@Composable
expect fun getExitIcon(): Painter

@Composable
expect fun getSelectAllIcon(): Painter

@Composable
expect fun getSelectNoneIcon(): Painter

@Composable
expect fun getMoveIcon(): Painter

@Composable
expect fun getAddGreenIcon(): Painter

@Composable
expect fun getCloseIcon(): Painter

@Composable
expect fun getUploadToCloudIcon(): Painter

@Composable
expect fun getMusicIcon(): Painter

@Composable
expect fun getChromeIcon(): Painter

@Composable
expect fun getFileIcon(): Painter

@Composable
expect fun getLoadFailedIcon(): Painter

@Composable
expect fun getPicIcon(): Painter

@Composable
expect fun getVideoIcon(): Painter

@Composable
expect fun getMusicIconIcon(): Painter

@Composable
expect fun getWebIcon(): Painter

@Composable
expect fun getDocIcon(): Painter

@Composable
expect fun getEditIcon(): Painter

@Composable
expect fun getCifsIcon(): Painter
