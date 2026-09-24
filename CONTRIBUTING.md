# Contributing

## Tracks

| Track | Owner | Owns | Label |
|---|---|---|---|
| A: Engine and game flow | @Samylay | `engine/`, end-of-match screens | `track:engine` |
| B: Power-ups | @AzerX2 | `cards/`, shop UI | `track:cards` |
| C: In-game UI | @Seleis36 | `app/.../ui/` design system, board, dice, motion | `track:ui` |
| D: Bots, voices, front door | @khvatovat | `bots/`, `content/`, title/pick/versus/timeline/settings screens | `track:bots-voice` |

Your personal guide (tasks in order, hand-offs, first steps) is `docs/team/<your-username>.md`.

## Rules

1. Take an issue from the current milestone, assign yourself, and branch as `<track>/<ID>-short-name` (for example `engine/E3-xitter-rules`).
2. Keep PRs small (under ~400 lines) and merge them to `main` daily. Put unfinished features behind `FeatureFlags`.
3. Don't wait on another track. Code against `FakeGameController`, `GameFixtures` or `TestGame`, which are all created in M0.
4. Changing a contract (anything listed in ROADMAP §3) needs approval from every consuming track.
5. CI must be green (build, unit tests, ktlint), and one reviewer from another track must approve.
6. Before merging, meet the definition of done in ROADMAP §7.
