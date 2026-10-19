package com.kingofxitter.app

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.navigation.XitterNavHost
import com.kingofxitter.app.ui.front.rememberGamePreferences
import com.kingofxitter.app.ui.motion.LocalMotion
import com.kingofxitter.app.ui.motion.rememberSystemMotion
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always on the dark Night background, so system bar icons stay light.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)
        // Every screen is designed as a portrait phone (412x915), and a phone held sideways is
        // too short for the board. Large screens have the room, so they keep both orientations.
        if (resources.configuration.smallestScreenWidthDp < LARGE_SCREEN_WIDTH_DP) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
        setContent {
            XitterTheme {
                // V5's animation settings, narrowed by the system's "Remove animations".
                val settings by rememberGamePreferences().settings.collectAsState()
                val system = rememberSystemMotion()
                CompositionLocalProvider(LocalMotion provides settings.motion(system.enabled)) {
                    // On a tablet the phone-shaped screens sit in a centred column instead of
                    // stretching their dice and buttons across the whole width.
                    Box(Modifier.fillMaxSize().nightBackground(), contentAlignment = Alignment.TopCenter) {
                        XitterNavHost(Modifier.widthIn(max = MAX_CONTENT_WIDTH))
                    }
                }
            }
        }
    }

    private companion object {
        /** Android's smallest width for a large screen (a 7-inch tablet and up). */
        const val LARGE_SCREEN_WIDTH_DP = 600

        /** Widest the game's column gets on a large screen. */
        val MAX_CONTENT_WIDTH = 600.dp
    }
}
