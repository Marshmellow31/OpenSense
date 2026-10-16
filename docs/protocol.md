# Protocol investigation

As of 2026-10-16, two distinct protocol families have **verified static APK evidence**.
Neither has been verified with a vehicle capture or a physical Android phone.
OpenSense implements independent **offline decoders** for both families. Its Bluetooth
screen performs discovery/SDP only; it opens no vehicle data socket and sends no
application protocol commands. The simulated dashboard is a separate code path.

## Evidence and confidence vocabulary

- **verified — static**: observed in an identified APK, manifest, DEX/Smali or REA result.
- **verified — local test**: passed a named test using synthetic inputs, not a vehicle.
- **historical**: manufacturer/developer documentation or an archival catalogue claim.
- **needs-confirmation**: unresolved on a specific head unit, firmware, variant or phone.
- **recommendation-not-fact**: a design choice for OpenSense.

See [research and artifact identities](research.md), [legacy specification](protocol-legacy.md),
[new specification](protocol-new.md), and [feature compatibility](compatibility.md).
These documents contain factual descriptions and independently authored code only.
Original APKs, assets and decompiled source are excluded from the public project.

## Family comparison

| Property | Legacy `com.visteon` 0.46 | New `com.contus.mahindra.xuv500` 3.5 |
| --- | --- | --- |
| Android transport in inspected code | Classic RFCOMM, secure socket factory | Classic RFCOMM, insecure socket factory; reflective channel-1 fallback |
| Service UUID | Standard SPP `00001101-0000-1000-8000-00805F9B34FB` | Same standard SPP UUID |
| Device selection | Bonded name `MAHINDRA_BLUESENSE` | HFP-connected name `XUV500` or resource value `MAHINDRA` |
| Envelope | `ED` header, explicit payload length, `EE` trailer | Three logical header bytes; length-delimited nibble stream |
| Integrity | CRC-16, polynomial 1021, init 0 | Sum of logical bytes except checksum, modulo 256 |
| Receive encoding | Binary bytes | Each nibble is a wire byte offset by 0x50 |
| BLE | Not used in inspected connection path | Not used in inspected connection path |
| Car compatibility | Needs parked hardware/capture confirmation | Needs parked hardware/capture confirmation |

A shared SPP UUID does **not** make the protocols interchangeable. The selected
profile is user-supplied context, not detected vehicle identity or proof of support.
No custom cryptographic authentication challenge was found in the inspected paths;
vehicle-side authentication and pairing requirements remain unverified. OpenSense
does not reproduce reflective socket fallbacks or try to bypass pairing.

## Data origin boundary

Both APK transport paths read from the infotainment system. That establishes the
app's immediate Bluetooth peer; it does not identify the underlying ECU or bus for
each signal. Door/body, HVAC and TPMS data plausibly reach the head unit from other
modules, but their routing, identifiers and hardware fitment need evidence.
There is no OBD, CAN, ECU or firmware access in OpenSense.

The newer app also includes phone location, local records, wearable messaging,
social/cloud and emergency assistance paths. A UI tile is insufficient evidence
that its data comes from the car. Location can have phone and head-unit sources;
the full parked-location selection/persistence path requires further tracing.

## Verification gates

1. Record exact head-unit manufacturer/model, firmware, vehicle build year, trim,
   transmission and original app generation. Inspect these while safely parked.
2. Confirm app identity against an independently known-original APK/certificate.
3. Obtain consented Bluetooth captures with direction, timestamps and a written
   action log. Keep full phone bugreports and captures private.
4. Compare framing, checksums, field lengths, units and state changes with captures.
5. Review a minimal read-only connection/session sequence before implementing it.
   Some apparent reads require startup notifications/ACKs; they are not passive.
6. Get owner approval before transmitting newly reverse-engineered control commands.
   No such command was sent in this session.

## Unresolved questions

- Which factory or replacement head units in W7 use the newer profile?
- Can a second SPP client coexist with HFP/audio/Android Auto on each firmware?
- Which initialization/acknowledgment messages are necessary and read-only in effect?
- Which message variants, signed fields, scaling factors and unavailable sentinels exist?
- How do loss, ignition/accessory mode, fragmentation and reconnect affect state?
- Which capabilities are advertised versus inferred from received messages?

The decoders intentionally expose raw packet fields. They do not feed the dashboard
or classify a parsed packet as a real vehicle measurement. `SIM1` is an OpenSense
test-only format and is never presented as a recovered vehicle protocol.
