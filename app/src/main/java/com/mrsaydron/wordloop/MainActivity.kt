package com.mrsaydron.wordloop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.mrsaydron.wordloop.ui.WordLoopApp
import com.mrsaydron.wordloop.ui.theme.WordLoopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WordLoopTheme {
                WordLoopApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
