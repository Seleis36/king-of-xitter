package com.kingofxitter.bots

import com.kingofxitter.engine.model.LeaderId

/**
 * `./gradlew :bots:sim` — plays matches with no UI and prints what happened.
 *
 * Options: `--games=N`, `--seed=N`, `--difficulties=intern,reply_guy,troll_farm`,
 * `--leaders=a,b,c,d,e,f`, `--limit=N` (actions before a match counts as stuck).
 * Pass them through Gradle as `--args="--games=10000"`.
 *
 * The process exits non-zero when any match failed, so the run can gate a release.
 */
fun main(args: Array<String>) {
    val options = parse(args)
    val config =
        SimulationConfig(
            games = options["games"]?.toInt() ?: SimulationConfig().games,
            seed = options["seed"]?.toLong() ?: SimulationConfig().seed,
            difficulties = options["difficulties"]?.let(::difficulties) ?: BotDifficulty.entries,
            leaders = options["leaders"]?.split(",")?.map { LeaderId(it.trim()) } ?: DEFAULT_LEADERS,
            actionLimit = options["limit"]?.toInt() ?: SimulationConfig().actionLimit,
        )
    val started = System.nanoTime()
    val report = Simulator.run(config)
    val seconds = (System.nanoTime() - started) / 1e9
    println(format(config, report, seconds))
    if (report.failures.isNotEmpty()) kotlin.system.exitProcess(1)
}

/** The report, as the lines the CLI prints. Separated from printing so tests can read it. */
internal fun format(
    config: SimulationConfig,
    report: SimulationReport,
    seconds: Double,
): String =
    buildString {
        appendLine("King of Xitter simulator")
        appendLine("  games        ${report.games} of ${config.games} completed, seed ${config.seed}")
        appendLine("  turns        %.1f average".format(report.averageTurns))
        appendLine("  no winner    ${report.noWinner}")
        appendLine("  power-ups    %.1f bought per match".format(report.averagePowerUpsBought))
        appendLine("  time         %.1fs".format(seconds))
        appendLine("  bots")
        config.difficulties.forEach { difficulty ->
            appendLine(
                "    %-11s %5.1f%% of %d seats".format(
                    difficulty.name.lowercase(),
                    report.winRate(difficulty) * 100,
                    report.seatsByDifficulty[difficulty] ?: 0,
                ),
            )
        }
        appendLine("  leaders")
        config.leaders.forEach { leader ->
            appendLine(
                "    %-11s %5.1f%% of %d seats".format(
                    leader.value,
                    report.winRate(leader) * 100,
                    report.seatsByLeader[leader] ?: 0,
                ),
            )
        }
        if (report.failures.isEmpty()) {
            append("  no failures")
        } else {
            appendLine("  FAILURES (${report.failures.size})")
            // Every failure prints its seed, which is all a bug report needs to reproduce it.
            report.failures.take(FAILURES_SHOWN).forEach { appendLine("    seed ${it.seed}: ${it.reason}") }
            if (report.failures.size > FAILURES_SHOWN) append("    …and ${report.failures.size - FAILURES_SHOWN} more")
        }
    }.trimEnd()

private fun parse(args: Array<String>): Map<String, String> =
    args
        .filter { it.startsWith("--") && it.contains('=') }
        .associate { argument ->
            val (name, value) = argument.removePrefix("--").split("=", limit = 2)
            name to value
        }

private fun difficulties(value: String): List<BotDifficulty> =
    value.split(",").map { name ->
        val wanted = name.trim().uppercase().replace('-', '_')
        BotDifficulty.entries.firstOrNull { it.name == wanted } ?: error("Unknown difficulty: $name")
    }

private const val FAILURES_SHOWN = 10
