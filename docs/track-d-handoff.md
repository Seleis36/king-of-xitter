# Track D handoff: bots, voices and the front door

Date: 2026-10-04. Owner: khvatovat. Branch: `codex/bots-voices-front-door`.

## What is implemented

| Task | Deliverable | Remaining external boundary |
|---|---|---|
| S5 | `Leader` roster (six leaders, colour, portrait, string keys), `BotBrain`/`BotDifficulty`, `BotMoves` legal-move surface, `CardMarket`, `TweetWriter`/`TweetMoment`/`Tweet`, portraits as VectorDrawables | Consuming-track approval of the two contracts |
| B1 | `InternBot`: uniform choice among legal moves | None |
| B2 | `ReplyGuyBot`: graded keep-dice policy, stay/log-off by followers, card valuation, `ReplyGuyWeights` | Real card semantics once C2–C7 land |
| B3 | `TrollFarmBot`: Monte Carlo playouts over the engine, Reply Guy as the tie-break | Re-tune depth once cards change match length |
| B4 | `./gradlew :bots:sim`: win rate per difficulty and leader, average turns, stuck/crash detection, non-zero exit on failure | Run again with a real deck for C-track balancing |
| V1 | 300 voice lines (6 leaders × 10 moments × 5), `TweetVoices`, `TemplateTweetWriter`, `TweetMoments` event mapping, `VoiceLog` | None |
| V2 | 108 zingers (attacker × target, plus the whole timeline), `EngagementNumbers`, `TrendingTags` | None |
| V3 | Match timeline with the design's three filters and outcome tags | Card names come from C1; the tag falls back when a card is unknown |
| V4 | Title with the trending marquee, Pick your leader with bot difficulty, Versus intro replaying `MatchSetup`'s roll-off | None |
| V5 | How to play, Settings (sound, haptics, animations and speed, bot speed, the §1 character cap) | None |
| V6 | `LeaderPersonalities` and `LeaderBots`: one brain per seat, weighted by its leader | Measure the band again once the deck is live |

Every Track D item in ROADMAP §5 is implemented. Nothing in the track is blocked.

## What is not claimed

- **No device or emulator run.** The work was verified by the JVM build, the unit
  suites, Android lint and the headless simulator. The local emulator could not be
  authorised for `adb`, so there are no screenshots and no on-device smoke test for
  the five new screens. Someone must open the app on a device or emulator before
  these tasks go green under ROADMAP §7.
- **No remote CI run and no cross-track review.** Both are required by ROADMAP §7.
- **No power-up deck in play.** The app still starts matches on `DefaultGameReducer`
  with `CardMarket.Unknown`, so bots never shop. That is deliberate: the base reducer
  rejects `BuyPowerUp`, and a bot that offered one would stall the controller. The
  shop comes alive when C2's `ShopReducer`, a dealt deck and a catalogue-backed
  `CardMarket` are wired into `MatchHost`, which is a three-line change there.

## Checks

`./gradlew build test ktlintCheck` passes locally: debug and release APK assembly,
Android lint and ktlint. 270 unit tests, 0 failures.

| Module | Tests |
|---|---:|
| `:engine` | 45 |
| `:cards` | 43 |
| `:bots` | 65 |
| `:content` | 32 |
| `:app` | 85 |
| Total | 270 |

### Simulated matches (B4)

| Run | Games | Result |
|---|---:|---|
| `--games=10000 --difficulties=intern,reply_guy` | 10,000 | 4.2s, no failures, nobody-wins 0 |
| `--games=2000` (all three difficulties) | 2,000 | 178s, no failures, nobody-wins 0 |

12,000 matches completed with no crash, no rejected action and no stuck match, which
covers ROADMAP M3's "10k simulated games with no crash" for the base rules.

Win rate per seat, all three difficulties, 2,000 matches: Intern 1.3%, Reply Guy
34.7%, Troll Farm 62.7%. The ladder is in the right order with a clear gap at each
step.

### Leader balance (V6)

Four Reply Guys with personalities, 4,000 matches: Trumpet 18.5%, Poutine 26.5%,
Kim 29.6%, Macaron 25.1%, Xi 32.0%, Mi-León 18.6% per seat.

The simulator explains the spread: with no deck in play, the only personality knob
that really bites is how long a bot clings to Xitter, because the Main Character
takes everyone else's tweets. Earlier, wider weights put Mi-León at 9.7% against
Xi's 33.6%, which made the brash leaders a trap, so the band was narrowed. Shop
appetites and character reserves currently do nothing. **Re-measure after C2.**

This also says something for Track B: in a base-rules match, chasing mandates beats
dealing unfollows. Damage should get better as the aggressive power-ups land, and the
numbers above are the baseline to compare against.

## Integration notes for the other tracks

### Track A (@Samylay)

- B1 replaces `firstAvailableAction`. `MatchHost` now drives bots with
  `LeaderBots(difficulty).asBotAction()`. `botRandom(state)` derives a bot's stream
  from the match's random state, so a bot never advances the reducer's stream.
- `MatchOptions` replaced `MatchHost`'s bare leader argument and carries the leader,
  difficulty, bot delay, character cap and an optional seed. `MatchGate` takes it too.
- `MatchHost.intro` publishes `MatchSetup`'s `rollOff` as `MatchIntro` for the Versus
  screen, and is null for a resumed save, so E9's resume still reaches the board.
- `GameViewModel.restore`'s contract is unchanged. The fresh-match check compares the
  restored state with the supplied one, so a save always wins.

### Track B (@AzerX2)

- `CardMarket` is the only thing a bot knows about a shop card: `appraise(cardId)`
  returns its cost and whether it is a Policy. Supply one backed by `CardCatalog`
  (`:app` depends on both modules) and bots will shop; with `CardMarket.Unknown`
  they never will.
- `./gradlew :bots:sim --args="--games=10000"` is yours for balancing. Pass
  `--difficulties=`, `--leaders=`, `--seed=` and `--limit=`. It exits non-zero when a
  match fails and prints the seed that reproduces it.
- `ReplyGuyBot` values a card as cost × 1.0 for a Policy and × 0.8 for an Executive
  Order, because cost tracks strength in the base game. If a card breaks that, the
  valuation hook to change is `ReplyGuyBot.worth`.
- V3's timeline names a bought card from `CardCatalog.byId[...]?.nameKey`, so a card
  without a `nameKey` string shows the generic "Bought a power-up".

### Track C (@Seleis36)

- `leaderVisual` now reads S5's roster for colours, names and portraits, and `XAvatar`
  defaults to the leader's portrait. The `LeaderVisual` fields `firstName`, `voice`
  and `portrait` are new; `LeaderVisualTest` fails if the roster gains a leader the
  resource table doesn't cover.
- The board's `mainCharacterLine` and the incoming screen's `tweet` now come from
  V1 through `VoiceLog`, using the injection points U5 and U7 left for them.
- `ComposeTweetScreen`'s `zingers` default is now V2's book. `rememberPlaceholderZingers`
  is kept for previews and tests.
- U7's placeholder counts were replaced by V2's `EngagementNumbers`;
  `compactCount` moved to `app/.../content/EngagementCounts.kt`.
- V5 provides `LocalMotion` from the player's settings in `MainActivity`, so U8's
  `MotionSettings.speed` is now driven by the animation-speed setting.
- New owned paths: `app/.../content/`, `ui/front/`, `ui/timeline/`. `CODEOWNERS` has
  the first one added.

## Reproducing the checks

```sh
./gradlew build test ktlintCheck
./gradlew :bots:sim --args="--games=10000 --difficulties=intern,reply_guy"
./gradlew :bots:sim --args="--games=4000 --difficulties=reply_guy"
python3 tools/convert_portraits.py --check
```

The portrait drawables are generated from `assets/portraits/*.svg`; `--check` fails if
a drawable no longer matches its source.
