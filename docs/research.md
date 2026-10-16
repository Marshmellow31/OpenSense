# BlueSense research log

Investigation date: 2026-10-16. Primary owner test target: **2018 XUV500 W7**,
as supplied by the user. Head-unit and firmware identity have not been supplied.
The workspace was initially empty, with no APK inputs or Git repository.
The requested `xuv500-bluesense-research` skill was not found in the searched
installed skill/plugin locations, so structured static investigation was used.
The user subsequently requested archival downloads of both apps and REA analysis.

## Identified and downloaded artifacts

| Property | New BlueSense | Original BlueSense |
| --- | --- | --- |
| APK package, locally verified | `com.contus.mahindra.xuv500` | `com.visteon` |
| Version name / code | 3.5 / 28 | 0.46 / 1 |
| File size | 31,427,383 bytes | 8,480,176 bytes |
| Minimum SDK | 17 | 7 |
| Target SDK | 28 | Not declared in inspected manifest |
| APK SHA-256 | `ebaedc28da5ccc1d5f09d9f95f4c95762d97082cde9ea50f47c6b8e077f2ad06` | `ebdac117ac7e532079057bcda4a2de9bf4d9f0d1b6bfafbdd34fe3edc70d5231` |
| Signer certificate SHA-256 | `94160ccdd30ce1f4a168cfbc9b3c944d0645f810eb951a89c32fcc2ae8059853` | `e7465890d7f3bc7fc6d2277c161ef9a2a07f92a1482d389d2aaa936aaa4818e3` |
| Signer SHA-1 matching archive listing | `b01b5c053fef2abfd300957aeb0543173fd79c66` | `7b71abe8d13034534ded08bf80073f65588bcb82` |
| SDK apksigner result | v1 verifies; v2/v3/v4 absent | v1 verifies; v2/v3/v4 absent |
| Native `.so` members | None found | None found |

Both were downloaded on this date from APKPure's public download links for
[New BlueSense](https://apkpure.net/new-blue-sense-xuv500/com.contus.mahindra.xuv500/download)
and [legacy BlueSense](https://apkpure.net/mahindra-blue-sense-app/com.visteon/download).
The working source was `https://d.apkpure.net/b/APK/<package>?version=latest`.
Initial requests returned 403; a later ordinary download request succeeded.
The `.com` download host closed connections. Private provenance sidecars retain
the successful URLs and method; originals remain under ignored `apk-input/`.

Signature validity and a matching archive fingerprint do not independently
authenticate the publisher. No known-original device export was available for
comparison. The newer v1 signature also produces warnings about unsigned
`META-INF` version metadata; these are retained in the private signer report.
No claim that these archives are malware-free is made. Neither APK was installed
or executed on this machine, an emulator, a phone or a vehicle.

## Permission and dependency observations

The legacy manifest declares Bluetooth/admin, vibration, calling, reboot and
boot-completed permissions; it has no Internet permission. Its 268 DEX classes
consist of 266 `com.visteon` classes and two Android annotations. Inspected modules
include infotainment service, Bluetooth transport, parser, UI/data holders,
reminders, personal records and local SQLite storage. No third-party SDK namespace
or native library was found within that bounded inventory.

The newer manifest declares Bluetooth/admin; calling/phone state/call log and
outgoing-call access; SMS; fine/coarse location; contacts/accounts/credentials;
storage/camera; Internet/network state/change; wake lock, vibration, boot receiver,
task/cache permissions and Google messaging receive. READ_PHONE_STATE occurs twice.
These are declarations, not evidence of actual use, grant or current OS support.
The private manifest record preserves exact names without granting any permission.

Newer package namespaces include Android support libraries, Google Play services,
Gson, Facebook, Twitter4J, Glide, Picasso, AndroidSVG, ZXing, OkHttp/Okio and
application-specific EcoSense/wearable/location/database paths. This identifies
packaged code families, not a fully versioned software bill of materials. No
credentials, tokens, original icons or other proprietary assets were copied into
OpenSense. Its own manifest does not inherit those broad legacy permissions.

## Historical application generations

These catalogue entries are **historical leads**, independent of local APK facts:

| Candidate | Historical evidence / relevance |
| --- | --- |
| `com.visteon` 0.46 | Older W8-focused generation; archived description cites infotainment 04.04.00 |
| `com.contus.mahindra.xuv` 1.0 | Separate 2015 New Age listing; not downloaded/analyzed in this session |
| `com.contus.mahindra.xuv500` | 2.1 (2017-04-28), 2.2 (2017-08-11), 2.5 (2017-11-14), 2.6 (2018-02-25), 2.7 (2018-04-22), 2.8.2 (2018-08-06), 2.9 (2019-03-26), 3.1 (2019-04-06), 3.5 (2019-09-23) in archive catalogue |
| `com.mahindra.bluesense.common` | Current retrieved Play listing describes Scorpio-N Z4; not an XUV500 compatibility lead |

The [archival version catalogue](https://apkpure.net/new-blue-sense-xuv500/com.contus.mahindra.xuv500/versions)
provides the newer timeline. [AppBrain's legacy listing](https://www.appbrain.com/app/mahindra-blue-sense-app/com.visteon)
and [separate New Age listing](https://www.appbrain.com/app/m-m-blue-sense-new-age-xuv500/com.contus.mahindra.xuv)
help distinguish package names. Archive dates for 0.46 disagree (2012 versus 2015);
they should not be treated as verified original release dates. The supplied image's
October 2024 delisting date was not independently established. Failed Play page
retrievals do not prove a delisting date or availability in every region.

## Manufacturer/developer evidence

- [Mahindra's April 18, 2018 launch release](https://www.mahindra.com/news-room/press-release/en/mahindra-launches-plush-new-xuv500)
  confirms BlueSense among range-level equipment. It is not a W7-by-feature matrix.
- [Mahindra's developer-owned New Age iOS listing](https://apps.apple.com/br/app/blue-sense-app-new-age-xuv500/id1091155358)
  documents audio/climate, alerts, location and vehicle information. Its EcoSense
  description restricts one older implementation to manual W6/W8/W10 with firmware
  9.0.0 or above. That restriction cannot automatically be transferred to a facelift W7.
- [Mahindra's current common-app Play listing](https://play.google.com/store/apps/details?id=com.mahindra.bluesense.common)
  specifies Scorpio-N Z4, supporting exclusion from the XUV500 protocol mapping.

Bounded GitHub searches for XUV500 Bluetooth/BlueSense and both package names did
not identify a relevant reusable XUV500 implementation. Repositories sharing the
BlueSense name often concern unrelated environmental sensors. This search is not
proof that no compatible implementation exists.

## Analysis method and coverage

1. SHA-256, bounded ZIP/DEX string inventory, SDK aapt2 manifest/badging and
   apksigner signature verification through `tools/inspect_apks.py`.
2. Local npm installation of [REA](https://github.com/morluto/rea) 6.5.0 with scripts
   disabled during installation. No global agent registration was changed.
3. REA package/class/method/reference inspection with JDK 21 and headless JADX 0.7.1.
   Engine SHA-256 was checked against REA's pinned metadata:
   `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`.
4. Whole-package Java/resources and Smali exports in ignored private folders;
   focused connection, framing, checksum, session and feature traces.
5. Independently authored offline parsers and synthetic tests. No original code
   was pasted into production sources, and no runtime commands were sent.

Legacy export: 268 classes including inners / 58 top-level, one Java decompilation
error, zero warnings. New export: 4,170 rows including inners / 2,095 top-level,
eight errors, forty warnings. New app namespace accounts for 527 rows. Smali
exports preserve a route to inspect Java failures; this does not automatically
resolve their semantics. Native APK execution, dynamic analysis, complete source
reconstruction and end-to-end feature equivalence were not performed.

JADX/REA and SDK tools were used. Apktool and Wireshark were not initially installed;
SDK/JADX supplied manifest/resource inspection. No packet capture was available for
Wireshark analysis. The [protocol index](protocol.md) records the findings, evidence
IDs, hypotheses and required hardware proof.
