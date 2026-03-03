package com.zephyrus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zephyrus.app.ui.navigation.ZephyrusNavHost
import com.zephyrus.app.ui.theme.ZephyrusTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity created")
        enableEdgeToEdge()
        setContent {
            ZephyrusTheme {
                ZephyrusNavHost()
            }
        }
    }
}
