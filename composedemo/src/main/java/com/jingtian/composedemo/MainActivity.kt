package com.jingtian.composedemo

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jingtian.composedemo.base.AppThemeText
import com.jingtian.composedemo.base.AppThemeTextField
import com.jingtian.composedemo.base.BaseActivity

class MainActivity : BaseActivity() {
    @Composable
    override fun content() = Main()
}

@Preview
@Composable
fun Main() {
    var name by rememberSaveable { mutableStateOf("") }
    HelloWorldText(name) { name = it }
}

@Composable
fun HelloWorldText(value: String, onValueChange: (String)-> Unit) {
    Column(
        Modifier
            .systemBarsPadding()
            .fillMaxSize(1f)
            .padding(horizontal = 12.dp)
            .wrapContentSize(Alignment.Center),
    ) {
        AppThemeText(
            "hello $value",
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
        )
        AppThemeTextField(
            value = value,
            onValueChange = onValueChange,
            Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .border(1.dp, Color.Cyan, shape = RoundedCornerShape(4.dp))
                .padding(3.dp)
                .border(2.dp, Color.Red, shape = RoundedCornerShape(4.dp))
                .padding(4.dp)
            ,
        )
    }
}