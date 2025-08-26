package com.jingtian.demoapp.main.base

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

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
    }
    private val mediaPickerCallbacks = mutableListOf<MediaPickerCallback>()
    private val documentPickerCallbacks = mutableListOf<DocumentPickerCallback>()

    val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            documentPickerCallbacks.forEach {
                it.onDocumentCallback(uri)
            }
        }
    }

    fun addMediaPickerCallbacks(mediaPickerCallback: MediaPickerCallback) {
        if (mediaPickerCallback !in mediaPickerCallbacks) {
            mediaPickerCallbacks.add(mediaPickerCallback)
        }
    }

    fun removeMediaPickerCallbacks(mediaPickerCallback: MediaPickerCallback) {
        if (mediaPickerCallback in mediaPickerCallbacks) {
            mediaPickerCallbacks.add(mediaPickerCallback)
        }
    }

    fun addDocumentPickerCallbacks(documentPickerCallback: DocumentPickerCallback) {
        if (documentPickerCallback !in documentPickerCallbacks) {
            documentPickerCallbacks.add(documentPickerCallback)
        }
    }

    fun removeDocumentPickerCallbacks(documentPickerCallback: DocumentPickerCallback) {
        if (documentPickerCallback in documentPickerCallbacks) {
            documentPickerCallbacks.add(documentPickerCallback)
        }
    }
}