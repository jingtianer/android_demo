package com.jingtian.demoapp.main.base

import android.net.Uri
import android.os.Looper
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat

open class BaseActivity : AppCompatActivity() {
    companion object {
        interface MediaPickerCallback {
            fun onMediaCallback(uri: Uri)
        }
        interface DocumentPickerCallback {
            fun onDocumentCallback(uri: Uri)
        }
    }
    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            mediaPickerCallbacks.forEach {
                it.onMediaCallback(uri)
            }
        } else {
            mediaPickerCallbacks.forEach {
                it.onMediaCallback(Uri.EMPTY)
            }
        }
        mediaPickerCallbacks.clear()
    }
    private val mediaPickerCallbacks = mutableListOf<MediaPickerCallback>()
    private val documentPickerCallbacks = mutableListOf<DocumentPickerCallback>()

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            documentPickerCallbacks.forEach {
                it.onDocumentCallback(uri)
            }
            documentPickerCallbacks.clear()
        }
    }

    fun pickFile(input: Array<String>, options: ActivityOptionsCompat?, documentPickerCallback: DocumentPickerCallback) {
        addDocumentPickerCallbacks(documentPickerCallback)
        pickFile.launch(input, options)
    }

    fun pickMedia(input: PickVisualMediaRequest, options: ActivityOptionsCompat?, mediaPickerCallback: MediaPickerCallback) {
        addMediaPickerCallbacks(mediaPickerCallback)
        pickMedia.launch(input, options)
    }

    private fun addMediaPickerCallbacks(mediaPickerCallback: MediaPickerCallback) {
        if (mediaPickerCallback !in mediaPickerCallbacks) {
            mediaPickerCallbacks.add(mediaPickerCallback)
        }
    }

    private fun addDocumentPickerCallbacks(documentPickerCallback: DocumentPickerCallback) {
        if (documentPickerCallback !in documentPickerCallbacks) {
            documentPickerCallbacks.add(documentPickerCallback)
        }
    }
}