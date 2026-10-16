# What is needed from the XUV500

The project currently has two downloaded APKs and static protocol findings.
The next blocker is evidence from the exact infotainment system, not another UI.

## First: identify the vehicle and head unit

While safely parked, record:

1. Vehicle build year/month, variant (W7 for the primary test car), and manual or
   automatic transmission. Do not send a VIN or registration number.
2. Whether the head unit is factory fitted or replaced; manufacturer/model if shown.
3. A photo or transcription of **Settings → System information / About / Version**
   on the head unit. Menu wording varies by firmware; do not enter engineering
   menus or change software. Capture firmware/software and Bluetooth versions.
4. The Bluetooth name shown in Android settings. Keep its address private.
5. Whether the old working app was **MAHINDRA BLUE SENSE APP** or **NEW BLUE SENSE –
   XUV500**, and whether it ever connected on this exact head unit.
6. Phone model, Android version, and any known combination with HFP/media/Android
   Auto that previously worked. Report TPMS hardware only if known to be fitted.

## Next: consented parked captures

Do not install either archival APK on a personal phone. Future runtime research
requires a dedicated test device, an authorized/tested input and an explicitly
agreed test scope. This session performed static analysis only.

For a later approved recording using a suitable test setup:

1. Use only an owned or explicitly authorized vehicle, stationary and safely parked.
2. On the test Android phone, enable Developer options, then **Enable Bluetooth HCI
   snoop log** if supported. Availability, mode and export paths vary by OEM/Android
   version; record the phone model first so the exact export steps can be selected.
3. Record connection, idle reception and ordinary owner-operated displays separately.
   Write a timestamped action log: pairing/connect, ignition/accessory state, which
   screen was opened, and disconnect. Avoid control experiments at this stage.
4. Keep the original capture/bugreport private. It may contain other Bluetooth
   traffic, device identifiers, location or account information. Share a reviewed
   minimal trace through a private channel with clear source/direction annotations.
5. Turn snoop logging off after the test. Do not share a full phone bugreport publicly.

No guessed initialization, read query, replay or control command should be sent
from OpenSense until its effect has been reviewed. Owner approval is required
before newly reverse-engineered controls. Never access safety-critical ECUs,
attempt authentication bypass, or modify infotainment/vehicle firmware.

## What a first successful protocol validation would show

- The selected family matches actual directed traffic and checksum/framing rules.
- Fragmented and concatenated reads decode without missing or duplicating messages.
- A documented read-only field matches a head-unit display with known units.
- Disconnect/ignition changes clear stale readings; reconnect is bounded and cancellable.
- Unsupported features stay unavailable; simulated data cannot enter a real session.

Phone radio behavior, head-unit coexistence, physical-device transitions and vehicle
measurements remain separate gates from the passing JVM/emulator checks.
