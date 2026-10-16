# New BlueSense XUV500 profile

Status: **verified static artifact evidence; needs-confirmation on hardware**.
Target APK: `com.contus.mahindra.xuv500` version 3.5/code 28, SHA-256
`ebaedc28da5ccc1d5f09d9f95f4c95762d97082cde9ea50f47c6b8e077f2ad06`.
REA 6.5.0 with pinned JADX headless 0.7.1 inspected the APK without running it.

## Bluetooth connection

The transport constant in `com.contus.mahindra.xuv500.utils.b` is SPP UUID
`00001101-0000-1000-8000-00805F9B34FB`. Its inner connection thread `b.a` calls
`createInsecureRfcommSocketToServiceRecord`. An IOException path tries the hidden
`createRfcommSocket(1)` method reflectively. OpenSense does not reproduce this hidden
fallback. Its experimental transport uses the same public insecure RFCOMM factory;
see [current implementation and limits](live-integration.md).

`utils.a.a(Context)` obtains Android's HEADSET profile and inspects connected
devices, accepting the names `XUV500` or the `device_name_mahindra` resource value
`MAHINDRA`. These names are selection heuristics, not authenticated identities.
The source expects Android pairing/HFP connectivity; pairing success and current
Android behavior are untested. No BLE GATT path appears in this connection flow.

## Incoming envelope

`utils.n.a(byte[])`, overload 2 / DEX descriptor `([B)V`, subtracts decimal 80
from every received byte. Helpers join the resulting four-bit values in high,
then low order to form logical bytes. Thus incoming wire symbols are 0x50–0x5F.

| Logical byte offset | Meaning | Evidence |
| --- | --- | --- |
| 0 | Message identifier | Feature dispatch inspects first eight logical bits |
| 1 | Additive checksum | `utils.o.a(String[])`, overload 13 |
| 2 | Payload length N | Receive splitter reads nibble offsets 4 and 5 |
| 3 onward | N payload bytes | Receive size is `6 + 2*N` wire bytes |

Checksum: sum logical bytes at offsets 0 and 2 onward, retaining the low eight
bits; compare with byte 1. The original app calls this a CRC in some logging,
but the inspected routine is an additive checksum, not polynomial CRC-16.
The structural upper bound is 258 logical / 516 incoming wire bytes.

`utils.o.a(String)` constructs **outgoing raw nibbles** from bit strings, and the
socket write path passes them directly to `OutputStream.write`. The incoming
0x50 offset must not be assumed in the outgoing direction. `VehicleCommands` now
implements independent outgoing encoders for reviewed startup/media/HVAC commands.

There is no confirmed sync delimiter in this format. OpenSense's
`NewBlueSenseStreamDecoder` requires a known initial frame boundary; it supports
fragments and concatenation from that boundary. A bad symbol/checksum loses
alignment and parsing stops until `resetAtKnownBoundary()` is called. Arbitrary
noise is not scanned for plausible packets. A length corruption can leave a
partial packet; capture/session boundaries must discard it. These are defensive
design choices, not observed head-unit recovery behavior.

## Session and feature traces

The newer app reacts to an inbound message ID 1 of four logical bytes, schedules
a response after one second, and prepares a configuration request. Its timeout
watch checks an eleven-second timer and marks data stale when the recorded receive
time is over twenty seconds old. Transport states include disconnected 0,
connecting 2, connected 3 and lost 4. Error paths close sockets and notify UI.
The fallback and portions of reconnection code have decompiler control-flow
limitations; none were exercised against hardware.

Static dispatch in `utils.n.e(byte[])` includes:

| Logical message ID / length | Static consumer |
| --- | --- |
| 1 / 4 bytes | Poll/session state |
| 5 / 48 bytes | Vehicle configuration / identity fields |
| 6 / 10 bytes | Speed-related value |
| 7 / 4 bytes | Accessory / ignition state |
| 10 / 10 bytes | Fuel-economy fields and distance to empty |
| 19 / 15 bytes | Available media sources |
| 20 / 9 bytes | Playback / volume status |
| 21 / 4 bytes | Radio preset position |
| 23 / 64 bytes | Song information |
| Other length-based paths | TPMS, climate, lamps, EcoSense and emergency data |

Some handlers dispatch by length alone. These are static observations, not a
validated complete command catalogue. Units, masks, firmware-specific IDs,
unavailable values and variant-specific signals need captures. Identity fields
and location are sensitive; the replacement does not log or display those bytes.

## Evidence anchors and coverage

- Package/manifest: `ev_ed97e968dc824a8449cf5040afff44907e05a992eeff2628baa8b442a3f7b64e`.
- Receive transformation and frame splitting, `utils.n.a([B)V`:
  `ev_b17566a73e66469b6dcaaefcd583f5a669440a2c11044ebaebe0058f2d7c6b73`.
- Additive checksum, `utils.o.a([Ljava/lang/String;)Z`:
  `ev_467ef1d4abf4a30ae1d6d36100a7370d94f294ce16f9d79c935be601402c6acc`.
- Connection class inventory, `utils.b.a`:
  `ev_4c8c704ee4a6daad42aad6300738c66dfc45b421d09b6654a078ba572574274d`.
- Connection constructor, `utils.b.a.<init>`:
  `ev_fdd1c90bbc520ed5b8a00a4e7b2a4a097f9b5de7d7ae7dedddcd073547324373`.
- Incoming references to receive decoder:
  `ev_74aef822d4290a5918525ea0352f8cd2701c6c9a8fd3922070faf2293ebe21ad`.

The package overview reports 2,095 top-level classes. Full export inventories
4,170 rows including inner classes, of which 527 are under the app namespace.
Java/resources export reports eight errors and forty warnings, with corresponding
Smali exported for inspection. All proprietary outputs stay in ignored private
folders. This is a whole-package static inventory/export and focused protocol
investigation, not complete runtime equivalence or recovery of original source.
