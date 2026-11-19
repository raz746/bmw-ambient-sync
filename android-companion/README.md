# Android companion v0.1 — Pandora capture test

Native Android/Kotlin proof of concept for the Samsung Galaxy S26. Requests the user's permission for Android MediaProjection playback capture, then displays an RMS audio-level meter. **It is not yet a bass/beat detector** and does not connect to the car. The app does not save or transmit audio.

## Open on Windows
1. Install Android Studio with Android SDK 35 and JDK 17 (Android Studio's embedded JDK works).
2. In Android Studio choose **Open**, select the `android-companion` folder, and allow Gradle sync. Android Studio may ask to install missing SDK packages.
3. Connect the S26 by USB with developer options/USB debugging enabled, or build an APK with **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Launch Ambient Sync, start Pandora, tap **Test playback capture**, and grant the Android system prompt.
5. Watch the meter. Tap **Stop test** to stop capture. Test while parked, not while driving.

## Limitations
- Playback capture is only possible if Pandora permits it and its playback is routed through a capturable Android mix. Android Auto may route audio differently.
- A zero meter does **not** prove that no audio exists; it can indicate a blocked or incompatible audio path.
- The meter measures overall RMS loudness, not bass frequency or beat timing.
- This prototype has not been built or tested on a Galaxy S26 yet. Expect device-specific adjustments.
- This is a local foreground service with a visible notification; no raw audio is stored or uploaded.
- The project intentionally does not contain BMW vehicle control code.

## Next
Confirm whether Pandora capture produces nonzero levels, then add an FFT-based bass-energy detector and phone-to-iDrive transport.
