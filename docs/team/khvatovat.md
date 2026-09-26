# @khvatovat: Track D, Bots, voices and the front door

You make the opponents: the 3 bot difficulties, each leader's tweet voice, and the screens a player sees before and around a match.

**Owns:** `bots/`, `content/`, `app/.../ui/front/`, `ui/timeline/` · label `track:bots-voice` · 40 pts + M0

## Your tasks, in order

| Week | Task | Issue | Pts | Notes |
|---|---|---|---|---|
| 1 | S5 BotBrain + TweetWriter interfaces, Leader roster, portraits | #5 | 2 | Convert `assets/portraits/*.svg` to VectorDrawables |
| 1 | **B1 Intern bot** | #38 | 1 | **Day 4.** @Samylay needs it for E7 |
| 1–2 | V1 Tweet voices for all 6 leaders | #42 | 6 | The style guide is `design/screens/the-cast-and-their-tweet-voices.html` |
| 2 | V5 How to play + Settings | #46 | 3 | |
| 2–3 | B2 Reply Guy bot | #39 | 6 | Build on fixtures. It plays for real once E6 lands |
| 3 | V2 Zingers, engagement numbers, trending tags | #43 | 3 | @Seleis36's compose screen (U6) uses these |
| 3–4 | V4 Title, Pick your leader, Versus intro | #45 | 6 | The roll-off needs E8 |
| 4 | B4 Headless simulator | #41 | 3 | Needs E6. @AzerX2 uses it to balance cards |
| 4 | V3 Match timeline screen | #44 | 3 | |
| 5 | B3 Troll Farm bot | #40 | 6 | Monte Carlo on top of the engine |
| 5 | V6 Leader bot personalities | #47 | 3 | |

## Who waits on you

- **@Samylay** needs B1 (day 4) for E7.
- **@Seleis36** needs V2 zingers (week 3) for the compose screen.
- **@AzerX2** uses B4 (week 4) for balancing.

## What you wait on

- **E6 hook dispatch** (@Samylay, start of week 3) for B3 and B4, and for B2 to play for real.
- **E8 match setup** (@Samylay, week 3) for the Versus roll-off.
- **U1 components** (@Seleis36, end of week 1) for your screens. Meanwhile use plain Material components.

## Day one

1. Read the cast page and `pick-your-leader.html`, `title-screen.html` and `versus-intro.html` in `design/screens/`.
2. Read ROADMAP §3 (`BotBrain` and `TweetWriter` are your contracts).
3. Draft S5 and share it by day 2.

## Track A handoff (2026-09-26)

E7 already runs with a stand-in. E8 returns selected leaders and roll-off history for V4. Supply the six content leader IDs and adapt B1 through the injected bot action provider.
See [API usage and verification](../track-a-handoff.md).
