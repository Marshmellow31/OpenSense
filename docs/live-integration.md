# Experimental vehicle integration

Updated 16 October 2026. **Verified implementation/static evidence; hardware needs-confirmation.**
Neither original APK has been installed or executed. No phone/head-unit radio
session or vehicle control effect has been tested. Synthetic tests are not captures.
This local build is not a complete BlueSense replacement or production release.

## Implemented Bluetooth behavior

Choose generation/variant in Settings, pair the head unit in Android settings,
then use Settings → Bluetooth connection → Refresh paired head units → Connect.
Grant Nearby devices (Location on Android 8–11). Select only your own authorized
head unit, with the vehicle parked. The newer original selected an already
HFP-connected device; keep ordinary head-unit phone pairing connected for testing.

| Behavior | Legacy 0.46 | New 3.5 |
| --- | --- | --- |
| Transport | Secure RFCOMM, SPP UUID | Insecure RFCOMM, same SPP UUID |
| Startup | `11/05/01`, success payload 1 after two seconds; bounded retries | Configuration request ID 46; inbound ID 1 gets reply after one second |
| Integrity | ED/EE framing, payload length, CRC-16/XMODEM | Incoming symbols 50–5F, length, additive checksum; outgoing raw nibbles |
| Initialization | Startup ACK triggers vehicle/climate/song/volume/audio queries and timed alert requests | Poll response/configuration exchange; busy polling blocks user commands |
| Media writes | Playback toggle, next/previous, volume 0–45, six source commands | Play/pause, next/previous, volume 0–30, six source commands |
| Climate writes | Not recovered; unavailable | AC/Econ/auto/recirculation/rear, fan, vent mode and temperature |
| Live fields | DTE, speed, climate, doors/service/brake codes, lamp/wiper codes, media, raw five-wheel TPMS codes | DTE/economy/speed, media/source availability, climate, lamp/wiper/outside, door/service codes, four-wheel tyre decimal values |

All controls require an active protocol session and relevant reported status.
Newer sources require reported availability. Temperature writes require an explicit
Celsius unit report; Fahrenheit writes are not implemented. Commands do not
optimistically change readings. A socket write does not prove acceptance or effects.

Core sources: `VehicleCommands`, `VehicleSession`, `LiveVehicle`.
Android transport: `bluetooth/.../VehicleConnection.kt`.
Live UI: `app/.../ui/LiveScreens.kt`.
The original app byte layouts are documented in the two protocol profiles.

## Command evidence and independent implementation

Legacy generators/consumers: `UltraHexParserForXUV`, `AcknackUtility`,
`RadioActivity.onPlayButton/onForwardButton/onBackwardButton/onSliderMove` and
source-selection methods. Volume maximum 45 is set by `RadioActivity`.

Newer generators/consumers: `utils.n.a()/j()`, `AudioControl` source handlers,
playback methods, volume `onStopTrackingTouch`, `ClimateControl.m..q`, vent
handlers and `ClimateControl.a(SeekBar)` Smali (Java decompilation failed).
Additional field anchors: `AutoWiperLampStatus`, `TirePressure`, `utils.q`.

Important recovered command distinctions:

- New volume is **ID 21**, source selection **ID 18**, playback **ID 17**;
  ID 25 appears in tuner-band handling, not the volume slider.
- New radio mute/unmute use ID 17 with payloads 6/5; music pause/play use 1/0.
- New source codes: iPod 0, USB 1, CD 2, Bluetooth 3, radio 4, AUX 5.
- New next/previous payloads are 3/4. Legacy next/previous payloads are 2/3.
- New HVAC toggles use ID 36 with selector/value; temperature uses ID 38 with
  a 16-bit whole value, decimal-point byte 46 and fractional digit.
- Incoming/outgoing nibble offsets differ. Legacy CRC and newer additive sums
  must not be interchanged.

Tests use hardcoded golden command vectors. Legacy CRC vectors were independently
cross-checked with Python `binascii.crc_hqx`. Stream fixtures are synthetic and
exercise fragmentation, corruption, handshake rejection and expiry; they do not
establish that a particular head unit accepts those messages.

## Lifecycle and bounded behavior

- One selected head unit and one family per explicit connection. No family probing.
- Connection deadline 15 seconds. New reception timeout 20 seconds; initial Legacy
  startup permits its bounded retry window. Individual readings expire after 20
  seconds even while other frames/polls arrive.
- Leaving the foreground, disconnecting, changing profile or starting simulation
  closes the real socket and clears its data. Old jobs cannot close a newer socket.
- Writes are serialized through a bounded queue; delayed work is capped. User
  controls have a 300 ms rate limit. No automatic reconnect/control replay.
- New-format alignment failure closes the session; the parser does not guess a
  frame boundary. The initial socket boundary still needs capture corroboration.
- Device names are visible only in the local paired-device picker. No identifiers,
  raw messages, VIN, location or exception text enter the enum-only diagnostic log.

## Remaining differences and proof gates

This is **not exact runtime equivalence**. The following remain explicit:

- Newer climate/lamp/tyre/unit consumers follow the original's length-based
  dispatch. Their IDs, units, sentinels and firmware variations need captures.
  Vent modes and some status/tyre values are displayed as codes/head-unit units.
- Legacy climate is status-only. New Fahrenheit temperature writes, spare-wheel
  decimal mapping, radio preset/tuning/seek-hold details are not complete.
- The newer original's hidden reflective RFCOMM-channel fallback is not copied.
  Android public APIs are used; a head unit needing that fallback may fail.
- Original NACK-as-startup-success and malformed-frame bugs are not reproduced.
- HFP/head-unit coexistence, modern Android permissions, physical reconnects,
  ignition transitions and every vehicle-side command effect remain untested.
- The later [phone expansion](phone-feature-parity.md) implements current EcoSense
  fields, phone GPS trip history, explicit parking, reminders/private records,
  document links and manual assistance handoffs. Historical head-unit EcoSense
  archive formats and automatic parking/crash workflows remain unverified.
  Cloud and Wear OS are excluded by the user's confirmed scope. No automatic
  emergency or safety-critical command/API is exposed.
- Automatic parked data capture/export is not implemented. Testers should report
  head-unit firmware, original working app family, phone/Android version, received
  message counts, symptoms and redacted screenshots. Keep HCI logs private.

## First hardware test

1. Record phone/API, selected family, head-unit model/firmware and original working
   app family. Close the original app so it cannot compete for the SPP connection.
2. Pair normally, open OpenSense and connect explicitly while parked. Verify the
   socket reaches application startup and incoming message counts increase.
3. Compare DTE, climate status, source and volume with the head-unit display.
   Treat every mismatch as a failed mapping; do not infer tyre safety from this app.
4. If status matches, test one ordinary supported media action and observe its
   real effect. Only then test supported HVAC actions deliberately.
5. Disconnect/background the app and verify readings clear. Record failures before
   retrying. Do not test while driving or send unreviewed commands/replays.

See `vehicle-test-plan.md` for identification/capture privacy details.

## Verification record

Current results and day-end publication are recorded in `PROGRESS.md`. The old
public CI artifact predates this milestone; use the latest successful run for main.
No physical head-unit verification or production/store release is claimed.
