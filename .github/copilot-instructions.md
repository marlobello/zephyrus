# Copilot Instructions

## Project Overview

Zephyrus is a modern, simplistic Android native weather app. US-focused initially (with international expansion planned). Uses Open-Meteo APIs (free, no key required) for weather, UV, pollen, and geocoding data.

## Platform & Tooling

- **Platform:** Android native (Kotlin), min SDK 31 (Android 12), target SDK 36
- **Build system:** Gradle with Kotlin DSL + version catalog (`gradle/libs.versions.toml`)
- **UI:** Jetpack Compose with Material Design 3 and dynamic color (Material You)
- **Architecture:** MVVM — Compose UI → ViewModel (StateFlow) → Repository → Data Sources (Retrofit + Room)
- **Networking:** Retrofit + Kotlin Serialization + OkHttp (with logging interceptor)
- **Async:** Kotlin Coroutines + Flow
- **DI:** Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Storage:** Room (saved locations), DataStore Preferences (user settings)
- **Logging:** Timber (DebugTree in debug, OkHttp interceptor logs all API calls)
- **Location:** Google Play Services FusedLocationProviderClient

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew installDebug         # Build and install on connected device
```

## Architecture

```
app/src/main/java/com/zephyrus/app/
├── data/
│   ├── local/          # Room DB (SavedLocationEntity/Dao/ZephyrusDatabase), DataStore (UserPreferences)
│   ├── remote/         # Retrofit API services (WeatherApi, AirQualityApi, GeocodingApi), mappers
│   ├── model/          # API response DTOs (@Serializable)
│   └── repository/     # WeatherRepository, LocationRepository
├── domain/model/       # Domain models (CurrentWeather, DailyForecast, HourlyForecast, WeatherCondition, Location, TemperatureUnit)
├── ui/
│   ├── theme/          # MD3 theme (dynamic color), Color.kt, Type.kt
│   ├── navigation/     # Bottom nav (Current/Forecast/Maps), NavHost
│   ├── current/        # Current conditions screen + ViewModel
│   ├── forecast/       # 10-day forecast screen + ViewModel
│   ├── maps/           # Maps placeholder screen
│   ├── search/         # Location search + saved locations + ViewModel
│   └── components/     # Shared composables
├── di/                 # Hilt modules (NetworkModule, DatabaseModule, LocationModule)
├── util/               # WeatherIcons, AirportCodes, extensions
├── MainActivity.kt
└── ZephyrusApplication.kt
```

## Key Conventions

- **WMO weather codes** are mapped via `WeatherCondition.fromWmoCode()` — always use domain enum, never raw ints in UI.
- **Three named Retrofit instances** (`@Named("weather")`, `@Named("airQuality")`, `@Named("geocoding")`) for the three Open-Meteo endpoints.
- **Airport code search** uses a local lookup table (`AirportCodes.kt`) → city name → geocoding API, since Open-Meteo doesn't support IATA codes.
- **Temperature unit** (°F/°C) is persisted in DataStore and flows reactively via `UserPreferences.temperatureUnit`.
- **Result<T>** pattern is used in repositories for error handling — never throw from repository methods.
- **Timber logging** is used everywhere — tag with class context, log all state transitions and API calls.

## Weather Data

- Pull from publicly available US weather data via [Open-Meteo](https://open-meteo.com/) (free, no API key required).
- Weather: `api.open-meteo.com/v1/forecast`
- Air Quality/Pollen: `air-quality-api.open-meteo.com/v1/air-quality`
- Geocoding: `geocoding-api.open-meteo.com/v1/search`
- Do not commit API keys or secrets. Use `local.properties` or `BuildConfig` fields.

## Code Quality

- Evaluate every change holistically. New code must fit cleanly into the existing architecture — do not generate isolated snippets that ignore surrounding context.
- Avoid spaghetti code: keep clear separation between UI, domain, and data layers.
- Prefer small, single-responsibility classes and functions.
- Reuse existing utilities and components before creating new ones.
- **Vet all library versions, API endpoints, and SDK references against official documentation.** Do not guess or trust version numbers from unofficial sources. Always confirm from the library's official site, Maven Central, or the platform's release notes.

## Guidelines

- This project is MIT-licensed.
- Keep the UI minimal and clean, consistent with the "simplistic" design goal in the README.
