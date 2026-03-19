package com.zephyrus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zephyrus.app.data.local.UserPreferences
import com.zephyrus.app.domain.model.ThemeMode
import com.zephyrus.app.ui.navigation.ZephyrusNavHost
import com.zephyrus.app.ui.theme.ZephyrusTheme
import com.zephyrus.app.ui.update.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private val updateViewModel: UpdateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity created")
        enableEdgeToEdge()

        // Silent update check on launch (24h throttle)
        updateViewModel.checkOnLaunch()

        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            ZephyrusTheme(darkTheme = darkTheme) {
                ZephyrusNavHost(updateViewModel = updateViewModel)
            }
        }
    }
}
