# Legacy XUV500 protocol profile

Status: **verified static artifact evidence; needs-confirmation on hardware**.
This profile describes `com.visteon` version `0.46`, version code `1`, from APK
SHA-256 `ebdac117ac7e532079057bcda4a2de9bf4d9f0d1b6bfafbdd34fe3edc70d5231`.
The archival APK's signature verifies; its publisher identity has not been independently
authenticated. Neither original APK was installed or executed for this investigation.

The independently authored `LegacyFrameDecoder` accepts byte streams without owning
Android transport. `VehicleSession` and `VehicleConnection` now use it for experimental
live status/media integration; see [current implementation](live-integration.md).
The decoder itself does not transmit or establish trusted readings.
A car's year or trim cannot be selected reliably from
these APK bytes alone. Users must choose the legacy infotainment profile explicitly
until a hardware compatibility matrix exists.

## Transport observations

The legacy app searches bonded Bluetooth devices for the case-insensitive name
`MAHINDRA_BLUESENSE`, then creates an RFCOMM socket with the standard SPP UUID
`00001101-0000-1000-8000-00805F9B34FB`. This is Bluetooth Classic evidence.
The inspected transport path does not use BLE GATT services or characteristics.
Pairing is expected through Android; no separate cryptographic application login
was found in the inspected connection/startup path. This does not establish every
possible vehicle-side authentication requirement.

Source anchors: `BluetoothConnetionConstraintsHelper.getPairedDevices`,
`BluetoothXUVService.<clinit>` and `BluetoothXUVService$ConnectThread.<init>`.
REA evidence: `ev_280033a9b596e46cbb1f26bbf33eee3c8542decc1408f46fd150c4ad7c777f97`,
`ev_5f50ac2f66e9a3e0c911c7436e7a236dcc4c69cd548b3a523281d840dae1c9f6`.

## Packet layout

All offsets are zero-based. The payload length is treated as an unsigned byte in
OpenSense's defensive offline parser; original parser arithmetic uses Java signed
bytes in several places, so payloads above 127 bytes require hardware confirmation.

| Offset | Meaning | Static observations |
| --- | --- | --- |
| 0 | Start marker | `ED` |
| 1 | Protocol version | Generators use `01` |
| 2 | Application ID | Generators use `01` |
| 3 | Message number | Generators use `00` or `01` |
| 4 | Message type | ACK `01`, NACK `02`, app notification `11`, request `20`, inbound notification/response `21` |
| 5 | Feature ID | Alerts `01`, vehicle `02`, climate `03`, audio `04`, general `05` |
| 6 | Function ID | Feature-specific; raw value preserved |
| 7 | Payload length N | One byte |
| 8 through 7+N | Payload | N bytes |
| 8+N | CRC high byte | Most significant byte first |
| 9+N | CRC low byte | Least significant byte |
| 10+N | End marker | `EE` |

The total frame size is `11 + N`, giving a structural upper bound of 266 bytes.
Markers inside the payload are data. The offline decoder checks the length, end
marker and CRC, supports fragmented/concatenated packets, discards garbage, and
resynchronizes after invalid frames. It retains fewer than 266 pending bytes after
each feed. Output size follows the number of packets/errors in the supplied input.
Header version/application IDs are exposed raw, including unknown values.

CRC parameters are polynomial `0x1021`, initial value `0x0000`, no reflection, no
final XOR. The checksum covers offsets `0` through `7+N` inclusive. The independent
check input ASCII `123456789` produces `0x31C3`. This check vector is a local test,
not a vehicle capture.

Source anchors: `UltraHexParserForXUV.generatePacketForStartUpNotification(Z)[B`
(DEX method index 465, code-item offset 65952),
`AcknackUtility.getChecksum([B)[B` (index 1562, offset 166612), and
`AppConstants` nested feature/message definitions. Request type `20` is confirmed
in the smali instructions for `generatePacketForClimate`, method index 462,
code-item offset 65548. Some Java output has invalid variable declaration order;
the smali confirms the checksum is assigned after packet initialization.
REA evidence: `ev_b1c4181687353b13fbe4367567d1a14b569bac6e9bbc80bf256847d6ec4f1603`,
`ev_9c677f1ff86b770eb5b5046b6be8181d09335815f7d7f74b8b48d73459c8f03f`,
`ev_0fd891b2085d28246e72fe78d3efcf336cf9230e7f50e4cec6f76125fd667050`.

## Session and feature behavior

The startup notification uses message type `11`, feature `05`, function `01`,
and a one-byte success payload. The legacy app waits two seconds, then retries
up to five times at twenty-second intervals until its startup flag is set.
Its ACK path sets that flag based on function `01`, requests vehicle, climate,
song, volume and audio state, and starts further timed alert requests.
The original NACK path also sets the startup flag for function `01`.
OpenSense does not reproduce that permissive handshake.

The receive path calls `parseReceivedData` from
`BluetoothXUVService$ConnectedThread.run` at DEX code-unit offset 80, invoking
method index 486. Its original implementation treats each socket read as a
complete packet, initially uses 4096 bytes then 100 bytes, and does not reliably
honor the read length. OpenSense's offline decoder implements bounded stream
framing independently. Disconnect/error paths close streams, return to a listening
state and report connection loss; HFP/adapter broadcasts can trigger another
connection attempt. Timing/reconnection behavior has not been exercised on a car.

Static feature paths cover door/service/brake/TPMS alerts, park lamp, wipers,
distance to empty, speed, climate status, source/playback/volume/song controls,
radio, reminders, personal/vehicle records and local settings. Presence of a
function or UI does not prove its compatibility with every XUV500 variant.
Climate paths inspected here report status; no climate write command is claimed.
Several telemetry fields need unit, scaling, sign and bit-mask confirmation.

REA evidence: `ev_82f8edad059efa35dfe90c8fae92b28535a1f897f6d7956bb8f0fd8680894bc3`,
`ev_a3c98dbd563464b118b78a62f1c325786d1de7e4fa3fefec579dc6dd47b97213`,
`ev_00a07d45fa8387e1839d0bbcdefd102543de35c84b63231818bfe9ce31fc97d6`.

## Coverage and remaining proof

REA 6.5.0 with the pinned JADX headless 0.7.1 engine was used. Engine SHA-256:
`6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`.
The complete DEX inventory has 268 class definitions, including inner classes;
REA's package/search overview reports 58 top-level classes. Private exports
include all 268 smali classes and decoded resources. Java export reported one
decompilation error in `InfotainmentClientService$3.handleMessage`; its smali
is available. All raw/decompiled proprietary material remains ignored locally.

Still required: independently authenticated APK provenance, captured vehicle
traffic with consent, supported model/year/head-unit mapping, framing/CRC
cross-checks against captures, Android permission/device tests, and physical
reconnection/feature validation. Local synthetic parser tests establish only
the implementation's behavior on their stated test inputs.
