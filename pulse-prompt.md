# Pulse — Android App Build Prompt

Act as an autonomous Senior Android Developer and DevOps Engineer. Using your CLI and GitHub access, create, build, and deploy a complete, production-ready native Android application named "Pulse".

## 1. Project Overview & Tech Stack

* **App Name:** Pulse
* **Package Name:** `com.pulse.telemetry`
* **Language:** Kotlin (100%)
* **UI Framework:** Jetpack Compose with native Material 3 (`androidx.compose.material3`)
* **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles
* **Local Storage:** Room Database (SQLite) for historical telemetry logging + DataStore Preferences for saving user UI layout states
* **Background Processing:** WorkManager and Foreground Services for continuous, reliable metric logging
* **Build System:** Gradle (Kotlin DSL - `build.gradle.kts`)
* **Min SDK:** 26 (Android 8.0) | **Target/Compile SDK:** 34 (Android 14)

## 2. Design System: Native Material You (Material 3)

* Implement **Dynamic Color** (`dynamicDarkColorScheme` and `dynamicLightColorScheme`) so the app automatically adapts to the user's wallpaper palette on Android 12+, with a sleek Material 3 baseline fallback for older versions.
* Use authentic Material 3 components, typography, elevated/filled cards (`ElevatedCard`, `FilledTonalButton`), squircle/rounded corner shapes (`RoundedCornerShape(24.dp)`), and official Material Symbols/Icons.
* Include smooth spring animations (`animateContentSize`, `animateItemPlacement`) for all tile interactions.

## 3. Core UI/UX: Rearrangeable & Resizable Tile Dashboard

* **Grid Layout:** Build the home screen using a responsive 2-column grid (`LazyVerticalGrid` with `GridCells.Fixed(2)`).
* **Interactive Tiles:**
  * **Drag-and-Drop:** Users can long-press and drag any tile to rearrange its position on the dashboard grid.
  * **Resizable Span:** Users can double-tap or drag the edge/toggle of any tile to switch its size between **Compact Mode** (1x1 grid span - half width, showing core summary metrics) and **Expanded Mode** (2x1 full-width span - showing detailed charts, historical graphs, and app breakdowns).
  * **Persistence:** Save the exact order and size state of every tile in DataStore so the layout remains intact across app restarts.

## 4. Dashboard Telemetry & Modules (The Tiles)

Implement the following 6 core telemetry modules, logging data into the Room database so users can review historical trends over time:

1. **Battery & App Power Monitor:**
   * Real-time battery percentage, charging status, voltage, temperature (via `BatteryManager`), and estimated battery drain rate.
   * Per-app battery/foreground usage breakdown and historical discharge graph in Expanded Mode.
2. **Network Speed & Traffic Monitor:**
   * Live download/upload speed meter (in KB/s or MB/s) and total session/daily data consumed (Wi-Fi vs. Mobile Data) using `TrafficStats` and `NetworkCapabilities`.
   * Live latency ping indicator and real-time throughput sparkline chart in Expanded Mode.
3. **Digital Wellbeing & Screen Time:**
   * Total daily screen time, device unlock count, and a ranked list of most-used apps with exact usage durations (using `UsageStatsManager`).
   * Include an onboarding prompt/button to guide the user to grant `PACKAGE_USAGE_STATS` permission in Android Settings.
4. **System Task Manager (CPU & RAM):**
   * Live RAM usage (Used/Total MB and percentage via `ActivityManager.MemoryInfo`) and CPU load/frequency readings across cores.
   * Historical memory pressure graph and active process count in Expanded Mode.
5. **Storage Statistics:**
   * Internal storage breakdown (Used vs. Free GB, percentage bar) using `StorageStatsManager` / `StatFs`.
6. **Physical Activity & Device Event Logger:**
   * Chronological timeline logging device states (screen on/off events, charger connected/disconnected, network switches) alongside basic step/motion state tracking via `SensorManager` (Step Counter / Accelerometer).

## 5. Android Manifest & Permissions

* Declare all necessary permissions cleanly in `AndroidManifest.xml`: `PACKAGE_USAGE_STATS`, `ACCESS_NETWORK_STATE`, `INTERNET`, `FOREGROUND_SERVICE`, `ACTIVITY_RECOGNITION`, and `POST_NOTIFICATIONS`.
* Build a clean, non-intrusive permission request flow inside the app using Material 3 bottom sheets or cards to request runtime permissions and special system access (`ACTION_USAGE_ACCESS_SETTINGS`) without crashing if a permission is denied.

## 6. GitHub Repository & CI/CD Pipeline (GitHub Actions)

* Initialize a new GitHub repository named `Pulse-Android` (or use the current repository) and commit the complete Android project structure, including the Gradle Wrapper (`gradlew`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle-wrapper.properties`).
* Create a GitHub Actions workflow at `.github/workflows/android-build.yml`:
  * **Triggers:** `push` and `pull_request` on `main`/`master`, plus `workflow_dispatch`.
  * **Environment:** `ubuntu-latest` with `actions/setup-java@v4` (JDK 17, distribution `temurin`) and `gradle/actions/setup-gradle@v3`.
  * **Build Step:** Grant execute permission (`chmod +x gradlew`) and run `./gradlew assembleDebug`.
  * **Artifacts & Releases:** Use `actions/upload-artifact@v4` to upload the generated APK (`app/build/outputs/apk/debug/app-debug.apk`) named `Pulse-Debug-APK`, and automatically attach the compiled APK to a GitHub Release so it can be downloaded and installed directly on an Android device.

## 7. Execution Rules (No Placeholders)

* **Write 100% complete, compilable code.** Do not use `// TODO`, placeholder comments, or truncated files.
* Ensure all Gradle dependencies, Compose compiler versions, Kotlin versions, and Room KSP/KAPT plugins are compatible and resolve without build errors.
* After pushing the code and workflow to GitHub, monitor the GitHub Actions run. If the build fails, automatically inspect the action logs, fix the compilation or Gradle errors, commit the fix, and re-run until the APK compiles and uploads successfully.
