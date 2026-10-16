# OpenSense progress

Updated: 2026-10-16. State: **public research preview and static APK investigation**.
Physical vehicle compatibility is not established.
Implementation work was stopped at the user's request. The user subsequently
authorized creating the public `Marshmellow31/OpenSense` repository and committing
and pushing the independently authored project and documentation. This publication
does not authorize further vehicle experiments or control commands.

## Completed work

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
