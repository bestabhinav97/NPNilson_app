package com.example.np_nilson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.np_nilson.ui.MainApp
import com.example.np_nilson.ui.theme.NP_NilsonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NP_NilsonTheme {
                MainApp()
            }
        }
    }
}
