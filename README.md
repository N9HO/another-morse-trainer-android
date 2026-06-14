# Another Morse Trainer — Android

A native Android port of [Another Morse Trainer](https://anothermorsetrainer.app)
(the SwiftUI iOS app at [N9HO/another-morse-trainer](https://github.com/N9HO/another-morse-trainer)).
Learn to copy Morse code (CW) by ear with the Koch method.

Built with **Kotlin + Jetpack Compose**. The training logic (`morsekit`) is a
near 1:1 port of the iOS MorseKit package; the UI is rebuilt in Compose to match
the iOS app's navy/teal look.

## Features

- **Characters** — Koch-method ladder (A–Z, 0–9)
- **Common Words**, **Abbreviations**, **Q-Codes** — phrase drills
- **Pileup Runner** — call CQ and work a simulated CW pileup
- **Code Exam** — FCC/ARRL-style copy test at 5 / 13 / 20 WPM
- **Listen & Learn** — hands-free: hear the code, then the spoken answer; keeps
  playing with the screen locked (foreground service)
- **Voice answers** — speak your answer instead of tapping (microphone)
- **Progress** — daily streak, accuracy, best copy, per-character recognition chart
- **Settings** — speed, Farnsworth, sidetone pitch, haptics, daily reminders
- Dark navy/teal theme, adaptive icon, phone + tablet responsive layout

## Build

Requires JDK 17 (Android Studio's bundled JBR works) and the Android SDK.

```bash
./gradlew assembleDebug     # debug APK → app/build/outputs/apk/debug/
./gradlew bundleRelease      # signed release AAB (needs keystore.properties — see RELEASE.md)
```

`compileSdk`/`targetSdk` 35, `minSdk` 24.

## Release

See [RELEASE.md](RELEASE.md) for signing and Google Play upload steps. The
signing keystore and `keystore.properties` are intentionally **not** committed.
