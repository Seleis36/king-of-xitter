package com.kingofxitter.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kingofxitter.app.navigation.XitterNavHost
import com.kingofxitter.app.ui.front.rememberGamePreferences
import com.kingofxitter.app.ui.motion.LocalMotion
import com.kingofxitter.app.ui.motion.rememberSystemMotion
import com.kingofxitter.app.ui.theme.XitterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always on the dark Night background, so system bar icons stay light.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)
        setContent {
            XitterTheme {
                // V5's animation settings, narrowed by the system's "Remove animations".
                val settings by rememberGamePreferences().settings.collectAsState()
                val system = rememberSystemMotion()
                CompositionLocalProvider(LocalMotion provides settings.motion(system.enabled)) {
                    XitterNavHost()
                }
            }
        }
    }
}
