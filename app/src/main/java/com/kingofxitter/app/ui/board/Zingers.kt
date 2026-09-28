package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.model.LeaderId

/**
 * Source of attack tweets for the compose screen. V2 supplies real zingers per attacker and
 * target; until then [PlaceholderZingers] fills in.
 */
fun interface ZingerSource {
    /**
     * Returns [count] zingers from [attacker] at [target] (null when hitting everyone). [page]
     * changes with every "New takes" tap; the same inputs always give the same lines.
     */
    fun zingers(
        attacker: LeaderId,
        target: LeaderId?,
        page: Int,
        count: Int,
    ): List<String>
}

/**
 * Stand-in lines from `R.array.compose_placeholder_zingers`, with the target's handle filled in.
 * [lines] are format strings taking the handle as `%1$s`.
 */
class PlaceholderZingers(
    private val lines: List<String>,
    private val handleOf: (LeaderId?) -> String,
) : ZingerSource {
    override fun zingers(
        attacker: LeaderId,
        target: LeaderId?,
        page: Int,
        count: Int,
    ): List<String> {
        if (lines.isEmpty()) return emptyList()
        val start = Math.floorMod(page * count, lines.size)
        val handle = handleOf(target)
        return List(minOf(count, lines.size)) { index -> lines[(start + index) % lines.size].format(handle) }
    }
}
