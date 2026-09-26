# Track A review and handoff

Date: 2026-09-26. Owner: Samylay. Branch: `codex/engine-base-rules`.

## What is implemented

| Tasks | Deliverable | Remaining external boundary |
|---|---|---|
| S1/S2 | Five-module Android/JVM project, API 27 minimum, compile/target 36, CI and engine contracts | Consuming-track approval and merge |
| S6 (A half) | Fake controller and game-screen fixtures, including result edge cases | Seleis36's design/fixture review |
| E1–E5 | Deterministic dice, resolution, Xitter, ordered choices, turns, elimination and victory | None for base rules |
| E6 | Engine-owned card protocol, canonical dispatch, intents, modifiers, extra turns/counters | AzerX2's real catalogue, shop and 66 effects |
| E7 | Serial coroutine controller, paced bots, human pauses, lifecycle ViewModel, failure/retry | B1 bot adapter and U10 board wiring |
| E8 | Six-leader input, human plus three distinct seeded opponents, tied-only roll-offs and their history | D's roster and Versus screen |
| E9 | Versioned JSON, deterministic continuation, persisted human ownership, atomic file replacement | App shell's save path and resume/new-game navigation |
| E10 | Tweet, damage and Main Character statistics | Verify real card integrations when B lands |
| E11 | Victory, Defeat and Standings, localized copy, correct winner/ranks, callbacks and previews | S4/U1–U3 theme/components, S5 portraits, V1 text, navigation |
| E12 | Base-rule and hook tests, seeded golden games, full controller games and regression suite | Repeat against B/D integrations and simulator |

No independently implementable A item remains identified by the review.
The release app still opens the placeholder MainActivity. These APIs and screens
are ready for the owning tracks to connect; a full title-to-match app is not claimed.
The branch remains unmerged. Cross-track review and contract approval are required.

## Review and fixes

Two read-only agent reviews checked specification and repository standards, followed
by implementation and targeted re-review. This does not substitute for a teammate's
approval under CONTRIBUTING and ROADMAP §7.

Specification fixes include immediate start/end-turn victory, lethal hook effects
that previously allowed dice healing to revive an account, zero-tweet passive damage,
additive intent aggregation before clamping, follower-limit changes before healing,
and current-turn extra rerolls. Optional abilities have an explicit decline action.

Queue fixes cover purchase choices, choices added by choices, end-turn continuation,
eliminated choice owners, unavailable paid abilities, terminal queue cleanup, and
unsolicited stay/log-off actions. A rejected bot ability uses the available decline;
other rejected bot actions expose a failure instead of silently stopping.

Controller/save checks cover serialized actions, stale bot ticks, human ownership,
parent/owned scope cancellation, save-before-publication, transient retry, corrupt
save preservation, unsupported versions, decision serialization and RNG continuity.
The ViewModel exposes the restored `humanPlayerId`; callers must use that value.

Screen fixes include survival wins below 20, score defeats while still alive,
no-winner outcomes, winner-first final standings, score ties, live Xitter metadata,
missing/empty states, and compact victory standings. Material and name resources
are temporary substitutes for the shared theme/content modules.

One golden game's second player's totals changed from 12 tweets / 17 damage to
11 / 15 because the start-of-turn 20-mandate win now stops before that last attack.
The winner and turn count did not change. The harness now ends SHOP through the
public action instead of editing the phase in test code.

## Checks

On the final source, `./gradlew build test ktlintCheck` passed in the disposable SDK
container. This includes debug/release APK assembly, Android lint, and ktlint.

| Suite | Passed | Failed/skipped |
|---|---:|---:|
| CardHookReducerTest | 11 | 0 |
| GameRulesTest | 13 | 0 |
| ReviewRegressionTest | 13 | 0 |
| ControllerSetupSaveTest | 8 | 0 |
| StandingsTest | 2 | 0 |
| Total | 47 | 0 |

Coverage includes three recorded base-game seeds and three complete four-bot games
through the real controller, plus setup seeds 0–30 with tied roll-offs.
There are no implemented tests in the still-empty cards/bots/content modules.
CI now runs on pushes to `codex/**`, pull requests, main, and manual dispatch.
Remote CI status must be checked for the pushed commit before marking tasks done.

### API 27 visual evidence

A disposable Docker emulator uses `system-images;android-27;google_apis;x86`, KVM,
and software graphics. The debug-only `EndGamePreviewActivity` hosts production
composables. It is excluded from release builds. The layouts were visually inspected
against the content/structure of the Victory, Defeat and Standings design screens;
final shared fonts, avatars and components await their owners.

- [Victory](verification/api27/victory.png), [survival win](verification/api27/survivor.png).
- [Suspended defeat](verification/api27/defeat.png), [score defeat](verification/api27/scoredefeat.png), [no winner](verification/api27/nowinner.png).
- [Live standings](verification/api27/standings.png), [empty standings](verification/api27/empty.png).
- [320dp width, 150% text](verification/api27/small-top.png) and [scrolled buttons](verification/api27/small-bottom.png).

Text wraps, content scrolls, and buttons remain reachable. The standings back
callback and main-menu callback finish the fixture host. The share callback opens
the Android chooser; no share was sent. Fixture replay finishes the host, exercising
the callback only; creation/navigation of a new game belongs to the app shell.

Reproduce with the debug APK installed:

```sh
adb shell am start -W -n com.kingofxitter/com.kingofxitter.app.EndGamePreviewActivity --es fixture victory
adb shell wm size 720x1280
adb shell wm density 360
adb shell settings put system font_scale 1.5
```

Other fixture values: `survivor`, `defeat`, `nowinner`, `scoredefeat`, `standings`,
`finalstandings`, `ties`, `empty`. Reset with `wm size reset`, `wm density reset`,
and `settings put system font_scale 1.0`.

## Integration instructions

### UI and front door (C/D)

1. Call `MatchSetup.create(humanLeader, sixLeaderIds, seed)`. Animate `rollOff` in
   Versus; keep `state` and `humanPlayerId`. Apply the optional `characterCap = 280`
   setting to the initial state before starting; its default is uncapped.
2. Restore before bot execution via suspend `GameViewModel.restore`, with an
   app-private `MatchSaveFile` path. Put the returned instance in a match-scoped
   ViewModelStore using the shell's factory. Do not call restore on recomposition.
   A saved session wins over the supplied initial state. Use the returned
   `humanPlayerId`, not the newly selected leader, when rendering/submitting actions.
3. Collect state for rendering and events for transient animations. Events are
   non-replayed, so state is authoritative after recreation. Submit actions through
   the controller; never mutate phases to advance gameplay.
4. Observe `failure`, explain it in the UI, and call `retry()` after a transient
   persistence/bot failure. Malformed or unsupported saves throw during restore
   and remain on disk; the shell owns the recovery/new-game choice.
5. A new game requires explicitly replacing/removing the old session through the
   front-door flow. Pass replay/menu/share callbacks to the result surfaces and
   provide V1's actual final tweet when available. The screens do not erase saves.

### Cards and bots (B/D)

Inject `DefaultGameReducer(CardEffectRegistry(effects))` and a bot action provider.
The stand-in rolls once, resolves, chooses the first option and ends SHOP. The
controller falls back to decline when an optional ability is no longer affordable.
This is not the Intern strategy and does not buy cards.

C2 owns buy/refill/sweep handling. Its reducer adapter must delegate base actions
to the engine, update payment/deck/policies immutably, call `dispatchOnBuy` after
payment and before discarding an Executive Order, then preserve the resulting
choices/events. End shopping with the existing `Action.EndTurn`.

Card hooks return intents against their provided snapshot, without I/O or mutation.
Canonical ordering and summation make owned-policy list order irrelevant. Queued
paid abilities are checked against the current character balance. Card-specific
conditions and actual 66-card combinations still need B's effect tests and B4.

### Review and merge

Review the new `DeclineAbility`, serializable models, `characterCap`, controller,
setup and save interfaces with their consuming tracks. The cumulative branch also
contains earlier S1/S2 work and exceeds the small-PR guideline. Preserve the existing
foundation branches (`codex/s1-android-skeleton`, `codex/s2-engine-api`) for staged
review; do not force-rewrite published history to make the diff look smaller.
The dependency graph stays yellow until cross-track review and CI requirements
are satisfied. No teammate approval or merge has been performed by this session.
