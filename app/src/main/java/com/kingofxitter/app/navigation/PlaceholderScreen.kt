package com.kingofxitter.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground

/** Stand-in for a destination whose screen hasn't landed yet. Lists the routes it leads to. */
@Composable
fun PlaceholderScreen(
    route: XitterRoute,
    links: List<XitterRoute>,
    onNavigate: (XitterRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .nightBackground()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(route.titleRes),
            style = XType.DisplayLarge,
            color = XColors.Mandates,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.placeholder_coming_soon),
            style = XType.Body,
            color = XColors.Muted,
            textAlign = TextAlign.Center,
        )
        links.forEach { link ->
            Button(
                onClick = { onNavigate(link) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(link.titleRes))
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun PlaceholderScreenPreview() {
    XitterTheme {
        PlaceholderScreen(XitterRoute.Board, listOf(XitterRoute.ComposeTweet, XitterRoute.Shop), onNavigate = {})
    }
}
