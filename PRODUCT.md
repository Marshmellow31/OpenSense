# Product

<!-- impeccable:product-schema 1 -->

## Platform

android · portrait-only phone experience

## Users and purpose

XUV500 owners exploring an independent replacement for the discontinued BlueSense
applications. Primary test vehicle: user-owned 2018 W7. Operated while safely parked.

## Capabilities and constraints

Native Kotlin/Compose/Material 3 app with Classic discovery/SDP and explicit
Legacy/New RFCOMM sessions. APK-derived media/newer-HVAC commands are implemented;
physical acceptance and effects remain unverified. Synthetic data must stay labelled
and isolated. Actual connection requires deliberate head-unit selection.
Vehicle profile and simulation entry belong in Settings, without first-run gating.
Dark theme defaults on; users may choose light mode. No account or network permission.
The user confirmed private phone storage and Android sharing; skip cloud accounts
and Wear OS for this version. Ownership tools include fuel diary, reminders,
personal/vehicle records, documents/manual, parked-car finder, GPS journeys,
warning preferences and assistance. Phone GPS and head-unit EcoSense stay distinct.

## Confirmed visual brief

Replace the original UI with four destinations: Vehicle, Climate, Media, Insights.
One real XUV500 image cutout on a platform; 3D deferred. Glacier Blue Material 3 color
scheme, dark/light tonal surfaces, visual cabin/airflow and clear media controls.
Connection details, diagnostics and settings remain accessible from top-level actions.
The image is an illustrative XUV500, not proof of a selected variant's equipment.

## Evidence and boundaries

See PROGRESS.md, docs/protocol.md and docs/live-integration.md. Static findings and synthetic tests
do not prove real-car compatibility. Preserve private research exclusions and original
independently authored parsers. No door locks, safety-critical ECU or firmware writes.
Supported media/HVAC actions are experimental and must not imply verified effects.

## Accessibility

System Back, edge-to-edge insets, 48 dp targets, larger text and compact portrait phones.
Reduced motion follows Android animation settings. Unknown values remain unknown.
