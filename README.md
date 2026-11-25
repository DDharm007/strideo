# STRIDEO
### Every step leaves a mark.

<p align="center">
  <strong>A minimalist fitness companion built for people who refuse to stand still.</strong>
</p>

<p align="center">
  <a href="https://github.com/DDharm007/strideo/raw/refs/heads/main/apk/Strideo.apk">
    <img src="https://img.shields.io/badge/⬇_DOWNLOAD_STRIDEO-ANDROID_APK-black?style=for-the-badge&logo=android" alt="Download Strideo APK" />
  </a>
</p>

<p align="center">
  <strong>Get Strideo. Start moving. Leave your mark.</strong>
  <br/>
  <sub>Direct Android APK Download · Free Installation</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-black?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-Native-purple?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/Storage-Room_SQLite-black?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Maps-OpenStreetMap-green?style=for-the-badge&logo=openstreetmap" />
</p>

---

## What is Strideo?

**Strideo** is a minimalist fitness and activity tracking application designed for people who want to stay active, understand their performance, and build a consistent lifestyle.

It transforms your Android device into a personal fitness companion, bringing outdoor activity tracking, GPS-powered routes, performance analytics, health metrics, and activity history together in one focused experience.

Whether you're running through city streets, cycling across unfamiliar routes, or simply trying to become more physically active, Strideo helps you understand your movement and keep track of your progress.

The philosophy is simple:

**Move with intention. Track your progress. Keep going.**

### Built for everyday movement

- **GPS Activity Tracking** — Record outdoor movement using your device's location capabilities.
- **Interactive Maps** — Visualize and explore your routes using OpenStreetMap.
- **Performance Analytics** — Review activity metrics and understand your performance over time.
- **Health Metrics** — Keep important fitness and wellness information accessible.
- **Activity History** — Maintain a record of your tracked sessions.
- **Personal Goals & Streaks** — Build consistency and monitor your progress towards personal goals.
- **Home Screen Widgets** — Access daily activity summaries and quick GPS tracking directly from your Android home screen.
- **Local-First Storage** — Keep application data stored locally using Room and SQLite.

## Why Strideo?

Fitness shouldn't feel like managing spreadsheets.

Strideo follows a different approach: a clean, monochromatic interface that prioritizes useful information over unnecessary decoration.

It focuses on three principles:

1. **Clarity over clutter** — A minimal interface that keeps your attention on movement.
2. **Progress over vanity** — Meaningful activity information instead of meaningless numbers.
3. **Consistency over motivation** — Build habits through measurable progress and personal goals.

Every activity contributes to a bigger picture of your fitness journey.

---

## Download & Installation

<text>Download the latest Android APK directly from GitHub.</text>

<p align="center">
  <a href="https://github.com/DDharm007/strideo/raw/refs/heads/main/apk/Strideo.apk">
    <img src="https://img.shields.io/badge/DOWNLOAD-STRIDEO.APK-black?style=for-the-badge&logo=android" alt="Download Strideo" />
  </a>
</p>

**Installation steps**

1. Tap the Download Strideo APK button.
2. Your Android device will download `Strideo.apk`.
3. Open the downloaded file from your Downloads folder.
4. Allow installation from this source if Android requests permission.
5. Complete the installation and launch Strideo.

**Direct download link:** [Strideo.apk](https://github.com/DDharm007/strideo/raw/refs/heads/main/apk/Strideo.apk)

---

## Built with Purpose

| Technology | Purpose |
|---|---|
| Kotlin | Native Android development |
| Jetpack Compose | Modern declarative UI |
| Material 3 | UI components |
| Room / SQLite | Local database |
| OSMDroid | Interactive map rendering |
| Google Play Services Location | GPS and location tracking |
| Kotlin Coroutines | Asynchronous operations |
| Navigation Compose | Application navigation |
| Coil | Image loading |
| Gradle Kotlin DSL | Build configuration |

## Architecture

Strideo follows a native Android architecture with dedicated layers for user interface, location services, mapping, and local data management.

```text
Strideo
   |
   ├── Android Application
   |      ├── Kotlin
   |      └── Jetpack Compose
   |
   ├── Location Layer
   |      └── Google Play Services Location
   |
   ├── Mapping Layer
   |      └── OSMDroid
   |
   ├── Data Layer
   |      └── Room Database
   |             └── SQLite
   |
   └── System Integration
          └── Android Home Screen Widgets
```

## Getting Started

### Prerequisites

- Android Studio
- JDK 11 or compatible configured Gradle runtime
- Android SDK 36
- Android device or emulator (API 24+)

### Run Locally

**1. Clone the repository**

```bash
git clone https://github.com/DDharm007/strideo.git
```

**2. Open the project**

Open the cloned `strideo` directory in Android Studio.

**3. Sync Gradle**

Allow Android Studio to download and configure the required dependencies.

**4. Configure the environment**

The project includes an `.env.example` file. Copy it to `.env` if your local setup requires it.

**5. Run the application**

Select an Android device or emulator and click Run.

## Permissions

Strideo uses Android permissions for:

- Internet access for map tiles and network resources.
- Network state access.
- Coarse location.
- Fine location.

Location-dependent features require the appropriate permissions from the user.

## Project Structure

```text
strideo/
│
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           ├── res/
│           └── AndroidManifest.xml
│
├── apk/
│   └── Strideo.apk
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── metadata.json
└── .env.example
```

## Roadmap

- [ ] Expand running and cycling analytics.
- [ ] Improve historical activity insights.
- [ ] Introduce more personalized fitness goals.
- [ ] Enhance route visualization.
- [ ] Expand wellness tracking.
- [ ] Improve long-term performance statistics.

*The roadmap represents development directions, not necessarily currently implemented features.*

## Contributing

Contributions, suggestions, bug reports, and feature ideas are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a pull request.

For major changes, open an issue first to discuss your proposal.

## Developer

**Dharm Patel**

Founder of Craftorā · Full-Stack Developer · Product Builder

Building software at the intersection of engineering, design, and practical utility.

<p align="center">
  <a href="https://github.com/DDharm007">GitHub</a> ·
  <a href="https://craftoraa.com">Craftorā</a>
</p>

---

<p align="center">
  <strong>STRIDEO</strong>
  <br/>
  Move with intention. Track your progress. Keep going.
  <br/><br/>
  <i>Built for the ones who keep moving.</i>
  <br/><br/>
  Every step leaves a mark.
</p>
