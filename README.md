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

## Run it against ChaosBank-Server

The same tests against [ChaosBank-Server](https://github.com/VadimToptunov/ChaosBank-Server)
instead of the embedded mock (`docker compose up` there first; 10.0.2.2 is the host
machine from the emulator):

```bash
./gradlew :app:connectedStandardDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.cbBackend=http://10.0.2.2:8080
```

Needs a ChaosBank-Android build that has the server backend (`CHAOSBANK_BACKEND`).

## What's covered

62 tests, ported from the iOS suite and checked one defect at a time:

| File | Classes | What they pin down |
| --- | --- | --- |
| `CoreTests.kt` | Auth, Home, Markets, Transfer, Exchange, Portfolio, Card, Transactions, Order, Loans | the main flows, plus the defects they meet on the way |
| `ValueTests.kt` | `*ValueTests` | the money: totals, debits, credits, P&L, APR, order totals |
| `GapTests.kt` | Localization, Concurrency, Security | locale formatting, double submits and races, PIN/PAN masking, OTP, deep links, token storage |
| `KycTests.kt` | Kyc | the mock identity check (scripted document and selfie) and the €1,000 transfer gate it opens |

`tools/mutation_matrix.py` runs each class under each of its defects and lists the
survivors. 44 defects are mapped to the tests that should catch them, and all 44 are caught.

## Notes for Compose

- Kaspresso doesn't wait for Compose nodes the way it does for Views. Data loads
  asynchronously, so `support/Reads.kt` has explicit waits (`waitForTag`, `waitEnabled`,
  `waitForRows`, `eventually`). A read without a wait can see a placeholder (`+$0.00 · +0.00%`)
  and pass for the wrong reason.
- Prices tick every 700 ms. A test that compares two labels reads both from one
  semantics snapshot (`snapshotTexts`).
- Two `performClick()` calls wait for idle in between, so they never race.
  `rapidDoubleTap` sends both taps in one gesture.

## License

[MIT](LICENSE)
