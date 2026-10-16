# Testing and verification

Run these checks from the repository root with the JDK and Android SDK configured as described in the README:

```powershell
.\gradlew.bat :core:test :app:lintDebug :bluetooth:lintDebug :app:assembleDebug
python -m unittest discover -s tools -p 'test_*.py' -v
```

Use `bash ./gradlew` on macOS/Linux. Kotlin test reports appear under `core/build/reports/tests/test/`; Android lint reports under the corresponding module's `build/reports/`. Report the actual command result and revision; the presence of this document or CI configuration does not mean those checks passed.

## Automated coverage

Core tests exercise synthetic round trips, split/concatenated frames, invalid fields, oversized-frame recovery, binary garbage, decoder reset, seeded garbage, rejection of real-protocol input, reconnect state, bounded retries, cancellation, stale partial data, capability boundaries, and bounded diagnostic storage.

Recovered framing tests also cover the legacy CRC-16 independent check value, payload markers, all split boundaries, corrupted checksums/trailers, zero and maximum structural payload lengths, and bounded noise. New BlueSense cases cover nibble encoding, additive-checksum overflow, length/alphabet failures, fragmentation, concatenation and explicit alignment reset. Fixtures are synthetic: none is a captured vehicle response.

Recorded local results for this session are in [PROGRESS](../PROGRESS.md). Build and
instrumentation results apply to the named source state; CI has not run remotely.

Python tests create synthetic ZIP fixtures to verify inventory hashing, explicit lack of publisher authentication, invalid archives, and size limits. These tests do not analyze or authenticate a real original APK.

The Android instrumentation source provides Compose smoke coverage. Execute it only with a running emulator or connected test phone:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Instrumentation must account for the initial vehicle-selection screen and persisted preferences. A compiled instrumentation APK does not prove that tests ran. The CI workflow intentionally performs build, lint, core, and Python checks without claiming physical-device or vehicle coverage.

## Manual device checklist

| Area | Steps | Expected behavior |
| --- | --- | --- |
| First launch | Clear app data; choose each family and variant | Continue requires selection; chosen profile appears; compatibility stays unverified |
| Persistence | Change profile and theme; restart app | Preferences restore without telemetry or discovery results |
| Simulation | Start, stop, restart; background during an active demo | Values remain explicitly simulated; stop/background clears data; no automatic restart |
| Permission denial | Deny permission; return from Android app settings after granting | Helpful recovery message; discovery starts only after user action |
| Permission revocation | Revoke permission during discovery | Discovery ends without exposing identifiers or crashing |
| Adapter state | Start with Bluetooth off; disable while discovering | Clear unavailable/off state; no vehicle connection is implied |
| SDP | Request service inspection for an observed device | Metadata remains an observation; no vehicle data or control is enabled |
| Layout | Small phone, landscape, large text, gesture/three-button navigation | Scrollable content, reachable controls, readable labels, safe system insets |
| Diagnostics | Generate events, inspect, clear, restart | Bounded enum events; no names, addresses, payloads, or exception text |

Record phone model, Android version, selected profile, build revision, permission state, steps, and actual outcome. Remove identifying details from screenshots. Phone/emulator checks do not prove behavior on a vehicle.

## Static APK inventory

The original APKs are optional private research inputs. Keep them under ignored `apk-input/`; never install or execute an untrusted archive as part of this utility. Run:

```powershell
python tools/inspect_apks.py --input apk-input --output private-analysis/apk-inventory.json --android-sdk $env:ANDROID_HOME --java "$env:JAVA_HOME\bin\java.exe"
```

The utility records archive size/hash, candidate DEX strings and UUIDs, and optional Android SDK badging, manifest, and signature-tool results. Archive and per-entry bounds constrain analysis. A signature verifier can establish signature consistency for the supplied file; it cannot, by itself, establish publisher identity or trustworthy provenance. Candidate strings do not establish runtime behavior or a complete protocol.

Outputs belong in ignored `private-analysis/`. Publish independently written findings and small sanitized facts with confidence labels; do not publish original APKs, decompiled source, raw captures, or private logs. No automated test here proves original-app execution or real-car compatibility. Final device and vehicle validation remains pending.

## Repeat static REA inspection

Use Node.js 24.11+ (24.x), JDK 21, and REA's documented headless JADX 0.7.1 engine.
Obtain the engine through [REA's Android guide](https://github.com/morluto/rea/blob/main/docs/android-analysis.md)
and verify SHA-256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`.

```powershell
npm.cmd install --prefix tools-local/rea --ignore-scripts --no-audit --no-fund rea-agents@6.5.0
pwsh -File tools/rea-analysis.ps1 -Apk apk-input/com.visteon-0.46.apk -JadxJar tools-local/jadx-complete.jar -Jdk 'C:\path\to\jdk-21' -Report private-analysis/repeated-legacy-package.json
```

The wrapper validates the pinned engine and keeps raw reports private. It performs
package inspection without executing the APK. For class/method/reference analysis,
use the installed public REA CLI's `--help` and save outputs in the same ignored
folder; resolve method overload indices from its class inventory. No global agent
configuration or MCP registration is required for the local CLI workflow.
