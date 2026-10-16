# Architecture

OpenSense separates its Android interface, platform discovery, and platform-independent simulation logic. No module currently implements a real BlueSense data connection.

| Module | Responsibility | Boundary |
| --- | --- | --- |
| `app` | Compose screens, view model, user permissions, preference storage | Stores theme and selected vehicle profile only |
| `bluetooth` | Classic discovery, paired-device listing, standard SDP UUID requests | No RFCOMM socket, GATT session, vehicle payload, or control API |
| `core` | Models, capability states, synthetic codec/transport, simulation repository, diagnostics | JVM-testable; synthetic frames are never treated as vehicle data |
| `tools` | Offline APK archive inventory and tests | Private input/output; never executes or installs APKs |

## User flow and state

On first launch, the user selects a Legacy or New BlueSense family and a variant. `VehicleProfile` validates the variant against the selected family's list. The selection is a research preference, not detection of the car's hardware or a proof of support. Settings allow the user to choose again.

`OpenSenseViewModel` owns simulation and Bluetooth discovery lifetimes. The dashboard starts without telemetry. Starting simulation obtains generated `VehicleSnapshot` values through `SimulatedTransport` and `SimulationRepository`; stopping it clears the current sample. Backgrounding the app stops active simulation and discovery. Reconnection is user-initiated at the app level.

The simulation repository exposes connection states and bounds automatic retry attempts within an active simulated session. A new decoder is created for each retry so a partial frame cannot cross session boundaries. Cancellation ends retries and disconnect clears stale data.

## Codec and capability boundary

`SimulationCodec` uses an invented newline-delimited `SIM1` format solely for demos and tests. Its stream decoder supports fragmented and concatenated input, validates fields, caps frames, and resynchronizes after oversized data. This format is not a recovered vehicle wire protocol. `VehicleProtocol` remains the fail-closed live integration boundary: it rejects input because hardware behavior is unverified.

`LegacyFrameDecoder` and `NewBlueSenseDecoder` are separate offline parsers for statically recovered APK framing. The legacy parser validates CRC-16 and resynchronizes on a bounded binary envelope. The newer parser validates incoming offset nibbles and an additive checksum; its stream adapter requires a known initial boundary and stops on alignment loss. Neither decoder connects to the simulation repository or the Android Bluetooth discovery component. A future real repository must carry separate provenance and field-validity rules. See [protocol](protocol.md).

Capabilities distinguish simulated demonstrations from unverified real support. Feature controls remain unavailable where there is no verified implementation. Switching vehicle profiles does not enable a transport or control path.

## Bluetooth discovery

`BluetoothDiscovery` requests the platform permissions appropriate to the Android version, observes paired and discovered Classic devices, and lets the user request standard SDP service metadata. It bounds the observed-device list and stops scanning after a timeout. It handles denied/revoked permissions and an unavailable adapter.

Device addresses are used internally to deduplicate discovery results. UI records use session-local identifiers and generic labels. Diagnostic events receive predefined enums, never names, addresses, payloads, or exception strings. Returning an SDP UUID establishes only that the platform returned that metadata; it does not establish that a BlueSense protocol session can be opened or interpreted.

## Storage and future integration

Theme and vehicle profile preferences persist locally. Telemetry, discovery results, and bounded diagnostic events are memory-only. The app declares no Internet permission and has no account or analytics path.

Any future real transport must be introduced separately from simulation, with its evidence and feature-specific verification visible to users. Protocol framing, reconnection, authorization, stale-data handling, diagnostics, and compatibility must be reviewed before changing the blocked vehicle-data state. Existing simulation tests cannot validate hardware or original-app behavior.
