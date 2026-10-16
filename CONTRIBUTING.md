# Contributing to OpenSense

Inspect the current implementation and research evidence before changing behavior. Keep contributions focused and preserve unrelated work. Describe what the user will observe, how you checked it, and what remains unverified.

## Evidence and compatibility

Use explicit claim labels: **verified**, **historical**, **needs-confirmation**, or **recommendation-not-fact**. Identify the scope of every verification: static source inspection, unit test, emulator, physical phone, or stationary vehicle. A Bluetooth service UUID, a successful build, or a simulated dashboard is not vehicle compatibility evidence.

Vehicle profiles describe research candidates. Variant labels and model years do not guarantee a particular head unit. Supply the app family and a non-identifying description of the installed head unit when reporting compatibility.

The current app has no vehicle socket or control path. Protocol integration requires a separate, reviewable change with source-derived framing, malformed-input handling, reproducible fixtures, and owner-authorized stationary vehicle evidence. Never silently replace the synthetic codec with a presumed vehicle protocol. Do not implement vehicle controls from guessed command bytes.

## Before submitting

1. Read [architecture](docs/architecture.md) and [testing](docs/testing.md).
2. Run the core unit tests, Android lint, debug build, and static-inspector tests described there.
3. For UI changes, check profile selection, narrow screens, large text, system insets, navigation, theme changes, and foreground/background transitions. State which checks actually ran.
4. Inspect your diff for credentials, device identifiers, VINs, location history, raw logs, original APKs, decompiled code, copied branding, and unrelated changes. Submit independently authored source and sanitized evidence only.
5. Explain limitations and give exact reproduction steps. Include commit/build identity when available; never invent one for an uncommitted workspace.

Keep dependency changes explicit and reproducible. Avoid unnecessary new permissions, network services, or persistent diagnostic storage. Do not submit generated build directories, private analysis outputs, local SDK paths, signing keys, or APK artifacts.

## Suggested contribution areas

- Improve accessibility and test coverage around vehicle selection and error recovery.
- Add synthetic parser edge cases and bounded-stream tests.
- Review sanitized research notes with clear source and confidence labels.
- Report actual phone behavior with Android version and permission state.

License your original contributions under the repository's MIT license and confirm that any included third-party material is appropriate to redistribute. The project cannot make a blanket legal determination for research sources or uses.
