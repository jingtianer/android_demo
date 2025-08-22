package com.jingtian.demoapp.main.instrument;

import android.graphics.Canvas;

public class CanvasCaller {
    public boolean clipRect(TestCanvas canvas, float left, float top, float right, float bottom, int regionOp) {
        return canvas.nClipRect(canvas.getNativeCanvasWrapper(), left, top, right, bottom, regionOp);
    }
}
