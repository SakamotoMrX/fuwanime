package com.fuwanime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuwanime.data.local.ThemeMode
import com.fuwanime.ui.navigation.FuwaNavHost
import com.fuwanime.ui.player.LocalPipController
import com.fuwanime.ui.player.PipController
import com.fuwanime.ui.theme.FuwaTheme

class MainActivity : ComponentActivity() {

    private lateinit var pip: PipController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pip = PipController(this)
        val settingsFlow = (application as FuwaApp).container.settingsStore.settings

        setContent {
            val settings by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            // Wait for persisted settings so the first frame already uses the right theme.
            val current = settings ?: return@setContent

            val dark = when (current.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            val view = LocalView.current
            LaunchedEffect(dark) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            FuwaTheme(current) {
                CompositionLocalProvider(LocalPipController provides pip) {
                    FuwaNavHost()
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        pip.onUserLeaveHint()
    }
}
