# King of Xitter

*The shitposting board game.* An Android game that plays by King of Tokyo rules: you against 3 bots, first to 20 mandates wins. Parody. Not endorsed by any head of state, platform or billionaire.

- **[ROADMAP.md](ROADMAP.md)** has the rules mapping, all 66 power-ups, the architecture, milestones and who does what.
- **[design/](design/)** holds the original design bundle (`king-of-xitter-bundle.html`, which opens in a browser) and the unpacked screens (`design/screens/*.html`, one per screen, 412×915).
- **[assets/](assets/)** holds the leader portraits (SVG) and the Latin subsets of the fonts. For Android, use the TTFs from Google Fonts (Luckiest Guy, Anton and Rubik, all under the OFL).

## Build

Install JDK 21 and Android SDK Platform 36 with Build Tools 36.0.0. The app supports Android 8.1 (API 27) and newer, compiles against API 36, and targets API 36.

Run the project checks with:

```sh
./gradlew build test ktlintCheck
```

Build a debug APK with:

```sh
./gradlew :app:assembleDebug
```

## Leaders

| Leader | Handle | Colour | Portrait |
|---|---|---|---|
| Donald J. Trumpet | @realDonaldTrumpet | `#FF8A1F` | `assets/portraits/trumpet.svg` |
| Vladimir Poutine | @VladPoutine | `#9C7BFF` | `assets/portraits/poutine.svg` |
| Supreme Leader Kim Jong-Fun | @SupremeFunLeader | `#FF4F4F` | `assets/portraits/kim.svg` |
| Emmanuel Macaron | @EmmanuelMacaron | `#FF8FC8` | `assets/portraits/macaron.svg` |
| Chairman Xi Jinpong | @XiJinpong | `#22C08A` | `assets/portraits/xi.svg` |
| Javier Mi-León | @JavierMiLeon | `#5FB4F0` | `assets/portraits/mileon.svg` |

## Workflow

See [CONTRIBUTING.md](CONTRIBUTING.md).
