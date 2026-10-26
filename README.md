# ChaosBank ⨯ Kaspresso

Hand-written **[Kaspresso](https://github.com/KasperskyLab/Kaspresso)** UI tests that
drive **[ChaosBank-Android](https://github.com/VadimToptunov/ChaosBank-Android)**, the
deliberately-buggy Android neobank/broker built as a practice range for mobile
QA/SDET automation. It's the Android twin of the iOS reference suite,
[ChaosBank ⨯ KassiOS](https://github.com/VadimToptunov/ChaosBank-KassiOS-Tests).

ChaosBank's production code is correct, and its defects are switched on at launch.
So the contract every test here is held to is:

- green on the **clean** build;
- red when its own defect is switched on, with no test edits.

## How it fits together

```
vendor/ChaosBank-Android/   → the app under test (git submodule), built as-is
suite/src/                  → screen objects, tests and helpers
settings.gradle.kts         → includes the app's :app module and version catalog
build.gradle.kts            → grafts suite/src onto :app's androidTest source set
```

The tests compile into the app's own `androidTest` APK, so they run in-process with
`MainActivity` and match Compose nodes by the app's `testTag`s (the same identifier
strings as the iOS accessibility ids). Nothing in the app is changed.

## Run it

Needs JDK 21, the Android SDK and a running emulator or device (API 29+).

```bash
git submodule update --init --recursive
./gradlew :app:connectedStandardDebugAndroidTest
```

One class:

```bash
./gradlew :app:connectedStandardDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.vadimtoptunov.chaosbank_android.kaspresso.tests.HomeTests
```

## Run it against a defect build

Pass the defects (or a profile) as instrumentation arguments. `CbTestCase` forwards them
to the app as `CHAOSBANK_DEFECTS` / `CHAOSBANK_PROFILE`, so the same tests run against a
broken build without edits:

```bash
./gradlew :app:connectedStandardDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.cbDefects=pnlSign,cardCvvVisible
```

## License

[MIT](LICENSE)
