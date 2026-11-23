# NovaLogistik Mobile

Native Android app for NovaLogistik drivers, built with Kotlin and Jetpack Compose. Drivers can sign in, view assigned transports and drops, update transport status, capture delivery evidence, record expenses, and review their history and profile.

## Requirements

- Android Studio with Android SDK 37 and a device or emulator running Android 8.0 (API 26) or later.
- JDK 21 for the Gradle daemon. The project includes the Gradle wrapper; Android Studio can configure the SDK location in `local.properties`.

## Run

Open this directory in Android Studio, allow Gradle to sync, and run the `app` configuration on a device or emulator. From a terminal on Windows, build a debug APK with:

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`. On macOS or Linux use `./gradlew` instead.

## Backend

The app defaults to `https://api.logistik.kaminova.id/api/v1/`, configured in `app/src/main/java/com/kaminova/novalogistikmobile/data/api/NetworkModule.kt`. A reachable backend and driver credentials are required. The app requests camera and location permissions for evidence capture and location features.

## Tests

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

The second command requires a connected Android device or running emulator. The Plus Jakarta Sans font is distributed under the license in `PlusJakartaSans-OFL.txt`.
