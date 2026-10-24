# OpenSense progress

## 24 October 2026 — first GitHub prerelease verification

**Verified locally:** development APK build, app/Bluetooth lint (zero errors;
12 app warnings, two Bluetooth warnings), 53 freshly rerun JVM tests and three
Python tests. Android 16/API 36 emulator results: 20 normal-phone tests
(smoke, live UI, garage, diagnostics and reminder delivery), 13 compact-phone
tests (smoke, garage and diagnostics at 840×1680, density 420, 1.3× text), and one
background GPS recording test with injected non-zero locations. All final runs
passed. Normal/compact fixture screenshots were sampled for visual fit; this is
not a new exhaustive UI or accessibility review. The recovered emulator's crash
buffer was empty after these runs.

Added two instrumentation tests that prove automatic diagnostic events appear,
can be cleared, and record a failed connection without showing the supplied device
identifier. App runtime code is unchanged from the previous milestone. Corrected
README storage wording and added `docs/tester-handoff.md` for first-owner testing.

The first emulator attempt was aborted after Android displayed a system-process
ANR during severe host memory pressure. A cold boot with lower memory allocation
and direct instrumentation recovered the test environment. The first GPS fixture
timed out with one point; pre-seeding its nearby starting position before route
injection produced a passing repeat. Neither failed setup attempt is counted as a
passing test or proof of physical GPS behavior. Private logs remain under
`private-analysis/tester-readiness-*` and are not published.

The APK is `org.opensense.app`, version `0.1.0-dev` / code 1, minimum API 26,
target API 37, debug signed. APK signature verification passed. SHA-256:
`8e048046227ab73736a621b3a4deab1e06bf21d7a8deed93d46bc5044d793a8a`.
Stable signing/version upgrades remain future distribution work.

**Needs-confirmation:** actual head-unit identity/firmware, Bluetooth session and
handshake, incoming mappings/units, physical media/HVAC effects, phone permissions,
GPS/battery behavior, notification timing and external provider handoffs. Full
original-app parity remains incomplete. Diagnostics are enabled but only retain
100 in-memory enum/timestamp events; there is no diagnostic export or persistent
crash report. The tester must capture Connection and Session diagnostics screens
before restarting. Granular receive/timeout/disconnect reason logging remains a
follow-up if protocol diagnosis requires it.

The previous application commit's remote CI run `38921437206` is now verified
successful. This verification/handoff change is intended for the first
`v0.1.0-dev` GitHub prerelease, explicitly marked experimental. Release publication
and the CI result for this documentation/test commit must be checked on GitHub.

---

Updated: 2026-10-16. State: **portrait phone milestone and experimental live integration**.
Physical vehicle compatibility is not established. After the UI pause and Bluetooth
comparison, the user requested functional Bluetooth parity and said to continue.
That authorizes this implementation; no actual vehicle experiment was performed.
The user authorized finishing documentation and pushing this milestone to main.
Source commit **24a8ec87440221298a0501b943ce732a0c681236** was pushed and
verified against remote main. The earlier public CI artifact is historical.
[Milestone CI run 38921437206](https://github.com/Marshmellow31/OpenSense/actions/runs/38921437206)
was **in progress at the day-end handoff**, not yet a verified remote pass.

## Current vehicle integration

### Portrait phone feature expansion — day-end milestone

The user subsequently requested the remaining phone features with Impeccable,
portrait-only operation, private records/Android sharing and no cloud or Wear OS.
[Phone feature scope](docs/phone-feature-parity.md) records functionality and bounds.

- Implemented fuel diary/weighted full-to-full efficiency, reminder CRUD/lead times/
  notifications/rescheduling, personal/vehicle fields, document/manual links,
  explicit parked location/maps, foreground phone-GPS trip history, warning
  preferences, assistance dial/share handoffs and non-identity export/deletion.
- Added APK-derived current EcoSense fields and New BlueSense radio presets/band/
  stored-frequency encoders. No original proprietary material was copied publicly.
- Local JVM suite now **53 tests passed**. Build and app/Bluetooth lint pass.
- Normal portrait: **17 tests passed** (five smoke, six live UI, six garage).
  Compact portrait at 840×1680, 1.3× text: **six garage tests passed**.
- Separate notification test passed for one synthetic due reminder and cancellation.
  GPS runtime tests passed with host-injected non-zero emulator fixes, including
  backgrounding the Activity: persisted a trip and stopped cleanly. An earlier zero-coordinate fixture run timed out;
  real GPS, Doze and physical vehicle behavior are not established.
- Impeccable reviewer opened all 38 native fixture screenshots: disposition **fix**.
  Three material findings (compact Time button wrapping, internal route IDs in
  app bars and unitless slider semantics) were applied in one batch. Corrected
  six-test runs passed on both phone sizes, including slider adjustment/unit
  semantics and date/time picker actions. The reviewer scored all three fixes
  **resolved**, disposition **ship at that bounded scope**; no new whole-surface
  review or physical TalkBack claim is made. Native screenshots/reports stay ignored.
- Build/app lint/Bluetooth lint: pass, zero errors, 12 app warnings and two Bluetooth
  warnings (including the user-requested fixed orientation and dependency/API hints).
- OpenSenseUI emulator was stopped at the user's explicit day-end request.
- This milestone is saved to main with code, public findings and progress docs.
  No original APKs, decompiled sources, private captures, test screenshots, local
  tools, APK binaries, keys or user data are included. No PR/merge or store release.

### Resume next time

1. Use the latest successful GitHub Actions OpenSense-debug artifact or build from
   main. The old successful artifact linked later in this file is historical.
2. For the owner's first parked test choose **New BlueSense → W7** in Settings,
   identify the exact head-unit model/firmware, pair normally, and verify incoming
   status against the head-unit display before deliberately trying controls.
3. Confirm radio/climate effects, units/sentinels, physical GPS/battery behavior,
   document-provider access and reminder timing on an actual phone.
4. Original automatic emergency/parking events, optional lounge-light firmware
   selectors and historical EcoSense archive formats still need evidence. No cloud
   or Wear OS work is planned under the confirmed scope.

Final local evidence logs (ignored): garage-feature-build.log, garage-runtime-build.log,
garage-review-fixes-build.log, garage-phone-tests.log, garage-compact-tests.log,
garage-phone-capture-final.log, garage-review-fixes-phone.log,
garage-review-fixes-compact.log, garage-gps-diagnosis.log and garage-gps-background.log.

The prior integration summary below describes the immediately preceding milestone.

- Both families now have explicit paired-device selection, proper public RFCOMM
  socket factory/SPP UUID, startup/session messages and independent checked framing.
- Legacy startup ACK triggers status queries and timed alert requests. New polling
  receives a delayed reply/configuration request; busy status blocks commands.
- Media actions, sources and volume use APK-derived encoders. Legacy volume maximum
  is 45; newer is 30. New HVAC switches/fan/vent/Celsius temperature are implemented;
  legacy climate remains status-only.
- Separate nullable live data is displayed only in vehicle sessions; simulation
  remains isolated. Stale sections expire even while unrelated messages arrive.
- Disconnect/background/profile/simulation transitions close the socket and clear
  data. Queues/delayed work are bounded; no automatic control replay/reconnection.
- Command evidence, differences, remaining original features and parked test steps:
  [docs/live-integration.md](docs/live-integration.md).
- Local JVM suite: **43 passing tests** (28 existing plus 15 session/command tests).
  Current local build and app/Bluetooth lint pass. The final APK passes **11 emulator
  tests** at normal 1080×2400 and compact 840×1680 with 1.3× text, density 420.
  Landscape verification is recorded below when complete.
- The user confirmed **New BlueSense – XUV500** as the first hardware-test family.
  The primary vehicle remains the 2018 W7; head-unit model/firmware are unconfirmed.
- No original APK was run. No physical phone/head unit/car was tested. Full original
  runtime equivalence and production readiness are not established.
- Branch main, HEAD 4d67043771c70f7829ee1d0bca8e1e40ce586657. Changes remain local,
  uncommitted/unpushed; no PR, merge or release was created for this work.

Older sections below are historical baseline/handoff records, superseded above
where they describe discovery-only behavior or the earlier stop request.

## Completed work

### UI overhaul — 2026-10-16

The user authorized implementing the agreed Impeccable/Material 3 redesign:
single photographic XUV500 cutout on a platform, default dark theme, four main
destinations, and simulation plus vehicle selection moved into Settings.

- Vehicle opens directly; no first-run profile gate or dashboard demo-start button.
- Vehicle, Climate, Media and Insights use Glacier Blue Material 3 dark/light roles.
- Settings contains model/generation selection, local simulation and appearance.
- Local demo controls change target temperature, fan/air direction/AC flags and
  media source/track/playback/volume. They never send commands or play audio.
- Connection, permission recovery and diagnostics remain accessible separately.
- The CC0-source photo was edited into a transparent cutout; the user-requested
  aftermarket roof-carrier removal is applied. See docs/assets/README.md for source
  credit, original trim and both exact image-edit prompts.
- A short-window icon rail fixes landscape navigation overflow. Each destination
  owns its scroll state, avoiding Settings/profile scroll leaking into the dashboard.
- Current fresh-emulator interaction suite: **five tests passed** on OpenSenseUI,
  Android 16/API 36. Build and app lint pass. The final visual confirmation was
  stopped at the user's request; the implementation is saved locally.
- Existing protocol/transport code remains unchanged; real-vehicle compatibility
  and controls remain unverified and unavailable.

#### Paused UI handoff

User requested stopping this work on 2026-10-16. No UI-overhaul commit or push was
made. Public `main` remains the previously published baseline; local files contain
the redesign and updated documentation.

- Latest local APK: `app/build/outputs/apk/debug/app-debug.apk`, installed on the
  dedicated `OpenSenseUI` Android 16 emulator (`emulator-5556` during this session).
- The original Medium Phone AVD hit its low-storage threshold. A clean, dedicated
  OpenSenseUI AVD was created; unrelated apps/data in the old AVD were preserved.
- Tests/automation were stopped and emulator size/font overrides restored. The
  dedicated emulator remains available; no recurring follow-up was scheduled.
- Independent Impeccable review accepted phone visual direction and requested:
  compact landscape task composition, scrollable media-source sheet, and intact
  large-text airflow labels. All three code fixes are implemented, but final
  reviewer scoring of the corrections is pending.
- Latest code uses a compact landscape context header, complete car/platform or
  primary controls in the first viewport, a scrollable fully expanded source
  sheet, and wrapping 48 dp airflow chips. Demo/source-selection capture assertions
  were updated for the compact header and all four media choices.
- Latest build/lint log: `private-analysis/overhaul-sheet-ready.log` (passed).
  Latest five-test interaction log: `private-analysis/overhaul-publish-smoke.log`
  (passed). The `overhaul-publish-{phone,compact,landscape}.log` files record the
  final capture attempt; do not assume every capture completed after the stop.
- Earlier validated full captures are in `private-analysis/overhaul-final-captures/`;
  these PRECEDE the three reviewer corrections. `overhaul-publish-captures/`, if
  present, contains later captures and must be checked against completion logs.
- Resume by confirming the latest three visual fixes in the emulator, obtaining
  the bounded reviewer verdict, then refreshing DESIGN.md and README screenshots.
  No additional UI refinement or vehicle commands were authorized by the stop.

### Protocol foundation baseline

- Inspected empty workspace, installed toolchain and missing APK inputs.
- Researched distinct BlueSense packages and historical version/feature leads.
- Downloaded both requested APKs: `com.visteon` 0.46 and
  `com.contus.mahindra.xuv500` 3.5. Recorded SHA-256, signatures, manifests,
  permissions, archive provenance and code/resource inventories.
- Used REA 6.5.0 and a SHA-256-verified headless JADX engine for static package,
  class, method and reference inspection; exported Java/resources and all-class
  Smali privately. Originals were never installed or executed.
- Distinguished two Classic SPP protocols: legacy ED/EE binary framing with CRC-16,
  newer nibble encoding with additive checksum. Wrote separate factual specs.
- Implemented independent offline decoders and malformed/fragmented-stream tests.
- Created native Android app with Kotlin/Compose/Material 3, vehicle-family and
  variant selection, labelled simulated dashboard/media/climate, capability screens,
  Bluetooth discovery/SDP, settings and bounded redacted diagnostics.
- Added MIT license, contribution/security guidance, architecture/testing/research/
  compatibility documentation, CI and issue forms. Private research is ignored.

## Findings and evidence

See [research](docs/research.md) for exact input hashes, signer fingerprints and
provenance. [Protocol](docs/protocol.md) links both family specifications and REA
Evidence IDs. APK signatures verify with the v1 scheme; independent publisher
identity confirmation is still missing. Catalogue dates and app titles are not
hardware proof. Newer Java export has eight errors/forty warnings; legacy has
one error/zero warnings. All-class Smali exports exist, but decompiler errors do
not become automatically resolved semantics.

## Current implementation

Real Bluetooth work is limited to discovery and standard SDP. No vehicle data
socket, handshake, write API, firmware modification, cloud backend or telemetry
dashboard integration exists. Both recovered decoders are offline libraries;
`SIM1` is a distinct invented simulation format. Profile selection is persisted
alongside theme choice and establishes no compatibility.

## Verification

- Final expanded JVM suite: **28 tests passed, zero failures**, including separate
  legacy and new protocol parsers, malformed input, simulation and lifecycle tests.
- Android debug APK built. Final app lint: **zero errors, six warnings**;
  Bluetooth lint: **zero errors, two warnings**. Warnings include newer dependency
  versions and Kotlin convenience APIs; they were not suppressed.
- **Three Compose instrumentation tests passed** on an Android 16/API 36 emulator,
  covering onboarding, explicit simulation, unavailable vehicle features and
  switching to the legacy profile. Final rerun after scroll-reset fix passed.
- Python static-inspector tests passed (three synthetic cases).
- Portrait onboarding visually inspected at 1080×2400. Status-bar contrast and
  unnecessary empty onboarding navigation bar fixed. Profile/page transitions now
  reset scroll to the top; an instrumentation assertion covers this.
- Small-screen check at 840×1680 / density 420 (320×640 dp), font scale 1.3:
  onboarding scroll and profile selection work, and text wraps. Bottom navigation
  labels become cramped/clipped (notably Features); this is a known polish issue
  to fix on resume. Landscape check was not completed before the stop request.
- No physical phone, Bluetooth head unit, packet capture or vehicle test performed.
- CI configuration builds/lints, runs JVM/Python tests, and uploads only the
  independently authored OpenSense development APK after successful checks.
  Initial remote run reached SDK setup, then failed because the setup action
  requested the obsolete `tools` package. Its package selection was corrected to
  `platform-tools`. The subsequent [GitHub run 38847498840](https://github.com/Marshmellow31/OpenSense/actions/runs/38847498840)
  **passed** on commit `6c2527407199afa5ecdded9157a23d48f866112f`:
  Linux build, app/Bluetooth lint, JVM tests and Python tests all succeeded.
  `OpenSense-debug` artifact ID `12023593844` was uploaded successfully; it contains
  the CI-built debug APK and is retained until 2026-10-30. Emulator UI results above
  remain local evidence; the remote pipeline does not run that suite.

## Unverified assumptions and blockers

- W7 head-unit identity/firmware/transmission not supplied; support cannot be
  derived solely from year/trim or common SPP UUID.
- Wire checksum/envelope findings are static; capture corroboration and exact
  units, field validity, startup effects and reconnect timing remain unknown.
- No verified per-variant capability handshake. TPMS fitment is not assumed.
- Whole-package export is complete as an inventory task; whole-app semantics,
  third-party dependency versions, some obfuscated methods and runtime equivalence
  remain incomplete.
- Installed Android Studio 2025.3.3 needs upgrading for AGP 9.3 IDE sync; CLI build
  uses its bundled JDK 21 successfully. Default system Java is older.

## Required hardware evidence and next steps

Follow [vehicle evidence checklist](docs/vehicle-test-plan.md): record head-unit
About/Version information and original working app family, then arrange an
explicitly scoped parked capture. Correlate read-only fields before wiring the
offline decoder to a vehicle repository. Request owner approval before transmitting
newly reverse-engineered vehicle controls. Add model/firmware support only with
reproducible evidence.

## Repository / release state

Public repository: [Marshmellow31/OpenSense](https://github.com/Marshmellow31/OpenSense).
Initial project commit: `95054cc40334ec2cbd4b16893af823596b0f5b43`, pushed to
`main` and verified against the remote branch. Repository visibility and default
branch were confirmed as PUBLIC and `main`. The user explicitly authorized creation,
commits and push on 2026-10-16. No PR/merge, release or Play Store publication was
performed. Original APKs, decompiled proprietary outputs, private analysis/captures,
local tools, generated local builds and secrets remain excluded from Git. CI can
publish only its own OpenSense debug build as a time-limited workflow artifact.

README now explains the research method, artifact-analysis limits, protocol
differences, APK operation/data flows, build/download instructions, local test
results and a progress/next-milestone checklist. Local build/lint and Python checks
were rerun before publication; all passed. JVM reports contain 28 passing tests.
Remote CI and the development-APK upload are now verified as described above.
The final publication-status update changes documentation only; it does not change
the application or CI configuration tested by commit `6c25274`.

## Resume handoff

- Development APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Public findings: `docs/research.md`, `docs/protocol.md`, both family specs,
  compatibility matrix and `docs/vehicle-test-plan.md`.
- Private originals: `apk-input/com.visteon-0.46.apk` and
  `apk-input/com.contus.mahindra.xuv500-3.5.apk`, plus provenance sidecars.
- Private analysis: `private-analysis/apk-inventory.json`, REA Evidence JSON,
  `legacy-findings.md`, `legacy-decompiled/`, `legacy-smali/`, `new-full-export/`,
  `new-smali/`, screenshots and build/test logs. These remain Git-ignored.
- Local tools: REA 6.5.0 under `tools-local/rea/`; verified JADX engine
  `tools-local/jadx-complete.jar`. Partial download segments also remain ignored.
- Final successful logs: `private-analysis/final-checks.log`,
  `private-analysis/ui-final-checks.log`, `private-analysis/ui-final-tests.log`.
- First remaining local task: improve bottom navigation at 320 dp / larger fonts,
  then complete landscape checks. Main protocol blocker remains exact head-unit
  identity and consented parked captures. No real vehicle connection or control
  command should be enabled merely because static framing tests pass.
- No original APK was executed, no global REA agent configuration was changed,
  and no background follow-up or automation was created.
