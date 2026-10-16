# Security reporting

OpenSense is a research preview. It currently implements simulation and Classic Bluetooth discovery/SDP, with no vehicle socket or vehicle control commands. Discovery still interacts with Android's Bluetooth stack and must respect permissions and lifecycle boundaries.

## Report privately

Do not post exploitable details, original APKs, private links, device names or addresses, VINs, credentials, raw packet captures, or identifying diagnostics in a public issue.

When this project has a public GitHub repository with private vulnerability reporting enabled, use its **Security → Report a vulnerability** action. If that action is unavailable, open a minimal issue requesting a private reporting channel, without disclosing the vulnerability or sensitive evidence. No dedicated reporting email or response-time commitment is configured in this initial workspace.

A useful private report contains the affected OpenSense revision, Android version, relevant permissions, reproducible steps, expected and actual behavior, and a sanitized explanation of impact. Specify whether evidence came from source review, simulation, an emulator, a physical phone, or an owner-authorized stationary vehicle.

## Review priorities

- Untrusted Bluetooth broadcasts and permission revocation must not bypass permission checks or crash the app.
- Parsers must bound memory, reject malformed input, and discard stale data on reconnect.
- Simulation must remain visibly distinct from real observations.
- Diagnostics must not expose device identifiers, raw payloads, or exception text.
- No unreviewed path may open a vehicle socket or send vehicle commands.
- Original APKs, decompiled material, private analysis, and signing secrets must remain excluded from source and CI artifacts.

Only the current development source is in scope for maintenance. There are no published supported release branches or verified vehicle-support guarantees yet.
