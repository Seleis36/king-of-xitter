# S6 fixture review (Track C half)

S6 asks for one `GameFixtures` state per design screen, checked against that screen. The
generic fixtures (`yourRoll`, `shop`, `victory`, …) are shared by the engine, card, bot and UI
tests, so they stay generic. Next to them, `GameFixtures.Design` holds exactly what each design
file draws, and the previews and debug hosts use it to compare a screen with its design side by
side.

| Design screen | Fixture | Where to look | Result |
|---|---|---|---|
| `game-board-your-roll.html` | `Design.board` | Board preview "Design", gallery `board-design` | Matches: turn 7, roll 2 of 4 with four dice kept, Trumpet on Xitter, the same followers, characters and mandates for all four leaders |
| `compose-your-attack-tweet.html` | `Design.compose` | Compose preview "Design", gallery `compose-design` | Matches: three tweets at Trumpet, the Main Character |
| `incoming-tweet-stay-or-log-off.html` | `Design.incoming` | Incoming preview "Design", gallery `incoming-design` | Matches: turn 8, Trumpet's tweets plus CAPS LOCK STUCK take you from 7M to 4M |
| `power-up-shop.html` | `Design.shop` | Shop preview "Design" | Matches the line-up. The design prices Bot Farm at 50; the catalogue's 40 is the agreed cost (ROADMAP §1) |
| `victory.html` | `Design.victory` | Victory preview "Design", `EndGamePreviewActivity --es fixture design-victory` | Matches: you on 20, Kim 18, Poutine 7, Trumpet suspended on 11, 19 tweets, 23M unfollows, 4 turns as Main Character |
| `defeat-account-suspended.html` | `Design.defeat` | Defeat preview "Design", `design-defeat` | Matches: suspended on turn 12, Kim wins on 20 |
| `standings.html` | `Design.standings` (also `liveStandings`) | Standings preview "Design", `design-standings` | Matches: turn 8, Kim 16 (3M, 20), Trumpet 13 (2M, 30), you 12 (4M, 60, on Xitter), Poutine 4 (9M, 120) |
| `match-timeline.html` | `Design.timeline` | Timeline preview | The timeline's lines come from the voice log, not the state; the fixture supplies the line-up on turn 9 |

## Fixed in the generic fixtures

- Card ids now come from the catalogue (`c03_alpha_poster`, `c15_quote_tweet`, `c06_home_turf`,
  `c09_press_office`). The old ids (`alpha-poster`, …) predate C1 and made the policy tray and
  the shop crash until they learned to skip unknown cards.
- `liveStandings` said it followed `standings.html` but had Mi-León in Trumpet's seat and none of
  the design's followers or characters. It is now `Design.standings`.
- `composeTweet` was the opponent's turn, which the compose screen (your own attack) can't show.
  It is now `Design.compose`.

## Screens that did not match their design

Found while checking, and fixed in their own PRs: the Victory, Defeat and Standings screens were
Material stand-ins, the shop and the board's policy tray did not follow `power-up-shop.html` and
`game-board-your-roll.html`.
