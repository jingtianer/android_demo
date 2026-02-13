package com.jingtian.composedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContent {
            HelloWorld()
        }
//        ViewCompat.setOnApplyWindowInsetsListener() { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }
}

@Preview
@Composable
fun HelloWorld() {
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
        BasicText(
            "hello $value",
            Modifier.fillMaxWidth()
                .padding(vertical = 6.dp),
            style = TextStyle()
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            Modifier.fillMaxWidth()
                .padding(4.dp)
                .border(1.dp, Color.Cyan, shape = RoundedCornerShape(4.dp))
                .padding(3.dp)
                .border(2.dp, Color.Red, shape = RoundedCornerShape(4.dp))
                .padding(4.dp)
            ,
        )
    }
}