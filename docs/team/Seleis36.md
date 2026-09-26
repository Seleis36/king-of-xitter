# @Seleis36: Track C, In-game UI

You own the look and feel: the design system, the game board, the dice and all the motion. Your shared components are used by every other track's screens, so ship U1–U3 early.

**Owns:** `app/.../ui/theme/`, `ui/components/`, `ui/board/` · label `track:ui` · 40 pts + M0

## Your tasks, in order

| Week | Task | Issue | Pts | Notes |
|---|---|---|---|---|
| 1 | S4 Theme, fonts, `Modifier.sticker()`, nav shell | #4 | 2 | Get the TTFs from Google Fonts. Tokens are in `design/screens/ui-kit.html` |
| 1 | S6 GameFixtures + FakeGameController | #6 | 1 | Shared with @Samylay: check each fixture against its design screen |
| 1 | U1 Shared components | #28 | 4 | **Others wait on this.** Merge it by the end of week 1 |
| 1 | U2 Icon set | #29 | 2 | Extract the inline SVGs from `ui-kit.html` |
| 2 | U3 Stat widgets | #30 | 4 | |
| 2 | U4 Die component | #31 | 4 | |
| 2–3 | U5 Game board screen | #32 | 7 | Reference: `game-board-your-roll.html`. Build on `FakeGameController` |
| 3 | U6 Compose attack tweet screen | #33 | 3 | Placeholder zingers until V2 |
| 3 | U7 Incoming tweet: stay or log off | #34 | 3 | |
| 4 | U10 Wire the real controller + accessibility | #37 | 5 | Needs E7. M2 exit: a full match on a phone |
| 5 | U8 Motion | #35 | 5 | Slam, shake, rise, pop, stamp: see the Motion section of `ui-kit.html` |
| 5 | U9 Audio and haptics | #36 | 3 | |

## Who waits on you

- **@Samylay** (E11 end screens), **@AzerX2** (C8 shop) and **@khvatovat** (V3–V5 screens) all build on U1–U3.
- **Everyone** needs S4 (theme and nav shell) by day 3.

## What you wait on

- **E7 real GameController** (@Samylay, week 2) for U10. Until then everything runs on `FakeGameController` and fixtures.
- **V2 zingers** (@khvatovat, week 3) for U6. Use fixture strings until then.

## Day one

1. Open every screen in `design/screens/` in a browser. They are 412×915 phone frames.
2. Read ROADMAP §3.
3. Start S4 as soon as @Samylay pushes S1.

## Track A handoff (2026-09-26)

E7/E9 provide `GameViewModel`, saved human ownership and failure/retry; E11 surfaces have API 27 evidence. Connect them in S4/U10, replace Material stand-ins with U1–U3, and review S6 fixtures.
See [API usage and verification](../track-a-handoff.md).
