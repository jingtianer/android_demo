package com.jingtian.demoapp.main.widget

import android.R
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.viewbinding.ViewBinding
import com.jingtian.demoapp.databinding.CustomToastViewBinding
import com.jingtian.demoapp.main.app

object ToastQueue {
    private var toast: Toast? = null

    @Synchronized
    fun show(toast: Toast) {
        this.toast?.cancel()
        this.toast = toast
        toast.show()
    }
}

class ClickableSimpleToast : ClickableToast<CustomToastViewBinding> {
    constructor() : super(app, CustomToastViewBinding.inflate(LayoutInflater.from(app), null, false))
    constructor(context: Context) : super(context, CustomToastViewBinding.inflate(LayoutInflater.from(context), null, false))

    companion object {
        fun show(context: Context?, charSequence: CharSequence, duration: Int) {
            ClickableSimpleToast(context ?: app).apply {
                binding.root.apply {
                    highlightColor = Color.TRANSPARENT
                    movementMethod = LinkMovementMethod.getInstance()
                    text = charSequence
                }
                this.duration = duration
                ToastQueue.show(this)
            }
        }
    }
}

open class ClickableToast<B : ViewBinding>(context: Context, binding: B) : CustomToast<B>(context, binding) {

    private val windowManager = (context as? Activity)?.windowManager

    private fun initWindowParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams()
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION
        params.format = PixelFormat.TRANSPARENT
        params.gravity = gravity
        params.x = xOffset
        params.y = yOffset
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.windowAnimations = R.style.Animation_Toast
        return params
    }

    override fun show() {
        if (windowManager == null) {
            super.show()
            return
        }
        val params = initWindowParams()
        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun cancel() {
        if (windowManager == null) {
            super.cancel()
            return
        }
        val contentView = view
        if (contentView != null && contentView.parent != null) {
            try {
                windowManager.removeView(contentView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

open class CustomToast<B : ViewBinding>(context: Context, val binding: B) : Toast(context) {
    init {
        view = binding.root
    }
}