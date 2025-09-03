package com.jingtian.demoapp.main.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.View

open class SharedElementParcelable : Parcelable {
    val bundle: Bundle

    constructor(parcel: Parcel) : super() {
        this.bundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Bundle::class.java.classLoader, Bundle::class.java)
        } else {
            parcel.readParcelable(Bundle::class.java.classLoader) as? Bundle
        } ?: Bundle()
    }

    constructor() : super() {
        this.bundle = Bundle()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(bundle, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SharedElementParcelable> {
        override fun createFromParcel(parcel: Parcel): SharedElementParcelable {
            return SharedElementParcelable(parcel)
        }

        override fun newArray(size: Int): Array<SharedElementParcelable?> {
            return arrayOfNulls(size)
        }
    }

    open fun createSnapShotView(
        context: Context
    ): View {
        return View(context)
    }

}

interface ISharedElement {
    fun onCaptureSharedElementSnapshot(): SharedElementParcelable
    fun applySnapShot(snapshot: SharedElementParcelable?, isStart: Boolean): View
}