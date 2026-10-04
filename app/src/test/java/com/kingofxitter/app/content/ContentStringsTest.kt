package com.kingofxitter.app.content

import com.kingofxitter.app.R
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.content.TrendingTags
import com.kingofxitter.content.TweetVoices
import com.kingofxitter.content.ZingerBook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves that every key `:content` declares exists as an app string. [ContentStrings] resolves
 * keys at runtime, so without this the first missing key would only show up as a blank tweet on
 * a device. The generated `R.string` class carries one field per string, which is enough to check
 * the keys without a device or a Context.
 */
class ContentStringsTest {
    private val declared: Set<String> =
        R.string::class.java.fields
            .map { it.name }
            .toSet()

    @Test
    fun `every tweet voice line exists`() {
        val missing = TweetVoices.all.filterNot { it in declared }
        assertEquals(emptyList(), missing, "missing ${missing.size} voice lines")
        assertEquals(6 * 10 * 5, TweetVoices.all.size)
    }

    @Test
    fun `every zinger exists`() {
        val missing = ZingerBook.all.filterNot { it in declared }
        assertEquals(emptyList(), missing, "missing ${missing.size} zingers")
        // Six attackers, five targets plus the whole timeline, three lines each.
        assertEquals(6 * 6 * 3, ZingerBook.all.size)
    }

    @Test
    fun `every trending tag exists`() {
        assertEquals(emptyList(), TrendingTags.all.filterNot { it in declared })
    }

    @Test
    fun `every roster string key exists`() {
        LeaderRoster.all.forEach { leader ->
            listOf(leader.firstNameKey, leader.shortNameKey, leader.fullNameKey, leader.handleKey, leader.voiceKey).forEach { key ->
                assertTrue(key in declared, "missing $key")
            }
        }
    }
}
