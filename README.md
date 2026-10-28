# BTranslator

An Android 14+ companion that converts a connected remote's left/right mouse movements into **actual touchscreen swipes**. Built for CheerPlay-style Bluetooth touchpad remotes; any compatible Android mouse/touchpad input can be selected.

## Install and use

1. Install the versioned APK from this repository's Releases. It is a sideload build, not a Play Store app.
2. Pair and **connect** your remote in Android Bluetooth settings. Use a remote mode that produces mouse/touchpad input.
3. Open BTranslator and choose **Enable swipe control**. Read the disclosure and enable BTranslator under Accessibility / Installed apps (labels vary).
4. If Android blocks a sideloaded service, open the app's App info → top-right menu → Allow restricted settings, then retry Accessibility.
5. Select your connected input, or use **Detect my remote** and move only that remote while the app is open.
6. Open **Test**, try **Send left/right swipe**, and confirm that the card changes. These buttons call Android's gesture API; they do not fake a successful result.
7. Tap **Start translation**, then use the remote in your target app. Pause briefly between strokes.

Pause from the app, the optional ongoing notification, or the BTranslator Quick Settings tile. Screen off, disconnect, service interruption/restart, or changing the selected input pauses translation. A gesture already in progress can finish after pausing.

## What to expect

- Android 14 / API 34 or newer is required for generic motion interception. No root or ADB is required on your phone.
- The app consumes mouse, relative-mouse, and touchpad sources only while you explicitly start translation. **Android intercepts these sources globally: other mice and trackpads stop working normally while active.** Only your selected device is translated. Pausing requests zero sources and restores normal input. Finger touch is not intercepted.
- Android may keep displaying its mouse cursor. Some devices expose relative motion; others report cursor coordinates that can clip at the screen edge. Relative axes are preferred, with coordinate differences as fallback.
- Horizontal scroll input is optionally translated too. Up/down translation is optional and off by default. Keyboard-only remote modes are unsupported.
- The default gesture travels across 60% of the screen at mid-height in 220 ms. Tune lets you adjust recognition threshold, stroke pause, repeat protection, direction reversal, distance, position, and duration.
- Apps decide what a touchscreen swipe does. Main-display use is supported; external displays are not supported in this version.

## Privacy and permissions

No internet, Bluetooth scan/connect, camera, microphone, or storage permissions. Android handles the Bluetooth connection. The only ordinary runtime permission is optional notifications for the Pause action.

The accessibility service declares `canPerformGestures=true` and `canRetrieveWindowContent=false`. It does not read screen text or take screenshots. Its runtime accessibility event mask is zero; only selected motion sources are requested while active. Motion descriptions and gesture counters stay in the process; preferences stay locally on the phone.

## Implementation

- Native Java `TranslatorService` uses Android's public `setMotionEventSources` / `onMotionEvent` and `dispatchGesture` APIs.
- `StrokeRecognizer` groups a burst into one stroke, requires a minimum distance and direction dominance, and suppresses accidental repeats.
- `SwipeGeometry` builds bounded interior paths in the available main-display rectangle.
- Native `MainActivity` serves bundled HTML/CSS/JS from a restricted HTTPS asset origin. The app has its own adaptive icon, real native startup splash, dark-first / saved light theme, diagnostics, and About.

Primary API references: [motion interception](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onMotionEvent(android.view.MotionEvent)), [source masks](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo#setMotionEventSources(int)), [touch injection](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#dispatchGesture(android.accessibilityservice.GestureDescription,%20android.accessibilityservice.AccessibilityService.GestureResultCallback,%20android.os.Handler)).

## Build and verify

JDK 17+ (tested with 21), Android SDK 35, Gradle 8.13 / AGP 8.9.2. Set `sdk.dir` in ignored `local.properties` if needed.

```
./gradlew testDebugUnitTest lintDebug assembleRelease
```

The universal APK contains no native libraries and supports Samsung ARM64 devices. Release signing uses the existing local Android debug key; preserve that certificate for future sideload updates. No keystore is committed. Package: `com.efremandrei.btranslator`, version 1.0.0 / build 1.

For Android integration checks, start a disposable API 34+ emulator, then run `python tools/virtual_mouse.py` while `./gradlew connectedDebugAndroidTest` runs. The helper creates an actual uinput mouse inside the emulator; Android tests temporarily enable the service and restore prior accessibility settings. Create `work/stop-mouse` or press Ctrl+C to remove the test mouse. This helper is **not part of the APK** and is never needed on the user's phone.

For browser UI regressions, install Playwright, serve `app/src/main/assets` on `http://127.0.0.1:8766`, and run `node tests/ui.cjs`. The browser suite uses an explicit native-bridge fixture; real Android input/touch behavior is tested separately. Optional environment overrides: `PLAYWRIGHT_MODULE`, `CHROME_EXECUTABLE`, `QA_OUTPUT`.

See [VERIFICATION.md](VERIFICATION.md) for tested coverage and physical-device limits.

## Author

Andrei Efremuahkin · andrei.efr@gmail.com

https://github.com/efremandrei/BTranslator
