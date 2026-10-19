# Project dependency graph

This graph combines the task dependencies and cross-team hand-offs from
[`ROADMAP.md`](../ROADMAP.md) and [`docs/team/`](team/). Green means the task's
implementation is verified, committed, and pushed, including applicable
requirements in ROADMAP §7. Yellow means implementation or verification is in
progress or awaiting external review/integration. A task needs green CI and
a reviewer from another track before it becomes green; agent review alone does
not satisfy that requirement. Decision nodes record agreed decisions.

```mermaid
flowchart LR
  subgraph M0["Shared foundation"]
    S1["S1 Project and CI"]
    S2["S2 Engine API"]
    S3["S3 Card hooks and TestGame"]
    S4["S4 Theme and app shell"]
    S5["S5 Bot and voice contracts"]
    S6["S6 Fixtures and fake controller"]
    M0D["M0 Design decisions"]
    HDEC["Card-hook contract decided"]
  end

  subgraph A["A · Samylay · Engine"]
    E1["E1 Dice"]
    E2["E2 Dice resolution"]
    E3["E3 Xitter rules"]
    E4["E4 Turn state machine"]
    E5["E5 Elimination and win"]
    E6["E6 Card-hook dispatch"]
    E7["E7 Real controller"]
    E8["E8 Match setup"]
    E9["E9 Save and resume"]
    E10["E10 Match stats"]
    E11["E11 End screens"]
    E12["E12 Rules and golden-game tests"]
  end

  subgraph B["B · AzerX2 · Power-ups"]
    C1["C1 Card catalogue"]
    C2["C2 Deck and shop"]
    C3["C3 Executive Orders"]
    C4["C4 Passive policies"]
    C5["C5 Activated policies"]
    C6["C6 Counters and extra turns"]
    C7["C7 Complex cards"]
    C8["C8 Shop UI"]
    C9["C9 Ability prompts"]
  end

  subgraph C["C · Seleis36 · In-game UI"]
    U1["U1 Shared components"]
    U2["U2 Icons"]
    U3["U3 Stat widgets"]
    U4["U4 Dice component"]
    U5["U5 Game board"]
    U6["U6 Compose tweet"]
    U7["U7 Incoming tweet"]
    U8["U8 Motion"]
    U9["U9 Audio and haptics"]
    U10["U10 Connect real controller"]
  end

  subgraph D["D · khvatovat · Bots and voice"]
    B1["B1 Intern bot"]
    B2["B2 Reply Guy"]
    B3["B3 Troll Farm"]
    B4["B4 Simulator"]
    V1["V1 Leader voices"]
    V2["V2 Zingers"]
    V3["V3 Match timeline"]
    V4["V4 Front-door screens"]
    V5["V5 How-to and settings"]
    V6["V6 Bot personalities"]
  end

  S1 --> S2
  S2 --> S6
  S3 --> S6
  S4 --> S6
  S5 --> S6

  S2 --> E1
  E1 --> E2 --> E3 --> E5
  E1 --> E4
  E4 --> E6
  HDEC --> S3
  HDEC --> E6
  S3 -. card integration .-> E6
  E4 --> E7
  B1 -. replace stand-in .-> E7
  C2 -. shop integration .-> E7
  E7 --> E8
  E7 --> E9
  E5 --> E10
  U1 -. shared components .-> E11
  U3 -. shared widgets .-> E11
  E1 --> E12
  E2 --> E12
  E3 --> E12
  E4 --> E12
  E5 --> E12
  E6 --> E12

  S3 --> C1
  S3 --> C2 --> C3
  S3 --> C4
  E6 --> C5
  E6 --> C6
  E6 --> C7
  E6 -. live hook integration .-> C4
  U3 --> C8
  C5 --> C9
  U1 --> C9

  S4 --> U1
  S4 --> U2
  U1 --> U3
  U2 --> U3
  U2 --> U4
  U3 --> U5
  U4 --> U5
  S6 -. fake controller .-> U5
  U5 --> U6
  U5 --> U7
  U5 --> U8 --> U9
  E7 --> U10

  S5 --> B1
  S5 --> V1
  B2 --> B3
  E6 -. live engine integration .-> B2
  E6 --> B3
  E6 --> B4
  V1 --> V2
  V1 --> V3
  U1 --> V3
  U1 --> V4
  E8 --> V4
  U1 --> V5
  B2 --> V6
  M0D --> C1
  M0D --> V5

  classDef done fill:#b7efc5,stroke:#218838,color:#102a17,stroke-width:2px
  classDef active fill:#fff3cd,stroke:#bd8500,color:#3d2e00,stroke-width:2px
  class M0D,HDEC done
  class S1,S2,S3,S6,E1,E2,E3,E4,E5,E6,E7,E8,E9,E10,E11,E12 active
  class C1,C2,C3,C4,C5,C6,C8 active
  class S5,B1,B2,B3,B4,V1,V2,V3,V4,V5,V6 active
  class S4,U1,U2,U3,U4,U5,U6,U7,U8,U9,U10 done
```

## Current hand-offs

Implementation and verification details: [Track A review and handoff](track-a-handoff.md).

- S1/S2, Samylay's S6 fixtures, and E1–E12 have implementations on
  `codex/engine-base-rules`. All independently executable Track A work is
  locally verified. Yellow records the remaining cross-track approvals and
  integrations, not missing controller/setup/save implementations.
- E7 uses `RealGameController` and the always-first-option stand-in. Public
  `Action.EndTurn` exits `SHOP`, so C2 never needs to block base matches.
  E8 supplies seeded leader selection and roll-off history. E9 stores state,
  RNG, queued choices, and human ownership atomically.
- E6 is implemented and merged on `main` in PR #48, closing issue #12. S3's
  registry/effects remain open for AzerX2 to integrate; B3/B4 can use the real
  hook APIs. Cross-track review of the engine contract additions remains
  outstanding, so E6 stays yellow under ROADMAP §7. Actual card effects, shop
  purchases, and bot strategies remain owned by B/D.
- E11 uses Material stand-ins and was checked on an API 27 emulator, including
  small-screen/large-text layouts. S4/U1–U3 supply the final theme/components;
  S5/V1 supply portraits and generated match text; S4/U10 wire navigation.
- S6 is shared: Samylay supplies states; Seleis36 checks them against the design.
- Samylay (Track A) reviewed and approved Seleis36's S4 and U1–U10 work on
  2026-09-28. PRs #49–#59 are merged, the combined local build/test/lint check
  passed (100 tests, 0 failures), and post-merge CI passed on `main` at
  `bf5914174d09591d24a3c1069d6e935766a03c5f`.
- S1/S2 were previously green without evidence of the required cross-track
  approval. They are yellow until ROADMAP §7 is satisfied. Keep all task nodes
  yellow until implementation, tests, device checks where relevant, remote CI,
  review, commit and push are verified. S4 and U1–U10 are green after the
  cross-track review and validation recorded above.
- M0 decisions remain settled in ROADMAP §1. AzerX2 owns updating the Bot Farm
  catalogue/design to the agreed 40-character cost.
- All of Track D (S5, B1-B4, V1-V6) is implemented and pushed on
  `codex/bots-voices-front-door`. `:content` holds the leader roster, the
  TweetWriter seam, 300 voice lines, 108 zingers, fake engagement and the trending
  tags; `:bots` holds the three difficulties, the shared legal-move surface, the
  per-leader personalities and the `:bots:sim` simulator; `:app` gains the title,
  pick-your-leader, versus, how-to-play, settings and timeline screens. The local
  `build test ktlintCheck` passes with 270 tests and 0 failures, and 12,000
  simulated matches completed with no crash. The nodes stay yellow because ROADMAP
  §7 is not satisfied yet: there is no device/emulator run of the five new screens,
  no remote CI result, and no cross-track review. Details and integration notes are
  in [the Track D handoff](track-d-handoff.md).
- S3 (CardDef + TestGame), C1 (CardCatalog, all 66 cards), and C2 (Deck and shop logic) 
  are implemented and pushed on their respective branches. `CardDef` holds name, cost,
  type, rules-key and flavour-key for each card. `TestGame` is a fluent
  `GameState` builder for unit tests and Compose previews. `CardCatalog` exposes
  `all`, `byId`, and `standardDeck`. `ShopReducer` intercepts BuyPowerUp and SweepShop, 
  applies costs, and delegates hooks to the base reducer.
  18 unit tests cover structure for C1/S3, and tests exist for C2 logic.
  S3, C1, and C2 remain yellow until CI passes and a cross-track reviewer approves per ROADMAP §7.
- C3 (Executive Orders) is implemented with 16 card effects and matching tests. It remains yellow until CI passes and a cross-track reviewer approves per ROADMAP §7.
- C6 (Counters and extra turns) is implemented with 6 card effects and tests. The engine was updated to natively handle removing negative counters with likes and processing intrinsic effects for shadowban and cancelled counters. It remains yellow until CI passes and a cross-track reviewer approves per ROADMAP §7.
- C4 (Passive policies) and C5 (Activated policies) are implemented with their card effects and tests. They remain yellow until CI passes and a cross-track reviewer approves per ROADMAP §7.
- C4 (Passive policies) is implemented with 26 card effects. The implementation required adding missing hooks to E6 for healing, damage reactions, and logging off. It remains yellow until CI passes and a cross-track reviewer approves.
- C7 (Complex cards) is implemented with 8 cards including Hostile Takeover and Retweet for Retweet. It remains yellow until CI passes and a cross-track reviewer approves.
- C9 (Ability prompts) is implemented. It added support for selecting dice, faces, and targets directly from the GameBoard UI. It remains yellow until CI passes and a cross-track reviewer approves.
- 2026-10-19 emulator QA pass and fixes (#70–#82, self-merged at @Seleis36's request with green CI
  but no cross-track review, so no node changed colour):
  - Engine: a decision that suspends the current player ends their turn instead of freezing the
    match (#70). Taking Xitter when the Main Character logs off or is suspended scores its mandate (#81).
  - Cards: dice-changing abilities are only offered on their owner's roll, and Paid Promotion,
    Touch Grass and Smoke and Mirrors only when they can help (#72). The shop charges and shows the
    Lobbyist price (#73). `TestGame.withPlayer` seats a third leader (#82).
  - Bots: the Troll Farm searches with the match's reducer and buys power-ups, and the full-deck
    simulation deals every match (#71).
  - UI: C9's ability prompts on the dice (#74, which includes #69). CONTINUE on the title, and Back
    from the board goes home (#75). New takes and the posted zinger (#76). Victory, Defeat and
    Standings in the design system, and SEE RESULTS once you're suspended (#77). Portrait on phones,
    and a centred column on large screens (#78). The shop and policy tray from the design (#80).
  - S6: `GameFixtures.Design` holds one state per design screen, checked in
    [the S6 fixture review](s6-fixture-review.md) (#79).
  - Issues #3, #6, #9 and #27 are closed. They still need a human cross-track review before their nodes turn green.
