package com.kenews.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Surface
import com.kenews.app.ui.NewsScreen
import com.kenews.app.ui.theme.KeNewsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeNewsTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    NewsScreen()
                }
            }
        }
    }
}
