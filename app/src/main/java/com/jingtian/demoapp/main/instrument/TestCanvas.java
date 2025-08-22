package com.jingtian.demoapp.main.instrument;

import android.graphics.Canvas;

public class TestCanvas extends Canvas {

    protected long mNativeCanvasWrapper;

    private static native boolean nClipRect(long nativeCanvas,
                                            float left, float top, float right, float bottom, int regionOp);

    public void clipRect(float left, float top, float right, float bottom, int regionOp) {
        nClipRect(mNativeCanvasWrapper, left, top, right, bottom, regionOp);
    }
}
