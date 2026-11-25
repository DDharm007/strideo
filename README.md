# STRIDEO
### Every step leaves a mark.

<p align="center">
  <strong>A minimalist fitness companion built for people who refuse to stand still.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-black?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-Native-purple?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/Storage-Room_SQLite-black?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Maps-OpenStreetMap-green?style=for-the-badge&logo=openstreetmap" />
</p>

---

## The philosophy

Fitness tracking shouldn't feel like managing spreadsheets.

**Strideo** is a minimalist health and activity tracking application designed to make movement measurable, progress visible, and consistency part of your everyday life.

Whether you're running through city streets, cycling across new routes, or simply working towards a healthier lifestyle, Strideo brings your activity data into one focused experience.

No unnecessary noise. Just movement, metrics, and momentum.

## What makes Strideo different?

- **GPS Activity Tracking** — Track outdoor movement using your device's location capabilities.
- **Interactive Maps** — Explore routes with OpenStreetMap-powered mapping and different map styles.
- **Performance Analytics** — Understand your activity and monitor your performance over time.
- **Health Metrics** — Keep important wellness and fitness information accessible.
- **Activity History** — Maintain a record of your tracked sessions.
- **Personal Goals & Streaks** — Build consistency through progress tracking.
- **Home Screen Widgets** — Access your daily activity summary and quick GPS tracking directly from your Android home screen.
- **Local-First Storage** — Store application data using Room and SQLite.

## Built with purpose

| Technology | Purpose |
|---|---|
| Kotlin | Core application development |
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

Strideo is developed as a native Android application with a local data layer and location-aware functionality.

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
   └── Background & System Integration
          └── Android Home Screen Widgets
```

## Getting started

### Prerequisites

- Android Studio
- JDK 11 or compatible configured Gradle runtime
- Android SDK 36
- Android device or emulator (API 24+)

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/DDharm007/strideo.git
```

**2. Open the project**

Open the cloned `strideo` folder in Android Studio.

**3. Sync Gradle**

Allow Android Studio to download and configure the required dependencies.

**4. Configure environment**

The project includes an `.env.example` file. Copy it to `.env` if your local setup requires it.

No external API keys are required for the currently documented local-first map and storage configuration.

**5. Run the application**

Select an Android device or emulator and click Run.

## Permissions

Strideo uses Android permissions for:

- Internet access for map tiles and network resources.
- Network state access.
- Coarse location.
- Fine location.

Location-dependent functionality requires the appropriate permissions from the user.

## Project structure

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
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── metadata.json
└── .env.example
```

## Design philosophy

Strideo follows a minimalist, monochromatic visual direction.

- Clarity over clutter.
- Function over decoration.
- Meaningful metrics over vanity numbers.
- Consistency over temporary motivation.

The goal is simple: create a fitness experience that feels as intentional as the lifestyle it encourages.

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

I build software at the intersection of engineering, design, and practical utility.

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
</p>
