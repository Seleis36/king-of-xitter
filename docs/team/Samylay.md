# @Samylay: Track A, Engine and game flow

You build the rules engine: the heart of the game. Everyone else codes against your API, so **your M0 contracts and E6 are the two most important deliverables in the project.**

**Owns:** `engine/`, `app/.../ui/endgame/` · label `track:engine` · 40 pts + M0

## Your tasks, in order

| Week | Task | Issue | Pts | Notes |
|---|---|---|---|---|
| 1 | S1 Gradle skeleton + CI | #1 | 2 | Do this **first, day 1**. Everyone's M0 PRs need a building repo. JDK: use Android Studio's bundled JBR with Gradle 9+ |
| 1 | S2 Engine API | #2 | 2 | Freeze by day 3. KDoc every type |
| 1 | S6 GameFixtures + FakeGameController | #6 | 1 | Shared with @Seleis36: you write the states, they check them against the design screens |
| 1 | E1 Dice | #7 | 3 | |
| 1 | E4 Turn state machine | #10 | 4 | |
| 2 | E2 Dice resolution | #8 | 3 | |
| 2 | E3 Xitter rules | #9 | 5 | |
| 2 | E5 Elimination and win | #11 | 2 | M1 exit: a base-rules match plays end to end in a JVM test with 4 Intern bots |
| 2 | E7 Real GameController | #13 | 4 | Needs B1 (Intern bot) from @khvatovat on day 4 |
| 3 | **E6 Card hook dispatch** | #12 | 5 | Hook contract is decided. Start against ROADMAP §3 now and land dispatch by the start of week 3 |
| 3 | E8 Match setup + roll-off | #14 | 2 | |
| 3 | E12 Golden-game tests | #18 | 3 | |
| 4 | E10 Match stats | #16 | 1 | |
| 4 | E11 Victory, Defeat, Standings screens | #17 | 5 | Use @Seleis36's components (U1–U3) |
| 5 | E9 Save/resume | #15 | 3 | |
| 5 | Fix bugs found by the simulator | | | |

## Who waits on you

- **Everyone** needs S1, S2 and S6 by day 3.
- **@AzerX2** needs E6 for C5, C6 and C7 (week 3). Until then they test cards with `TestGame`.
- **@khvatovat** needs E6 for B3 (Troll Farm) and B4 (simulator), and E8 for the Versus intro roll-off.
- **@Seleis36** needs E7 to wire the real game into the board (U10, week 4).

## What you wait on

- **B1 Intern bot** (@khvatovat, day 4). Meanwhile use a bot that always picks the first option.
- **S3 card definitions and effects** (@AzerX2) follow the contract in ROADMAP §3. The hook contract is decided, so E6 can proceed now; integrate S3 as it lands.
- **U1–U3 components** (@Seleis36) for E11. Meanwhile use plain Material components.

## M0 decisions

Char cap, Bot Farm cost, and leader abilities are decided in ROADMAP §1.

## Day one

1. Read ROADMAP §1 and §3.
2. Open `design/screens/how-to-play.html` and `game-board-your-roll.html` in a browser.
3. Push S1 so the others can open their M0 PRs.
