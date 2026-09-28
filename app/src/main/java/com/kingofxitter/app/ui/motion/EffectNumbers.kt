package com.kingofxitter.app.ui.motion

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType

/** Rising numbers (`rise`) for [effects]: "−3M" in red, "+2" in gold, "+10" in green. */
@Composable
fun EffectNumbers(
    effects: List<BoardEffect>,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        effects.filter { it.isNumber }.forEach { effect ->
            key(effect.id) {
                OutlinedText(
                    text = effectText(effect),
                    style = XType.DisplayLarge.copy(fontSize = 24.sp),
                    color = effectColor(effect.kind),
                    modifier = Modifier.rise(),
                    outline = 4.dp,
                )
            }
        }
    }
}

@Composable
private fun effectText(effect: BoardEffect): String =
    when (effect.kind) {
        BoardEffect.Kind.Damage, BoardEffect.Kind.Heal -> stringResource(R.string.effect_followers, signed(effect.amount))
        else -> signed(effect.amount)
    }

/** "+2" or "−3" with a real minus sign. */
internal fun signed(amount: Int): String = if (amount < 0) "−${-amount}" else "+$amount"

private fun effectColor(kind: BoardEffect.Kind): Color =
    when (kind) {
        BoardEffect.Kind.Damage -> XColors.Followers
        BoardEffect.Kind.Heal -> XColors.Paper
        BoardEffect.Kind.Mandates -> XColors.Mandates
        else -> XColors.Characters
    }

/** Latest effect of [kind] for [playerId], used as the key that replays a one-shot animation. */
fun List<BoardEffect>.latest(
    kind: BoardEffect.Kind,
    playerId: Any?,
): Long? = lastOrNull { it.kind == kind && it.playerId == playerId }?.id
