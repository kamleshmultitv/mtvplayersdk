package com.app.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.app.sample.composable.ContentScreen
import com.app.sample.viewModel.ContentViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ContentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContentScreen(viewModel = viewModel)
        }
    }

}