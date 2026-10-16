# Phone feature implementation

Scope confirmed 16 October 2026: **portrait phone app, dark Material 3 default,
private local records and Android sharing. No cloud accounts or Wear OS for now.**
The implementation is independent Kotlin code, not a repackaged/copied original APK.

## Implemented surfaces

| Original feature / owner task | OpenSense implementation | Evidence / limits |
| --- | --- | --- |
| Both Bluetooth app generations | Explicit paired-head-unit connection, family-specific RFCOMM/startup/decoders | Synthetic protocol tests; real head units untested |
| Climate | Legacy status; New status, AC/Econ/auto/recirc/rear, fan/vent, Celsius target | Commands from APK generators/Smali; physical effects unverified |
| Media / tuner | Playback, source availability/selection, volume, radio mute, band cycling, six preset selection/storage | New preset-store checksum sums the 16-bit frequency field as the original does; hardware unverified |
| Fuel diary | Add/edit/delete dated fills, odometer/litres/cost/notes/full flag, weighted full-to-full efficiency and history chart | Integer minor-unit costs; partial fills included correctly; manual records |
| Reminders | Add/edit/complete/reopen/delete, date/time and lead time, notifications, boot/package-update rescheduling | Inexact Android alarms; alerts can be delayed by system battery policy |
| Personal/vehicle information | Owner/contact/address/licence/registration/engine/chassis/year/colour/fuel/tank/insurance/notes | Saved privately; no identity fields in standard export |
| Documents / manual | User-selected persistent PDF/image links, external viewer, link removal; own manual PDF | No original proprietary manual or assets redistributed; provider/viewer availability matters |
| Parked car / finder | Explicit current-location save, accuracy/time/note, straight-line distance, maps handoff and forget | Phone location; no fabricated automatic parking trigger or live location sharing |
| Trip diary | User-started foreground GPS recording, route preview, distance/moving time/max speed/slowing estimates, local history/delete | Phone GPS estimates; process-loss checkpoints labelled interrupted; not an EcoSense score |
| Head-unit EcoSense | APK-derived current-trip score, distance/economy/duration/speed/event fields | New 1392-bit record consumer; private location/identity segments ignored; captures still needed |
| Warning preferences | Door/service/brake/tyre display choices, low-range and GPS speed references | Does not replace vehicle warning systems or invent decoded safety alerts |
| Assistance | Saved contact in dialer, blank dialer, reviewed saved-parking-location message | No automatic crash detection, SMS or calls; no CALL_PHONE/SEND_SMS permission |
| Privacy / sharing | Explicit non-identity JSON export, local-record deletion, document-grant cleanup | Notes/titles remain in export; review before sharing; no cloud sync |

Access everyday tools from **Vehicle → Your garage**, **Settings → Your garage**,
or **Insights**. Generation selection and simulation remain in Settings.

## Reverse-engineering anchors

Private REA-generated sources were inspected for `DigitalFuelLogNewEntry`,
`AddReminder/Reminder/AlarmManagerBroadcastReceiver`, `PersonalInfo`,
`DocumentWallet/DocumentViewer/EManual`, `CarFinderNew/CarLocationActivity`,
`EcallActivity`, `AudioControl`, `utils.n` and `utils.f`.
No proprietary source, original APK, private packet capture or original graphic
was added to the public source tree.

Radio distinctions: ID 25 selects a preset, ID 26 cycles the band, and ID 32 stores
the current frequency in a preset. These must not be treated as one tuning API.
EcoSense fields come from `utils.f.a(byte[], Context)` for the 174-byte logical
current-trip record, not a guessed phone-derived score. Archive formats, invalid
sentinels, units and firmware differences still need corroboration.

The requested viral-reverse-engineering skill concerns social-content analysis,
not DEX/Android protocols. Its observable-evidence rule is respected; REA/static
APK paths supply behavior evidence, and Impeccable supplies the native UI workflow.

## Deliberate proof boundaries

This does **not** establish complete original-app runtime equivalence. Automatic
head-unit parking/crash workflows, optional lounge-light firmware modes, every
historical EcoSense archive layout and legacy radio-specific details are not
enabled from guesses. The inspected lounge-light screen reuses HVAC selectors;
it cannot establish that the user's W7 supports a lighting command.

Social/account/cloud and watch integrations are excluded by the user's confirmed
scope. Android share/dial/map/viewer intents are user handoffs; opening them does
not prove message delivery, a placed call, navigation completion or cloud storage.

## Local persistence and bounds

`GarageStore` uses a versioned JSON file with Android AtomicFile in private app
storage, asynchronous serialized writes and visible save errors. Records never
enter enum diagnostics. Limits: 1,000 fills, 200 reminders, 100 document links and
50 GPS trips. Saved traces are reduced to at most 2,000 points per trip. A corrupt
existing data file is preserved and blocks overwriting rather than silently reset.

GPS recording is a deliberately started location foreground service, independent
of the Bluetooth session. Bluetooth still closes when the Activity backgrounds;
the user-started trip recorder continues with its notification until stopped.
No ACCESS_BACKGROUND_LOCATION permission or automatic GPS session is requested.

## Verification

Results are recorded in `PROGRESS.md`, with emulator GPS/notification tests clearly
separated from physical-device/vehicle evidence. Native visual review uses actual
portrait emulator screenshots with synthetic fixture records, normal and 1.3× text.
No browser screenshot is used as native rendering evidence.
