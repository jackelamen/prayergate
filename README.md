# PrayerGate

An Android app that pauses you with one English question before you open a
distracting app. No server, no AI, no login. Everything stays on the phone.

![The question screen](docs/question-screen.png)
![Onboarding, stats and settings](docs/app-screens.png)

## Build and install

1. Open this folder in Android Studio (or use the command line below).
2. Plug in your phone with USB debugging on.
3. Run:

```
./gradlew installDebug     # build and install on the phone
./gradlew test             # run the unit tests (no phone needed)
```

Needs JDK 17 or newer and Android SDK platform 35. Works on Android 8.0+.

## First-run setup

One thing per screen: welcome, "Display over other apps", "Usage access",
notifications, battery, choose apps, done. On the two system-settings steps
PrayerGate checks twice a second and brings itself back as soon as the switch
is on. Steps already allowed are skipped, and setup cannot finish until at
least one app is chosen.

Settings also has a **User guide** (Help) and **Reset PrayerGate** (Data), which
deletes all records and settings and returns to the first setup screen.

## How it works

```
 Engine A (UsagePollingService)          Engine B (ForegroundAccessibilityService)
 asks UsageStatsManager every 300 ms     gets a "window changed" event from Android
                \                                   /
                 v                                 v
                    GateController.onForegroundApp()
                    - did the foreground app change?
                    - is it one of the chosen apps?
                    - is its cooldown over?
                                 |
                                 v
                          QuestionActivity
                 question -> feedback -> Close / Continue
                                 |
                                 v
                    Room database (trigger_logs table)
```

Only the engine selected in Settings acts. Switch between them there to compare.

**Time check** (Settings, off by default): after you tap Continue, PrayerGate
counts how long you stay in that app (5, 15 or 30 minutes). When the limit
is reached you get another question, headed "15 min on Instagram".
Leaving the app or locking the phone pauses the clock. Coming back within
one minute carries on where it left off; a break of a minute or more starts
it from zero. The one minute is fixed in `SessionTimer.kt`
(`BREAK_RESETS_AFTER_MS`) and is deliberately not a setting, so a quick
switch away cannot dodge the check. The clock is driven from
`GateController.kt`, so it works with both engines. Time checks are
logged with reason `TIME_CHECK` and left out of the debug screen's delay
figures.

## Where things are

| Folder / file | What is in it |
| --- | --- |
| `app/src/main/assets/questions.json` | The 150 bundled questions |
| `tools/make_questions.py` | Edit questions here, then run it to rebuild the JSON |
| `app/src/main/res/values/strings.xml` | Every piece of on-screen text |
| `.../data/` | Room database, settings, question picking, stats maths |
| `.../detection/` | Both engines, the shared rules, permissions, boot receiver |
| `.../ui/` | All screens. `QuestionActivity.kt` is the intervention |
| `.../ui/theme/Theme.kt` | Colours, fonts and shapes, with the design rules at the top |
| `.../ui/Components.kt` | Shared pieces: buttons, cards, labels and the full stop |
| `.../ui/Ground.kt` | The deep blue background with its giant full stop and large pale italic word |

## Learning features

- **300 questions**: 50 vocabulary and 50 grammar at each level. Edit or add
  them in `tools/make_questions.py`, then run it to rebuild the JSON.
- **Review tab**: every question whose last answer was wrong, with the right
  answer and why. "Practise" opens the question screen in practice mode
  (nothing is logged as a trigger); a correct answer there removes the
  question from the list.
- **Difficulty suggestions**: with at least 20 answers at the current level
  in the last 7 days, the Stats screen suggests moving up at 85% correct or
  more, or down at 50% or less. Acting on it or tapping "Not now" hides it
  for a week. The rule is `adviseDifficulty` in `data/Stats.kt`.

- **Korean explanations**: every question has a Korean explanation as well
  (`tools/explanations_ko.py`). Settings > Explanations switches it on; it
  is on by default when the phone's language is Korean.
- **Gentle escalation**: from the 3rd time the same app is opened within an
  hour, "Continue" waits 5 seconds after you answer (10 seconds from the 5th
  time). "Close" always works straight away. The rule is
  `continueDelaySeconds` in `data/Stats.kt`.

- **Per-app stats**: the Stats screen lists each chosen app for the last 7
  days with how often you chose Close ("closed 9 of 23").
- **Home-screen widget**: long-press the home screen, choose Widgets, then
  PrayerGate. It shows your streak and today's question (the same question
  all day, from your level). Tapping it opens that question in practice
  mode. It refreshes every hour and after every answer.

## Testing the two engines on your phone

1. Finish onboarding and switch on one or two apps.
2. Open those apps a few dozen times with Engine A selected.
3. In Settings choose Engine B, grant it, and repeat.
4. In Settings, tap the version number at the bottom 7 times. The debug screen
   shows the trigger count, median delay and 90th-percentile delay per engine.

"Delay" is the time from the app coming to the foreground (as reported by
Android) to the first drawn frame of the question.

### Samsung Galaxy notes

- Engine B's switch is greyed out ("Restricted setting") when the app was
  installed from a downloaded APK. Try to switch it on once, then go to
  Settings > Apps > PrayerGate > ⋮ > Allow restricted settings, and try again.
  Installing with `adb install` or from the Play Store avoids this. The app
  shows these steps itself once you have tried once.

- Besides turning off battery optimization, add PrayerGate to
  Settings > Battery > Background usage limits > Never sleeping apps.
- Engine A needs "Appear on top" (display over other apps). Without it Android
  blocks a background app from opening a screen.

## Languages (English and Korean)

The app follows the phone's language: Korean phones get the Korean version,
everything else gets English. On Android 13 and newer you can also pick a
language for PrayerGate alone in Settings > Apps > PrayerGate > Language.

- English text: `app/src/main/res/values/strings.xml`
- Korean text: edit `tools/strings_ko_source.xml`, then run
  `python3 tools/make_strings_ko.py`. It writes
  `app/src/main/res/values-ko/strings.xml` (do not edit that file by hand),
  checks that every English string has a Korean version, and glues the
  letters of each Korean word together so lines only wrap between words.
- Korean explanations for the questions: `tools/explanations_ko.py`.
- The questions themselves stay in English, since English is what is
  being practised.

## Screen sizes

Layouts are checked from small phones (320dp wide) and the Galaxy Z Fold
cover screen to the unfolded Fold, in landscape, and with large system font
sizes. On wide screens the content stops at a comfortable reading width and
sits in the middle; on a phone turned sideways the question screen puts the
question on the left and the answers on the right. Labels that must stay on
one line (options, tabs) shrink slightly instead of breaking a word.

## Before publishing on Google Play

- The accessibility service needs a declaration in the Play Console, and the
  in-app disclosure (`DisclosureScreen.kt`) must be shown in your review video.
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and the `specialUse` foreground
  service type are both reviewed by Google; be ready to explain them.

## Legal

Settings > Help > About & legal shows, in English and Korean:

- **Privacy policy.** PrayerGate collects nothing; this says so and lists what
  stays on the phone. The final app has no internet permission (check the
  merged manifest) and Android backup is off. A hostable copy for the Play
  Console privacy-policy link is in `docs/privacy-policy.md`.
- **Open-source licences.** Component list plus full texts in
  `app/src/main/assets/licenses/` (SIL OFL 1.1 for the fonts, Apache 2.0 for
  AndroidX, Compose and Kotlin). Add any new library here.
- **Trademarks.** App names such as Instagram belong to their owners.

Before publishing: put a contact email in `legal_contact`
(`app/src/main/res/values/strings.xml`) and in `docs/privacy-policy.md`.
The Contact section of the in-app policy appears once it is filled in.

## Fonts

Newsreader and Hanken Grotesk, both under the SIL Open Font License
(see `licenses/`).

## Building more apps like this

`.claude/skills/gate-app-builder/` is a Claude skill for making sibling apps
(prayer, motivation...) from this one. Fill in
`.claude/skills/gate-app-builder/assets/brief-template.md` (examples in
`assets/examples/`) and ask Claude to build the app from the brief. The skill
forks this project, renames it (`scripts/rename_app.py`), swaps the action,
content, brand and words, and verifies the result.

