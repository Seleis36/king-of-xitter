package com.kingofxitter.app.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kingofxitter.app.ui.board.ZingerSource
import com.kingofxitter.content.ZingerBook
import com.kingofxitter.engine.model.LeaderId

/**
 * V2's zingers behind U6's [ZingerSource]. The book holds three lines per attacker and target, so
 * "New takes" walks through them in order and comes back round; the same page always offers the
 * same lines.
 *
 * A pair with no lines (a leader outside the roster) falls back to the whole-timeline set, and
 * then to nothing, so the compose screen never shows a blank option.
 */
class ContentZingers(
    private val texts: TweetTexts,
) : ZingerSource {
    override fun zingers(
        attacker: LeaderId,
        target: LeaderId?,
        page: Int,
        count: Int,
    ): List<String> {
        val keys = ZingerBook.keys(attacker, target).ifEmpty { ZingerBook.keys(attacker, null) }
        if (keys.isEmpty()) return emptyList()
        val handle = target?.let(texts::handle) ?: texts.everyoneHandle
        val start = Math.floorMod(page * count, keys.size)
        return List(minOf(count, keys.size)) { index -> texts.text(keys[(start + index) % keys.size], handle) }
    }
}

/** A [ContentZingers] bound to the composition's resources. */
@Composable
fun rememberContentZingers(): ZingerSource {
    val texts = rememberTweetTexts()
    return remember(texts) { ContentZingers(texts) }
}
