package com.kingofxitter.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kingofxitter.app.ui.components.IconSetSample
import com.kingofxitter.app.ui.theme.ThemeKitSample
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground

/**
 * Debug-only host that renders one design sample on a device, for checking components against
 * `design/screens/`. Not included in release builds.
 *
 * `adb shell am start -n com.kingofxitter/com.kingofxitter.app.DesignGalleryActivity --es sample theme`
 */
class DesignGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        super.onCreate(savedInstanceState)
        val sample = samples[intent.getStringExtra("sample")] ?: samples.getValue("theme")
        setContent {
            XitterTheme { sample(Modifier.fillMaxSize().nightBackground().safeDrawingPadding()) }
        }
    }

    private companion object {
        /** Sample name to composable. Each UI task adds its samples here. */
        val samples: Map<String, @Composable (Modifier) -> Unit> =
            mapOf(
                "theme" to { modifier -> ThemeKitSample(modifier) },
                "icons" to { modifier -> IconSetSample(modifier) },
            )
    }
}
