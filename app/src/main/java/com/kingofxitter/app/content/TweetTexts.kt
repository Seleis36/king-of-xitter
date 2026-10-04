package com.kingofxitter.app.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.kingofxitter.app.R
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.content.TemplateTweetWriter
import com.kingofxitter.content.Tweet
import com.kingofxitter.content.TweetEvent
import com.kingofxitter.content.TweetWriter
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.LeaderId

/**
 * Renders the tweets V1 writes. The writer returns a template key and the leaders to mention;
 * this fills the template's handle argument, so the board, the timeline and the end screens all
 * show the same line for the same position.
 *
 * Every template gets exactly one argument, whether or not it uses it: a line about tweeting at
 * everyone on Xitter has no single target, so it falls back to [everyoneHandle] instead of
 * leaving the template an argument short.
 */
class TweetTexts(
    private val strings: ContentStrings,
    private val everyoneHandle: String,
    private val writer: TweetWriter = TemplateTweetWriter(),
) {
    /** [tweet]'s text with its mention filled in, or an empty string for an unwritten tweet. */
    fun text(tweet: Tweet): String {
        if (tweet.textKey.isEmpty()) return ""
        return strings.text(tweet.textKey, tweet.mentions.firstOrNull()?.let(::handle) ?: everyoneHandle)
    }

    /** Writes [event] in [leaderId]'s voice and renders it in one step. */
    fun write(
        event: TweetEvent,
        leaderId: LeaderId,
        random: HookRandom,
    ): String = text(writer.write(event, leaderId, random))

    /** [leaderId]'s handle, or the "everyone" handle for a leader outside the roster. */
    fun handle(leaderId: LeaderId): String =
        LeaderRoster
            .byId(leaderId)
            ?.handleKey
            ?.let(strings::text)
            ?.takeIf { it.isNotEmpty() } ?: everyoneHandle
}

/** A [TweetTexts] bound to the composition's resources. */
@Composable
fun rememberTweetTexts(): TweetTexts {
    val strings = rememberContentStrings()
    val everyone = stringResource(R.string.compose_everyone_handle)
    return remember(strings, everyone) { TweetTexts(strings, everyone) }
}
