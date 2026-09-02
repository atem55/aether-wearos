# Poweratti for Wear OS

Native Wear OS app for tracking LARP power pools on a Galaxy Watch Ultra (and any Wear OS 3+ watch).

Standalone — no phone companion required. Pools are saved on the watch with DataStore.

Source: [github.com/atem55/aether-wearos](https://github.com/atem55/aether-wearos)

## What it does

- Add up to 10 named pools (Mana, Spirits, EP, Blood, Primal, Ring, or a custom name)
- Pick a chip colour and white or black text
- Set a maximum (1–999)
- **Start full?** — on starts at max, off starts at 0
- Optional regen: amount + interval in minutes (e.g. 1 every 5 minutes)
- Regen never goes above max; the timer only runs while you are below max
- Missed ticks catch up if the app was in the background
- Main list has + / − on each pool, plus a compact **Add pool** button
- Regenerating pools have a radio on the right — only the selected one counts down; the others pause
- The armed pool has a pause / play control in its chip so you can freeze **all** regen (lunch, etc.)
- A regenerating pool gaining power fires a short vibration
- A regenerating pool hitting 0 fires a strong vibration
- Trash a pool, or clear all

## Install on your Galaxy Watch Ultra

You need [Android Studio](https://developer.android.com/studio) on a computer (the Wear OS SDK comes with it).

1. Open Android Studio → **Open** this folder.
2. Let Gradle sync. First sync downloads Wear OS libraries.
3. On the watch: **Settings → About watch → Software** and tap the version 7 times to unlock developer options.
4. **Settings → Developer options** → turn on **ADB debugging** and **Debug over Wi‑Fi**.
5. Pair the watch in Android Studio’s device dropdown (or use the Wear OS pairing assistant).
6. Click **Run**. Poweratti installs and opens on the watch.

### Sideload the debug APK

If you already have `app-debug.apk` (included in the project zip):

1. Enable ADB debugging over Wi‑Fi on the watch.
2. From a computer with [platform-tools](https://developer.android.com/tools/releases/platform-tools):

```
adb connect WATCH_IP:5555
adb install -r app-debug.apk
adb shell am start -n app.aether.wear/.presentation.MainActivity
```

The app is signed with a debug key so you can sideload it. For Play Store you’d generate your own release keystore.

## Package

- Application ID: `app.aether.wear`
- minSdk 30 (Wear OS 3 / Galaxy Watch 4 and newer, including Watch Ultra)
- compileSdk / targetSdk 35
- Kotlin 2.0, Jetpack Compose for Wear OS (Material 1.4)
