# @AzerX2: Track B, Power-ups

You own all 66 power-ups (cards), the deck, the shop and the shop screen. Most of your logic is pure functions, so you can build and test almost everything before the engine is finished.

**Owns:** `cards/`, `app/.../ui/shop/` · label `track:cards` · 40 pts + M0

## Your tasks, in order

| Week | Task | Issue | Pts | Notes |
|---|---|---|---|---|
| 1 | S3 CardDef + CardEffect hooks + TestGame | #3 | 2 | Agree on the hook list with @Samylay by day 2. `TestGame` is your main tool until E6 lands |
| 1 | C1 Catalogue of all 66 cards | #19 | 3 | Names, costs and effects are in ROADMAP §2. Change the parody names if you have better ones |
| 1 | C2 Deck and shop logic | #20 | 4 | |
| 2 | C3 Executive Orders | #21 | 4 | Needed for M1 |
| 2–3 | C4 Passive policies (26 cards) | #22 | 8 | Write them as hook functions and unit-test each with `TestGame` |
| 3 | C8 Card component + shop screen + policy tray | #26 | 4 | Reference: `design/screens/power-up-shop.html`. Use @Seleis36's components |
| 3–4 | C5 Die-manipulation and activated policies | #23 | 6 | Needs E6 |
| 4 | C6 Counters and extra turns | #24 | 4 | Needs E6 |
| 4 | C9 Card-ability prompts | #27 | 1 | |
| 5 | C7 Complex cards (Copycat, Burner Account, …) | #25 | 6 | |
| 5 | Balance pass using @khvatovat's simulator (B4) | | | |

## Who waits on you

- **@Samylay** needs S3 to finish the engine API (S2), and to know where to call hooks (E6).
- **@khvatovat's** Reply Guy bot (B2) values cards, so it reads your `CardCatalog` (C1).

## What you wait on

- **E6 hook dispatch** (@Samylay, start of week 3) for C5–C7. Until then, do C1–C4 against `TestGame`. If E6 slips, bring C8 forward.
- **U1–U3 components** (@Seleis36, end of week 1/2) for C8. Meanwhile use plain Material components.

## Day one

1. Read ROADMAP §2 and §3.
2. Open `design/screens/power-up-shop.html` and `ui-kit.html`.
3. Draft the `CardEffect` interface and review it with @Samylay.
