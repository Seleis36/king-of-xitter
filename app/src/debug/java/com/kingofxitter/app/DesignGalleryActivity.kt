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
import com.kingofxitter.app.ui.board.BoardSamples
import com.kingofxitter.app.ui.board.BoardScreen
import com.kingofxitter.app.ui.board.ComposeSamples
import com.kingofxitter.app.ui.board.ComposeTweetScreen
import com.kingofxitter.app.ui.board.IncomingSamples
import com.kingofxitter.app.ui.board.IncomingTweetScreen
import com.kingofxitter.app.ui.components.ComponentKitSample
import com.kingofxitter.app.ui.components.DiceKitSample
import com.kingofxitter.app.ui.components.IconSetSample
import com.kingofxitter.app.ui.components.StatsKitSample
import com.kingofxitter.app.ui.motion.MotionKitSample
import com.kingofxitter.app.ui.theme.ThemeKitSample
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground

private typealias Sample = @Composable (Modifier) -> Unit

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
        val samples: Map<String, Sample> =
            buildMap {
                put("theme") { modifier -> ThemeKitSample(modifier) }
                put("icons") { modifier -> IconSetSample(modifier) }
                put("components") { modifier -> ComponentKitSample(modifier) }
                put("stats") { modifier -> StatsKitSample(modifier) }
                put("dice") { modifier -> DiceKitSample(modifier) }
                put("motion") { modifier -> MotionKitSample(modifier) }
                BoardSamples.all.forEach { (name, state) ->
                    put("board-$name") { modifier -> BoardScreen(state, BoardSamples.you, onAction = {}, modifier = modifier) }
                }
                put("incoming-design") { modifier ->
                    val state = IncomingSamples.design
                    IncomingTweetScreen(state, BoardSamples.you, state.pendingDecisions.first(), {}, modifier, IncomingSamples.designHit)
                }
                put("incoming-fixture") { modifier ->
                    val state = IncomingSamples.fixture
                    IncomingTweetScreen(state, BoardSamples.you, state.pendingDecisions.first(), {}, modifier)
                }
                ComposeSamples.all.forEach { (name, state) ->
                    put("compose-$name") { modifier ->
                        ComposeTweetScreen(state, BoardSamples.you, onSend = {}, onCancel = {}, modifier = modifier)
                    }
                }
            }
    }
}
