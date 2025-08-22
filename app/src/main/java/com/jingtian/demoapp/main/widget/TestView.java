package com.jingtian.demoapp.main.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class TestView extends View {
    public TestView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setScaleX(2f);
        setScaleY(2f);
    }
}
