# OpenSense first tester handoff

Use the OpenSense debug APK (`org.opensense.app`, `0.1.0-dev`) for an
experimental test. Do not send either original research APK from `apk-input/`.
Local checks do not establish vehicle compatibility or full BlueSense parity.

## Before starting

Record phone model, Android version, vehicle year/variant, head-unit model and
firmware, selected BlueSense generation, and APK SHA-256. Do not include a VIN,
registration number or Bluetooth address. Preserve existing app records before
uninstalling: a debug-signature mismatch can prevent an in-place update.

## Phone-only check

1. Open Vehicle, Climate, Media, Insights and Settings. Check scrolling and readable
   labels with the phone's larger text setting.
2. Settings → Simulation: enable it, exercise climate/media previews, then disable
   it. Confirm generated readings clear. Backgrounding also stops simulation.
3. Add, edit and delete a synthetic fuel entry and reminder in Your garage. Check
   theme/profile persistence after restarting.
4. Deny Nearby devices permission, then grant it through app settings. Test Bluetooth
   off/on and refresh paired devices. Expect recovery guidance and no invented data.
5. Separately test document access, notification timing and user-started GPS trips
   on the phone. Emulator results do not establish physical GPS or battery behavior.

## First parked vehicle check

1. Use an authorized vehicle while safely parked. Identify the original working
   BlueSense generation; the primary owner's target is New BlueSense → W7.
2. Pair through Android Bluetooth settings and keep ordinary head-unit phone
   pairing connected. Close any other BlueSense app that could hold the connection.
3. Settings → Bluetooth connection → Session diagnostics → Clear diagnostics.
   Return to Bluetooth connection, refresh paired head units and connect deliberately.
   Connecting sends startup/status requests automatically.
4. Record the time, connection message, received/rejected counters, and whether
   readings agree with the head unit. A socket connection alone is insufficient.
5. Only after status reception and explicit owner agreement, try one supported
   media or climate action at a time and record the observed result. Stop if behavior
   is unexpected. Legacy climate controls are unavailable; New temperature writes
   require reported Celsius units. Do not test controls while driving.
6. Background the app: the vehicle session should close and readings should clear.
   Reconnection requires a deliberate user action.

## Logs and feedback

Diagnostics are enabled automatically at **Settings → Bluetooth connection →
Session diagnostics**. They contain the latest 100 allowlisted event codes with
timestamps and stay in memory. They are lost when the app process ends; no file
export, automatic upload or persistent crash report is implemented. Take screenshots
before closing/restarting, and send them privately with the steps and timestamps.

Vehicle logging currently records socket connection, outgoing message and generic
session failure events. Received/rejected totals appear on the connection screen,
but individual incoming frames, handshake completion, specific failure causes and
disconnect reasons are not logged. This is sufficient for basic feedback, but may
not explain a protocol failure. Do not promise complete troubleshooting evidence.

Include expected versus actual behavior, reproducible steps, phone/Android version,
head-unit firmware, selected profile and APK hash in each report. Keep full phone
bugreports and Bluetooth HCI captures private; arrange a separate scoped capture
only if needed. See [vehicle evidence plan](vehicle-test-plan.md).

## Remaining work

- Confirm physical Bluetooth connection/handshake, units, stale-data clearing,
  command effects, permissions and radio coexistence on the exact head unit.
- Confirm physical GPS, battery/background behavior, reminder timing and external
  document/map/dial/share handoffs.
- Corroborate packet mappings and unsupported firmware/variant behavior with
  authorized captures. Full original-app parity remains incomplete.
- Add explicit redacted diagnostic export and more granular session reason codes
  if remote protocol diagnosis requires them; currently screenshots are necessary.
- Establish stable signing/versioning before a broader update/distribution cycle.
