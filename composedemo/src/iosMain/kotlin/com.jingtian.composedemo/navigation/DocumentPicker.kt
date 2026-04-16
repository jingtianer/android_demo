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
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject

val utTypeImages = listOf(
    UTTypeImage   ,
    UTTypeGIF     ,
    UTTypeJPEG    ,
    UTTypePNG     ,
    UTTypeTIFF    ,
    UTTypeHEIF    ,
    UTTypeHEICS   ,
    UTTypeRAWImage,
    UTTypeDNG     ,
    UTTypeEXR     ,
    UTTypeICO     ,
    UTTypeICNS    ,
    UTTypeSVG     ,
)

val utTypeAudios = listOf(
    UTTypeAudio      ,
    UTTypeMP3        ,
    UTTypeMPEG4Audio ,
    UTTypeWAV        ,
    UTTypeAIFF       ,
    UTTypeMIDI       ,
    UTTypeM3UPlaylist,
    UTTypePlaylist   ,
)

val utTypeVideos = listOf(
    UTTypeVideo                   ,
    UTTypeAVI                     ,
    UTTypeMPEG4Movie              ,
    UTTypeQuickTimeMovie          ,
    UTTypeAppleProtectedMPEG4Video,
    UTTypeMPEG2Video              ,
)

val utTypeWeb = listOf(
    UTTypeText            ,
    UTTypePlainText       ,
    UTTypeHTML            ,
    UTTypeCSS             ,
    UTTypeJSON            ,
    UTTypeXML             ,
    UTTypeYAML            ,
    UTTypeTabSeparatedText,
    UTTypeUTF16PlainText  ,
    UTTypeRTF             ,
)

val utTypeDir = listOf(UTTypeFolder)

val utTypeOther = listOf(
    UTTypePDF       ,
    UTTypeZIP       ,
    UTTypeBZ2       ,
    UTTypeArchive   ,
    UTTypeExecutable,
)

val utTypeAll = utTypeImages + utTypeAudios + utTypeVideos + utTypeWeb + utTypeOther

class DocumentPicker(
    private val extensions: List<*> = utTypeAll,
    private val onResult: (MultiplatformFile?)->Unit,
) : IDocumentPicker {
    override fun launch(mimes: Array<String>) {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: return

        val delegate = DocumentPickerDelegate(onResult)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = extensions, asCopy = false).apply {
            this.delegate = delegate
            allowsMultipleSelection = false
        }
        rootVC.presentViewController(picker, animated = true, completion = null)
    }
}

class DocumentPickerDelegate(private val callback: (MultiplatformFile)->Unit) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        controller.dismissViewControllerAnimated(true, completion = null)
        didPickDocumentsAtURLs
            .filterIsInstance<NSURL>()
            .mapNotNull { url ->
                // 安全域 URL 必须 startAccessing
                val success = url.startAccessingSecurityScopedResource()
                if (success) url.absoluteURL?.path?.also {
                    // 用完记得 stop（建议业务层管理）
                    val path = Path(it)
                    println("DocumentPickerDelegate: documentPicker: $path, ${path.extension}")
                    callback(MultiplatformFileImpl(path, path.extension))
                    url.stopAccessingSecurityScopedResource()
                    url.toString()
                } else null
            }
        println("file: ${didPickDocumentsAtURLs.toTypedArray().contentDeepToString()}")
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        controller.dismissViewControllerAnimated(true, completion = null)
    }
}

@Composable
actual fun rememberDocumentPicker(onResult: (MultiplatformFile?)->Unit): MutableState<IDocumentPicker> {
    return remember {
        mutableStateOf(DocumentPicker(onResult = onResult))
    }
}