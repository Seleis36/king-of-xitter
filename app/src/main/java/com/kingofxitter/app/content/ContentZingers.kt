package com.kingofxitter.app.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kingofxitter.app.ui.board.ZingerSource
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.content.TweetVoices
import com.kingofxitter.content.ZingerBook
import com.kingofxitter.engine.model.LeaderId

/**
 * V2's zingers behind U6's [ZingerSource]. The book holds three lines per attacker and target,
 * which is exactly one screenful, so "New takes" also draws on the attacker's V1 attack lines,
 * which name any target. It walks the pool in order and comes back round; the same page always
 * offers the same lines.
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
        val handle = target?.let(texts::handle) ?: texts.everyoneHandle
        return keys(attacker, target, page, count).map { texts.text(it, handle) }
    }

    companion object {
        /** The string keys on [page]: the pair's own lines first, then the attacker's attack lines. */
        fun keys(
            attacker: LeaderId,
            target: LeaderId?,
            page: Int,
            count: Int,
        ): List<String> {
            val written = ZingerBook.keys(attacker, target).ifEmpty { ZingerBook.keys(attacker, null) }
            if (written.isEmpty()) return emptyList()
            val pool = written + TweetVoices.keys(attacker, TweetMoment.ATTACKED)
            val start = Math.floorMod(page * count, pool.size)
            return List(minOf(count, pool.size)) { index -> pool[(start + index) % pool.size] }
        }
    }
}

/** A [ContentZingers] bound to the composition's resources. */
@Composable
fun rememberContentZingers(): ZingerSource {
    val texts = rememberTweetTexts()
    return remember(texts) { ContentZingers(texts) }
}
