---
name: OpenSense
description: A calm Glacier Blue garage companion for XUV500 owners
colors:
  primary: "#A9C7FF"
  on-primary: "#12305C"
  primary-container: "#294775"
  on-primary-container: "#D8E5FF"
  secondary: "#B9C7DF"
  secondary-container: "#303E54"
  tertiary: "#E8C383"
  tertiary-container: "#58431D"
  background: "#0C111A"
  surface: "#101620"
  on-surface: "#E3E8F2"
  on-surface-variant: "#B5C0D1"
  surface-container-low: "#121B27"
  surface-container: "#18212E"
  surface-container-high: "#212D3D"
  outline: "#8391A6"
  outline-variant: "#364355"
  error: "#FFB4AB"
  light-primary: "#355F9A"
  light-background: "#F5F7FC"
  light-surface: "#FAFBFF"
  light-on-surface: "#172131"
  light-on-surface-variant: "#4F5D70"
  light-surface-container: "#E9EEF7"
  light-surface-container-high: "#DEE6F2"
  light-outline-variant: "#C3CDDC"
typography:
  headline:
    fontFamily: "Android system sans-serif"
  title:
    fontFamily: "Android system sans-serif"
  body:
    fontFamily: "Android system sans-serif"
  label:
    fontFamily: "Android system sans-serif"
rounded:
  panel: "16dp"
spacing:
  compact: "8dp"
  date-actions: "12dp"
  row: "14dp"
  short-screen: "16dp"
  panel: "20dp"
  screen: "24dp"
  bottom: "28dp"
  wide-screen: "36dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
  button-outlined:
    textColor: "{colors.primary}"
  button-text:
    textColor: "{colors.primary}"
  input:
    textColor: "{colors.on-surface}"
  navigation:
    backgroundColor: "{colors.surface-container}"
  chip:
    textColor: "{colors.on-surface}"
  tonal-panel:
    backgroundColor: "{colors.surface-container}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.panel}"
    padding: "{spacing.panel}"
  detail-row:
    textColor: "{colors.on-surface}"
    padding: "12dp 0"
---

# Design System: OpenSense

## Overview

**Creative North Star: "A calm garage display"**

OpenSense extends the user-approved Glacier Blue Material 3 world established on 16 October 2026. A real XUV500 cutout rests on a low oval platform on Vehicle; quiet headings, aligned readings, tonal actions and grouped status rows make it an automotive companion. The image is illustrative, not proof of a selected variant's equipment. Retain the existing asset; 3D remains deferred.

The built ownership extension uses the same world for private phone records: full task routes, readable forms, grouped lists and useful empty states. Material 3 supplies native structure and interaction. Dark is the fresh-install default; an explicit light choice persists. This remains a portrait-only phone product with Vehicle, Climate, Media and Insights as its four main destinations. Vehicle generation, simulation and appearance remain in Settings without an onboarding gate. Private phone storage and deliberate Android handoffs constrain the interface; cloud accounts and Wear OS are outside the confirmed scope.

**Key Characteristics:**
- Glacier Blue actions on midnight tonal surfaces, with a separate cloud-light scheme.
- System Material typography, vector icons and native controls.
- A photographic vehicle signature with restrained diagrammatic data elsewhere.
- Scrollable ownership routes with honest source, permission and empty-state language.

This source-derived refresh preserves the established world; it is not a new concept or fresh full-surface approval. Source anchors are `DesignSystem.kt`, `GarageComponents.kt`, `GarageScreens.kt`, `GarageForms.kt`, `OwnershipScreens.kt`, `TripScreens.kt`, `SettingsScreens.kt` and `MainActivity.kt`. Feature semantics live in `docs/phone-feature-parity.md`; product authority remains `PRODUCT.md`.

## Colors

Frontmatter records recurring explicit assignments from `DesignSystem.kt`, not every inherited Material default. Native components resolve the active `MaterialTheme.colorScheme`; light-prefixed tokens describe the alternate scheme. Dynamic wallpaper color is not enabled.

### Primary

Glacier Blue marks actions, icons, selected emphasis and route traces. Paired dark ink keeps filled actions readable; primary containers support quieter emphasis. Light mode uses deeper blue emphasis against cloud surfaces.

### Secondary

Cool slate-blue supports subdued controls and containers, subordinate to primary actions.

### Tertiary

Warm amber identifies simulation or attention, including Science and Demo indicators. It must not imply a verified vehicle warning. Error serves failures, blocked permissions and interrupted states where implemented.

### Neutral

Midnight background, surface and container tones establish depth. Pale on-surface text carries primary content; muted on-surface-variant carries subtitles and provenance. Outline roles define fields, separators and the platform edge. Light mode uses cloud-white and blue-gray surfaces with dark ink.

**The Source Clarity Rule.** Keep simulated, phone-derived, manually entered and head-unit values visibly distinguishable; color alone is insufficient.

## Typography

No custom `Typography` or brand font is supplied to `MaterialTheme`: use inherited Material 3 system sans-serif roles and Android sp/font scaling. Sidecar browser preview metrics are visual translations, not new native overrides. The incumbent direction requests clear measurement figures; explicit tabular-number font features are not implemented in the inspected source.

- `headlineLarge` with Medium weight: main tab heading; large trip distance also uses headlineLarge.
- `headlineMedium`: garage introduction and selected measurements/scores.
- `headlineSmall`: profile, connection and diagnostic introductions.
- `titleLarge`: app-bar context, panel titles and prominent saved-record summaries.
- `titleMedium`: section labels, dates and threshold values; shared sections add SemiBold.
- `bodyLarge`: detail-row title; `bodyMedium`: descriptions and errors; `bodySmall`: explanatory, permission and provenance text.
- `labelSmall/Medium/Large`: compact metadata, chart captions, status and trailing values.

**The Human Context Rule.** App bars show “Trip details”, “Edit fill-up” and “Edit reminder” for record routes; never expose internal route keys or record IDs as titles.

## Layout

Scaffold insets surround the content row. Each tab/route resets its own vertical scroll state. Compact content has horizontal gutters (24 dp), bottom padding (28 dp) and normal vertical separation (24 dp). Let content wrap and scroll with larger text; do not scale the entire page down. The established accessibility target is at least 48 dp touch areas; still captures do not prove every hit region or keyboard state.

Four labelled bottom destinations appear on main compact surfaces. Task routes replace them with app-bar Back and honor system Back; record editors return to their list, other garage routes to Your garage. Access Your garage from Vehicle, Settings or Insights. Adaptive source remains at width ≥600 dp: a rail, gutters (36 dp), and a 72 dp icon column when height <480 dp. Existing vehicle surfaces retain their implemented wide two-column composition. This is code behavior, not expanded tablet/landscape product support.

Garage introductions use an 8 dp title/subtitle gap. Panels use 20 dp padding and 12 dp internal gaps. Detail rows have minimum height (64 dp), vertical padding (12 dp), icon/text spacing (14 dp) and a weighted text column. Record lists use 8 dp entry gaps and horizontal dividers. Personal records form a single scrolling column grouped as Owner, Vehicle, and Policy & assistance.

Date/time actions use `FlowRow`, with horizontal gaps (12 dp) and vertical gaps (8 dp). Preserve complete “Choose date” and “Time” labels when wrapping onto separate lines. Reminder lead chips also wrap. Native date and 24-hour time pickers retain confirm/cancel actions.

Native fixture evidence: `.impeccable/review/r2-phone` (API 36, 1080×2400, font 1.0) and `r2-compact` (API 36, 840×1680, font 1.3), with synthetic records and metadata in `captures.txt`. The full review required three fixes; `garage-verdict.md` resolves date/time wrapping, human titles and slider semantics. Its ship verdict covers those three scored fixes only, not a new review of every surface. No native web detector ran. Physical gestures, TalkBack listening, IME fit, real GPS/battery behavior and head-unit effects remain unverified by this documentation pass.

## Elevation & Depth

Use Material tonal layering rather than custom decorative shadows. Shared panels are flat surface-container sheets; grouped rows stay open on the background with dividers. The vehicle platform uses tonal ovals and an outline arc beneath the real cutout. Never morph the image to imply doors opened.

Incumbent scene motion is limited to airflow while a simulated fan runs and follows system Remove animations. Ownership forms add no custom motion system; Material components retain library interaction behavior.

## Shapes

Shared panels and incumbent climate/media summary actions use gently rounded corners (16 dp). Status pills use percentage capsules (`RoundedCornerShape(50)`). Buttons, chips, switches, sliders, outlined fields and dialogs retain Material shapes/states. Garage fields do not explicitly inherit the panel radius.

## Components

### Actions and navigation

Filled Material buttons serve explicit primary tasks (add, save, start), outlined buttons secondary/recovery/handoff actions, and text buttons quieter row actions. Fuel save shows “Saving…” while pending; errors appear in place. Deletion opens a Delete/Keep confirmation. External actions describe handoffs, not completed calls, shares or navigation. Icon-only actions carry spoken labels; decorative icons beside text avoid duplicate descriptions.

### Forms and records

`FormField` is a full-width labelled Material `OutlinedTextField`, optionally decimal-keyboard and multiline. Source limits values to 120 characters for single-line and 1,000 for multiline fields. Fuel labels include km, litres and ₹. Form errors use error color. Date/time selection displays local date/time above wrapping actions. Editors retain readable titles and explicit save/delete actions. Native focus/disabled/error styling comes from Material defaults.

### Panels, rows and empty states

`TonalPanel` groups a coherent summary/action: next reminder, recorder, fuel overview. `DetailRow` combines a primary vector icon, title, muted subtitle and optional trailing value; actionable rows add a chevron. `EmptyGarage` pairs a bookmark icon with title and next-step explanation, with 24 dp vertical padding. Keep grouped ownership lists instead of adding a card around every row.

### Choices and warning references

Warning switches name door, service, brake-pad and tyre display choices. Reminder lead times use filter chips. Warning sliders preserve standard adjustable range semantics and add distinct accessible names plus actual value/unit state: “Low-range reference” in kilometres (10–200 km), and “GPS speed reference” in kilometres per hour (30–120 km/h). A focused slider must distinguish purpose and unit; nearby text or a percentage alone is insufficient. Preferences do not replace vehicle warnings or create automatic safety alerts from uncertain signals.

### Journey and insight graphics

The route preview is a 180 dp Canvas with a primary path, neutral under-stroke and endpoint dots; gaps over 30 seconds break the line. Its semantic label says recorded GPS path preview, not a street map. The fuel bar chart is 100 dp high, showing up to 12 recent full-to-full intervals with a km/L caption. Head-unit EcoSense has its own labelled section and never invents a score from phone GPS. Unknowns remain unavailable.

### Private ownership and handoffs

Documents/manual, parking, assistance, records and privacy share the full-route hierarchy. Document links refer to provider files; removing a link does not delete originals. No proprietary manual is bundled. Assistance prepares a reviewed message using saved parking coordinates/time, not live tracking. Export copy names excluded identity/location fields and notes that titles/notes remain included. The deliberately started phone GPS foreground recorder remains separate from Bluetooth and head-unit EcoSense. UI availability does not establish real-car compatibility, production readiness or successful external delivery.

## Do's and Don'ts

### Do:
- Do preserve the real XUV500 cutout/platform and the four Material destinations.
- Do use Material roles, system type scaling, readable units and vector icons.
- Do let date/time actions and reminder chips wrap with complete labels.
- Do label phone estimates, manual records, simulation and head-unit data distinctly.
- Do preserve scrolling, native Back and visible recovery actions.

### Don't:
- Don't replace the established world or add vehicle imagery to every ownership form.
- Don't expose internal route keys or UUIDs as app-bar titles.
- Don't leave adjustable controls without focused purpose and value/unit descriptions.
- Don't imply Android handoffs prove completion.
- Don't claim synthetic/emulator evidence verifies physical-device or vehicle behavior.
