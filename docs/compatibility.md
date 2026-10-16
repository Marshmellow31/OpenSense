# Compatibility and feature matrix

No physical vehicle is verified compatible yet. Profile selection records the
owner's choice; it does not detect hardware or unlock controls. The primary
test target is the user's 2018 XUV500 W7, with head-unit firmware still unknown.

Evidence labels: **S** = inspected APK static path; **H** = historical developer
or manufacturer description; **U** = hardware/firmware support needs-confirmation.
See [research](research.md) for hashes, sources and scope, and [protocol](protocol.md)
for the incompatible message formats. No feature is vehicle-verified.

| Feature | Original APK/documentation evidence | Required hardware / data source | Applicable variant evidence | Protocol status | OpenSense implementation |
| --- | --- | --- | --- | --- | --- |
| Bluetooth discovery/status | S: both transport paths use Classic SPP | Compatible factory/replacement head unit and Android adapter | H: legacy W8; newer New Age/facelift leads; W7 U | Separate framing profiles recovered statically; car session U | Classic discovery/SDP; simulation lifecycle; vehicle socket blocked |
| Music/media | S: legacy RadioActivity/audio functions; newer AudioControl and media IDs 19/20/21/23 | Head-unit source/playback service; supported media source | Exact per-trim availability U | Raw framing decoded offline; control behavior U | Simulated playback display; vehicle writes disabled |
| Climate display | S: legacy climate data holder, newer ClimateControl/192-bit path | Compatible HVAC-to-head-unit integration | W7 and each head-unit firmware U | Static consumer paths; scaling/units U | Simulated temperatures; vehicle unavailable |
| Climate controls | H: newer developer listing; S: newer controls/code paths | Suitable HVAC/control interface | Legacy write support not established; newer per-trim U | No approved/validated control sequence | Disabled |
| Fuel/economy/range | S: legacy vehicle holder; newer message 10/economy/DTE path | Vehicle data made available by infotainment | Per-variant and firmware U | Static assignments found; capture/units U | Simulated fuel/range; no decoded vehicle values |
| Tyre pressure/temperature | S: legacy TPMS parser; newer Tiretronics consumer | TPMS sensors, receiver and exposed signals | Do not assume W7 has factory TPMS; U | Payload candidates; masks/scaling U | Unavailable, even in demo |
| Door/body status | S: legacy alert/door module; newer vehicle/status consumers | Body signals exposed by infotainment | All exact trims U | Static paths found; validity/bit mapping U | Simulated closed-door state; vehicle unavailable |
| Trips/EcoSense | S: newer GreenDrive modules/local DB/wearable paths; H: developer listing | Compatible transmission/firmware and exposed trip signals | Historical older manual W6/W8/W10 restriction; W7 U | Several length-based records; full semantics U | Unavailable |
| Last parked location | H: developer location feature; S: newer location modules/preferences | Phone location and/or head-unit GPS; source selection unresolved | No per-trim verified matrix | Parking trigger/source needs more tracing | Unavailable; no location permission on modern app |
| Warnings, lamps/wipers, speed | S: legacy warnings/lamp/speed modules; newer related dispatch | Applicable sensors/body data/head-unit integration | Exact hardware U | Some raw consumers mapped; masks/units U | Unavailable |
| Reminders/personal records | S: local legacy SQLite/reminder activities; newer reminder modules | Primarily phone-local storage, not proof of a vehicle service | App-generation feature | Local feature rather than vehicle protocol | Not implemented |
| Emergency assist | S: newer emergency/SMS/location/call paths | Exact infotainment event and phone/network permissions | Variant/region/firmware U | Not validated; no emergency testing performed | Not implemented; no SMS/call permissions |
| Wearable companion/social/cloud | S: newer wearable and third-party namespaces | Companion device/services/accounts where applicable | App feature, not car capability | App-to-watch/network boundaries need further mapping | Not implemented; no Internet permission |

## Vehicle profiles in the app

| User-selectable family | Variants offered as research context | Current meaning |
| --- | --- | --- |
| XUV500 Legacy BlueSense | W8; Other / unsure | Candidate for original `com.visteon` head units |
| XUV500 New BlueSense | W6, W8, W10, W7, W9, W11; Other / unsure | Candidate for newer `com.contus.mahindra.xuv500` head units |

Variants are choices for evidence collection, not promises of support. New Age
and facelift naming overlap across model years/markets; a head-unit replacement
can invalidate a year-based assumption. Selecting New BlueSense W7 does not claim
that the newer 3.5 APK was proven on this specific car. An unknown or contradictory
head unit should remain unavailable rather than silently using the other decoder.

## Record needed for each future verification

Use the compatibility issue form with model/year/trim/transmission, stock versus
replacement head unit, firmware/build information, phone/API level, selected
profile, APK hash if relevant, capture/action-log identity, exact observed feature,
parked test conditions and reproducible outcomes. Never publish VIN, MAC address,
precise location, personal data or unredacted bugreports. A connected socket,
successful checksum or visible UI does not establish every feature's correctness.
