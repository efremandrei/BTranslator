# BTranslator 1.0.0 / build 1

Artifact: `BTranslator-v1.0.0-build-1-universal.apk`

- Package: `com.efremandrei.btranslator`.
- Minimum Android 14 / API 34; target and compile SDK 35.
- Universal Java/web APK with no native libraries; compatible with Samsung ARM64 devices running Android 14 or newer.
- APK signature verification passed. Certificate SHA-256: `6afe635f1eeb6ae2def13bfdd671fee6e1103cb9ca09113d4b1826de433a092d` (existing local Android debug certificate, for sideload installation).

## Checks completed

- `testDebugUnitTest`: **7 tests passed**. Distance / direction thresholds, diagonal/noise rejection, one output per stroke, cooldown, reversal, invalid input, and bounded swipe geometry.
- `lintDebug assembleRelease`: passed with zero lint errors. Advisory warnings concern target SDK 35 rather than 36, intentional JavaScript for bundled local content, the platform backup attribute, and a service reference that is explicitly cleared in `onDestroy`. Backup/data-extraction rules explicitly exclude app data.
- Browser UI suite: setup gating, escaping remote names, start/pause controls, preference and theme persistence, native test-coordinate requests, no fake completed gestures, browser touch handling, About, reduced motion, help, and **12 layouts** across 320 / 412 / 800 px widths. No external requests or script errors. This suite uses a declared Android bridge fixture and does not claim hardware validation.
- Android 15 / API 35 emulator: the full **3-test integration suite passed**. A real Android `uinput` mouse generated alternating motion bursts through the Android input pipeline; BTranslator converted them into left and right **SOURCE_TOUCHSCREEN** strokes received by a separate debug-only native target Activity. After pausing, further mouse bursts generated no new touch strokes and the requested source mask was zero.
- The same emulator suite verified that the production WebView **Send left/right swipe** buttons reach the actual HTML touch handler through the native bridge and Android gesture dispatcher. It also verified the Pause receiver and screen-off pausing / restoration of pointer input. Original accessibility settings were restored afterward.
- After startup-only changes, the production WebView / real-touch integration test was rerun and passed.
- The APK installed on the emulator. Its packaged custom icon was displayed in Android App info. Native branded startup and the usable interface were inspected. Dark-first / saved-light theme behavior is implemented, including persisting the native splash theme.

The debug target Activity and emulator mouse helper are not included in the release APK. The helper requires an emulator shell only for testing; the phone app requires neither root nor ADB.

## Limits

No physical Samsung or CheerPlay was available. Real Bluetooth pairing, the exact signals / modes of the user's unit, pointer-edge behavior, and target apps on that phone remain unverified. The in-app source list, Detect my remote, live motion deltas, delivered/cancelled counts, and real-touch test pad support that check.

Android consumes all requested mouse / touchpad source types while translation is active. Only the selected device is translated, so other mice/trackpads cannot be passed through in that mode. Finger touch remains available. Android may still display a mouse cursor. Apps decide what the generated swipes do.
