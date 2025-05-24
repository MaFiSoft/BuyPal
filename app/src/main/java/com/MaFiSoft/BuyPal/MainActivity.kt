package com.MaFiSoft.BuyPal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuyPalApp()
        }
    }
}

@Composable
fun BuyPalApp() {
    MaterialTheme {
        Text("Hello, Jetpack Compose!")
    }
}

@Preview
@Composable
fun BuyPalAppPreview() {
    BuyPalApp()
}
