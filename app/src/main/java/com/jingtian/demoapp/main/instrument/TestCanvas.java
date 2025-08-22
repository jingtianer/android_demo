package com.jingtian.demoapp.main.instrument;

import android.graphics.Canvas;

public class TestCanvas extends Canvas {

    public long mNativeCanvasWrapper;

    public long getNativeCanvasWrapper() {
        return mNativeCanvasWrapper;
    }

    static native boolean nClipRect(long nativeCanvas,
                                    float left, float top, float right, float bottom, int regionOp);
}
