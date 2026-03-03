package com.jingtian.composedemo.main.drawer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jingtian.composedemo.R
import com.jingtian.composedemo.base.AppThemeBasicTextField
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.dao.DataBase
import com.jingtian.composedemo.dao.model.DEFAULT_DESC
import com.jingtian.composedemo.dao.model.DEFAULT_USER_NAME
import com.jingtian.composedemo.dao.model.FileType
import com.jingtian.composedemo.main.playIntent
import com.jingtian.composedemo.ui.theme.LocalAppPalette
import com.jingtian.composedemo.ui.theme.LocalSecondaryTextStyle
import com.jingtian.composedemo.ui.theme.drawerBackground
import com.jingtian.composedemo.ui.theme.goldenRatio
import com.jingtian.composedemo.utils.BitMapCachePool
import com.jingtian.composedemo.utils.CoroutineUtils
import com.jingtian.composedemo.utils.FileStorageUtils
import com.jingtian.composedemo.utils.FileStorageUtils.isHidden
import com.jingtian.composedemo.utils.UserStorage
import com.jingtian.composedemo.utils.ViewUtils.dpValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@Composable
fun DrawerHeader() {
    var userName by remember { mutableStateOf(DEFAULT_USER_NAME) }
    var userDesc by remember { mutableStateOf(DEFAULT_DESC) }
    var userAvatarImage by remember { mutableStateOf<ImageBitmap?>(null) }

    val avatarSize = DrawerDefaults.MaximumDrawerWidth * (1 - goldenRatio)
    val borderSize = 2.dp
    val iconSize = avatarSize / 8f

    val scope = rememberCoroutineScope()
    var editUserInfoJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        val innerUserInfo = UserStorage.userInstance
        userName = innerUserInfo.userName
        userDesc = innerUserInfo.userDesc
    }
    LaunchedEffect(Unit) {
        val fileInfo = UserStorage.userInstance.userAvatar
        val bitmap = withContext(Dispatchers.IO) {
            val (_, image) = BitMapCachePool.loadImage(
                fileInfo,
                avatarSize.dpValue.toInt(),
                avatarSize.dpValue.toInt()
            )
            image
        }
        userAvatarImage = bitmap?.asImageBitmap()
    }

    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            uri?.takeIf { !it.isHidden() } ?: return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val imageStorage = FileStorageUtils.getStorage(FileType.IMAGE) ?: return@launch
                val currentUser = UserStorage.userInstance
                val currentImageId =
                    currentUser.userAvatar.storageId.takeIf { it != DataBase.INVALID_ID }
                val nextId = if (currentImageId != null) {
                    BitMapCachePool.invalid(currentImageId, FileType.IMAGE)
                    imageStorage.asyncStore(currentImageId, uri)
                } else {
                    imageStorage.asyncStore(uri)
                }
                currentUser.userAvatar.storageId = nextId
                currentUser.userAvatar.fileType = FileType.IMAGE
                currentUser.userAvatar.uri = uri
                UserStorage.userInstance = currentUser
                val (_, image) = BitMapCachePool.loadImage(
                    currentUser.userAvatar,
                    avatarSize.dpValue.toInt(),
                    avatarSize.dpValue.toInt()
                )
                val bitmap = image?.asImageBitmap()
                withContext(Dispatchers.Main) {
                    userAvatarImage = bitmap
                }
            }
        }
    )

    var enableEdit by remember { mutableStateOf(false) }

    fun onUserNameChange(value: String) {
        val userInstance = UserStorage.userInstance
        userInstance.userName = value
        userName = value
        editUserInfoJob?.cancel()
        editUserInfoJob = CoroutineUtils.runIOTask({
            UserStorage.userInstance = userInstance
        })
    }

    fun onUserDescChange(value: String) {
        val userInstance = UserStorage.userInstance
        userInstance.userDesc = value
        userDesc = value
        editUserInfoJob?.cancel()
        editUserInfoJob = CoroutineUtils.runIOTask({
            UserStorage.userInstance = userInstance
        })
    }

    fun pickImage() {
        multipleImagePickerLauncher.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                .build()
        )
    }

    fun circleOffset(R: Dp, r: Dp, halfBorder: Dp) : Dp {
        return R * (1 - sqrt(.5f)) - r + halfBorder * sqrt(0.5f)
    }

    val context = LocalContext.current
    val playIntent = remember(UserStorage.userInstance) {
        playIntent(context, UserStorage.userInstance.userAvatar)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .drawerBackground()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val currentUserAvatarImage = userAvatarImage

        val imageModifier = Modifier
            .size(avatarSize, avatarSize)
            .clip(CircleShape)
            .border(borderSize, LocalAppPalette.current.strokeColor, CircleShape)
            .clickable {
                if (enableEdit) {
                    pickImage()
                } else {
                    if (playIntent != null) {
                        context.startActivity(playIntent)
                    }
                }
            }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, top = 6.dp, end = 6.dp)
                .background(
                    color = LocalAppPalette.current.cardBg, shape = RoundedCornerShape(4.dp)
                )
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 6.dp, horizontal = 6.dp)
        ) {
            Row {
                Box {
                    if (currentUserAvatarImage == null) {
                        Image(
                            painter = painterResource(R.drawable.user),
                            contentDescription = "头像",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Image(
                            bitmap = currentUserAvatarImage,
                            contentDescription = "头像",
                            modifier = imageModifier,
                            contentScale = ContentScale.Crop,
                        )
                    }
                    if (enableEdit) {
                        Image(
                            painter = painterResource(
                                R.drawable.edit
                            ),
                            contentDescription = "编辑用户信息",
                            Modifier
                                .size(iconSize, iconSize)
                                .clickable {
                                    enableEdit = !enableEdit
                                }
                                .align(Alignment.BottomEnd)
                                .clickable {
                                    if (enableEdit) {
                                        pickImage()
                                    } else {
                                        if (playIntent != null) {
                                            context.startActivity(playIntent)
                                        }
                                    }
                                }
                                .offset(
                                    -circleOffset(
                                        avatarSize / 2f, iconSize / 2f, borderSize / 2f
                                    ),
                                    -circleOffset(avatarSize / 2f, iconSize / 2f, borderSize / 2f)
                                )
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.align(Alignment.CenterVertically)) {
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight(600),
                        )
                    ) {
                        if (enableEdit) {
                            AppThemeBasicTextField(
                                value = userName,
                                onValueChange = { value ->
                                    onUserNameChange(value)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入用户名",
                            )
                        } else {
                            AppThemeText(
                                text = userName,
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入用户名",
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalSecondaryTextStyle.current.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight(600),
                        )
                    ) {

                        if (enableEdit) {
                            AppThemeBasicTextField(
                                value = userDesc,
                                onValueChange = { value ->
                                    onUserDescChange(value)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入个性签名",
                            )
                        } else {
                            AppThemeText(
                                text = userDesc,
                                modifier = Modifier.fillMaxWidth(),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                hint = "输入个性签名",
                            )
                        }
                    }
                }
            }
            Image(
                painter = painterResource(
                    if (enableEdit) {
                        R.drawable.close
                    } else {
                        R.drawable.edit
                    }
                ),
                contentDescription = "编辑用户信息",
                Modifier
                    .size(16.dp)
                    .clickable {
                        enableEdit = !enableEdit
                    }
                    .align(Alignment.BottomEnd)
            )
        }
        DrawerFunctionArea()
    }
}