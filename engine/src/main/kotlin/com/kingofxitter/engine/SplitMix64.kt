package com.kingofxitter.engine

import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom

/** Small deterministic generator whose complete state is stored in [GameState]. */
internal class SplitMix64(
    var state: Long,
) : HookRandom {
    override fun nextInt(bound: Int): Int {
        require(bound > 0)
        var bits: Long
        var value: Long
        do {
            state += GOLDEN_GAMMA
            var mixed = state
            mixed = (mixed xor (mixed ushr 30)) * MIX_1
            mixed = (mixed xor (mixed ushr 27)) * MIX_2
            bits = (mixed xor (mixed ushr 31)) ushr 1
            value = bits % bound
        } while (bits - value + (bound - 1) < 0L)
        return value.toInt()
    }

    private companion object {
        const val GOLDEN_GAMMA = -7046029254386353131L
        const val MIX_1 = -4658895280553007687L
        const val MIX_2 = -7723592293110705685L
    }
}
