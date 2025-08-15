# AMG Quiz App (Android)

**What it is:** A Jetpack Compose quiz app that loads 45 random questions each session from your provided DOCX, with answer key and scoring.

## Build (Android Studio)
1. Open Android Studio → *Open* → select this folder.
2. Let it sync (it will download Gradle dependencies).
3. Run: **Build → Build APK(s)** (or click the Run ▶ button to install on a device/emulator).

The debug APK will be at: `app/build/outputs/apk/debug/app-debug.apk`.

## Notes
- JSON with answers: `app/src/main/assets/questions_amg_pitesti_2025_with_answers.json`
- Min SDK 24, Target SDK 34.
