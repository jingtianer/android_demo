package com.jingtian.composedemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.jingtian.composedemo.multiplatform.MultiplatformFile
import com.jingtian.composedemo.multiplatform.MultiplatformFileImpl
import com.jingtian.composedemo.utils.extension
import kotlinx.io.files.Path
import platform.Foundation.NSLog
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerImageURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject

class ImagePicker(
    private val extensions: List<UTType> = utTypeImages,
    private val onResult: (MultiplatformFile?)->Unit,
): IImagePicker {
    override fun launch() {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return

        val delegate = ImagePickDelegate(onResult)
        val picker = UIImagePickerController()
        picker.delegate = delegate
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary

        // 只允许选择图片
        picker.mediaTypes = extensions.map { it.identifier }
        picker.allowsEditing = false
        rootVC.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberImagePicker(onResult: (MultiplatformFile?)->Unit): MutableState<IImagePicker> {
    return remember { mutableStateOf(ImagePicker(onResult = onResult)) }
}

class ImagePickDelegate(private val onResult: (MultiplatformFile?)->Unit) : NSObject(), UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
    // 选择完成
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, null)

//        NSLog("ImagePickDelegate: imagePickerController")
        // 取出图片
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerImageURL] as? NSURL ?: return
        val success = image.startAccessingSecurityScopedResource()
//        NSLog("ImagePickDelegate: imagePickerController $image, $success")
        val path = Path(image.absoluteURL?.path ?: return)
//        NSLog("ImagePickDelegate: imagePickerController $path")
        onResult(MultiplatformFileImpl(path, path.extension))
    }

    // 取消选择
    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
    }
}