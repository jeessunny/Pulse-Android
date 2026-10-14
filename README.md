# ⚡ Pulse — Real-Time Android System Telemetry

**Pulse** is a native, modern Android telemetry and system diagnostic application built 100% in Kotlin using Jetpack Compose and native Material 3 (Material You).

---

## 📱 Features & Telemetry Modules

Pulse organizes device telemetry into an intuitive, rearrangeable 2-column dashboard:

### 1. 🔋 Battery & App Power Monitor
- Real-time battery percentage, charging state, voltage, temperature, and battery health.
- Estimated battery drain rate (% per hour) based on historical samples.
- **Expanded Mode:** Historical battery discharge sparkline chart + per-app foreground power usage breakdown.

### 2. 📶 Network Speed & Traffic Monitor
- Live upload and download speed meters (B/s, KB/s, MB/s).
- Session and daily data consumption (Wi-Fi vs. Mobile Data).
- Real-time latency ping indicator (ms).
- **Expanded Mode:** Real-time network throughput history sparkline chart.

### 3. ⏳ Digital Wellbeing & Screen Time
- Total daily screen time and device unlock counter.
- Ranked list of most-used apps with usage duration.
- Clean onboarding prompt to grant `PACKAGE_USAGE_STATS` access without crashing.

### 4. 🧠 System Task Manager (CPU & RAM)
- Real-time RAM memory gauge (Used vs. Available GB and percentage).
- Multi-core CPU load and active running process counter.
- **Expanded Mode:** Historical memory pressure graph.

### 5. 💾 Storage Statistics
- Internal storage capacity breakdown (Used vs. Free space).
- Visual capacity bar and formatted gigabytes overview.

### 6. 🏃 Activity & Device Event Logger
- Chronological timeline logging system events (screen on/off, charger connected/disconnected, network switches).
- Step counter and motion state tracking (Stationary, Walking, Active).

---

## 🎨 UI/UX: Interactive Dashboard Tiles
- **2-Column Responsive Grid:** Built using `LazyVerticalGrid` with `GridCells.Fixed(2)`.
- **Resizable Spans:** Every tile can be toggled between **Compact Mode** (1x1 span) and **Expanded Mode** (2x1 full-width span) via double-tap or header resize toggle.
- **Rearrangeable Order:** Tiles can be moved up or down with immediate visual feedback.
- **Persistence:** Layout order and tile expansion states are automatically persisted across restarts using **Jetpack DataStore**.
- **Material You Design:** Native Material 3 with dynamic wallpaper color adaptation (`dynamicDarkColorScheme` / `dynamicLightColorScheme`).

---

## 🛠 Tech Stack & Architecture
- **Language:** Kotlin 100%
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture
- **UI Framework:** Jetpack Compose + Material 3 (`androidx.compose.material3`)
- **Database:** Room SQLite Database for telemetry snapshots and event logging
- **Layout Storage:** Jetpack Preferences DataStore
- **Background Processing:** Android Foreground Service with live notification + WorkManager daily cleanup
- **Build System:** Gradle (Kotlin DSL) | Min SDK 26 | Target SDK 34

---

## 🚀 Building & Installing

### Automated GitHub Actions CI/CD
Every push to `main` triggers `.github/workflows/android-build.yml`, which compiles the debug APK and publishes it as a downloadable release artifact named `Pulse-Debug-APK`.

### Local Build via Command Line
```bash
./gradlew assembleDebug
```
The compiled APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.
