package com.jingtian.demoapp.main.instrument;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class TestActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, getClass().getSimpleName() + "插装成功！", Toast.LENGTH_LONG).show();
    }
}
