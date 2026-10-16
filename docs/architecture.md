# Architecture

OpenSense uses native Kotlin/Compose with three modules. Live integration is
experimental and APK-derived; physical car and phone radio behavior are unverified.

| Module | Responsibility |
| --- | --- |
| app | Four Material 3 screens, Settings/Connection, permissions, ViewModel and preferences |
| core | Separate simulation/live models, framing, encoders, sessions, expiry and diagnostics |
| bluetooth | Classic discovery/SDP and explicit selected-family RFCOMM session lifetime |

## Separate data paths

SimulationRepository accepts only SimulatedTransport/SIM1. DemoControls is local
presentation state and never writes a socket. Simulation remains an explicit
Settings option. VehicleSnapshot continues to mean simulated data only.

VehicleConnection creates a fresh VehicleSession for one selected family and
paired head unit. VehicleSession uses LegacyFrameDecoder or NewBlueSenseStreamDecoder,
applies supported fields to nullable LiveVehicle, and produces documented startup/status
messages. VehicleCommands encodes allowlisted media/HVAC actions. There is no
arbitrary hex editor, ECU or firmware API.

LiveScreens renders live fields and supported controls. Unknowns remain blank.
A command write never mutates a received reading. Raw codes, units and original
length-based dispatch need hardware confirmation; see [integration](live-integration.md).

## Connection and lifecycle

The app opens directly on Vehicle. Settings contains family/variant, simulation
and appearance. Selection is a preference, not compatibility detection. Connection
requests permissions, identifies paired head units locally, and offers explicit
Connect/Disconnect. Discovery uses generic labels separately.

Legacy uses public secure RFCOMM/SPP; newer uses public insecure RFCOMM/SPP.
The newer original's hidden channel fallback is not used. Sessions have a 15-second
socket deadline, a protocol-specific initial startup allowance, 20-second receive
timeout and individual 20-second reading expiry. New alignment failure closes the
session. Reconnect is manual and never replays user controls.

Writes use a bounded queue and one writer; scheduled work is capped and user
commands have a 300 ms rate limit. Backgrounding, changing profile, starting
simulation or disconnecting cancels work, closes the socket and clears data.
Generation checks prevent old jobs from overwriting/closing a newer session.
Permission revocation ends reception and blocks writes.

## UI and storage

The dark-first Glacier Blue Material theme and illustrative CC0 car cutout remain.
Tabs are Vehicle/Climate/Media/Insights. The user now requests portrait-only phone
operation; the manifest requests portrait. Routes have
independent scrolling. Live and simulated state never share a decoder/model.

Theme/profile and user-created garage records persist. Private AtomicFile JSON
stores fuel, reminders, owner/vehicle records, document links, explicit parked
location and GPS trip history. Document access uses user-granted provider URIs.
TripService is a separately started location foreground service; it can continue
while the Activity's Bluetooth session closes. ReminderReceiver schedules inexact
alarms and private notifications and restores pending schedules after reboot.
Vehicle identities, live telemetry, protocol buffers and diagnostics remain in memory.
Diagnostics contain at most
100 timestamped enum events, never raw packets or exception text. There is no
Internet/account/analytics/cloud path.

## Verification boundary

JVM tests cover fixed command vectors, corruption/fragmentation, startup, missing
fields, expiry, full-to-full fuel math and GPS estimate bounds. Emulator tests cover
UI, private records, notification delivery and injected GPS recording with no radio
peer. None establishes head-unit acceptance or physical-device effects. The existing
public CI artifact predates the local UI/vehicle integration until deliberately
committed, pushed and checked remotely.
