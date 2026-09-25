# Project dependency graph

This graph combines the task dependencies and cross-team hand-offs from
[`ROADMAP.md`](../ROADMAP.md) and [`docs/team/`](team/). Green means the task's
implementation is verified, committed, and pushed, including applicable
requirements in ROADMAP §7. Yellow means implementation or verification is in
progress. Green does not imply that a PR has been reviewed or merged.

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
  S3 -. hook contract alignment .-> E6
  E4 --> E7
  B1 --> E7
  E7 --> E8
  E7 --> E9
  E5 --> E10
  U1 --> E11
  U3 --> E11
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
  class S1,S2 done
  class M0D done
  class S6,E1,E2,E3,E4,E5,E10 active
```

## Current hand-offs

- E6 needs S3's `CardEffect` hook contract. It unlocks C5–C7, B3, and B4.
- Align AzerX2's S3 hook list with the already-committed S2 engine API before
  E6 is implemented.
- E7 needs B1. Until then, use the always-first-option bot described in the
  Samylay guide.
- U1–U3 support end-game, shop, and front-door screens. Teams can use Material
  stand-ins until those components land.
- S6 is shared: Samylay supplies fixture states and Seleis36 checks them against
  the design screens.
- M0 design decisions are settled in ROADMAP §1. AzerX2 owns updating the
  Bot Farm catalogue/design to the agreed 40-character cost.
