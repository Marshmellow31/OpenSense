# Emulator screenshots · 27 October 2026

Actual application UI captured using the existing `UiCaptureTest` instrumentation
test on the `OpenSenseUI` Android emulator. The test passed at 1080 × 2400,
420 dpi, fontScale 1.0. App source: `6cc73d565f88f32136a3a0c5b0d3a97b1ccdffb2`.
The app and test APKs were assembled from that source without application changes.

These screenshots show an offline app or its built-in simulation, never a
connected physical vehicle. No Bluetooth, HVAC, head-unit acceptance or real
playback result is established. Simulation labels are preserved in the images.
The earlier compact 840 × 1680 / 1.3× text capture set was excluded.

| Capture | State |
| --- | --- |
| [Vehicle, offline](vehicle-offline-dark.webp) | No live readings |
| [Vehicle, dark](vehicle-demo-dark.webp) | Built-in simulation |
| [Climate, dark](climate-dark.webp) | Built-in simulation |
| [Media, dark](media-dark.webp) | Simulated playback paused |
| [Insights, dark](insights-dark.webp) | Built-in simulation |
| [Settings](settings-dark.webp) | Emulator configuration screen |
| [Vehicle, light](vehicle-light.webp) | Built-in simulation |
| [Climate, light](climate-light.webp) | Built-in simulation |

## Reproduce

With the Android SDK and JDK configured, use a dedicated emulator:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain
adb -e install -r app/build/outputs/apk/debug/app-debug.apk
adb -e install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -e shell wm size 1080x2400
adb -e shell settings put system font_scale 1.0
adb -e shell am instrument -w -e class org.opensense.app.UiCaptureTest -e capturePrefix portfolio-normal-20261027 org.opensense.app.test/androidx.test.runner.AndroidJUnitRunner
```

The test writes original PNG captures under the app's external files directory,
`ui-review/`. Selected captures were converted to WebP with Sharp, quality 90,
effort 6, with dimensions unchanged. Only WebP assets are published here.
