package com.jingtian.composedemo

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main(args: Array<String>) {
    application {
        MainWindow()
    }
}

@Composable
fun ApplicationScope.MainWindow() {
    Window(onCloseRequest = {
        this.exitApplication()
    }) {
        MaterialTheme {
            Text("Click me!")
        }
    }
}