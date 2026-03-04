# Copilot Instructions

## Project Overview

Zephyrus is a modern, simplistic Android native weather app. US-focused initially (with international expansion planned). Uses Open-Meteo APIs (free, no key required) for weather, UV, pollen, and geocoding data.

## Platform & Tooling

- **Platform:** Android native (Kotlin), min SDK 31 (Android 12), target SDK 36
- **Build system:** Gradle 9.3.1 with Kotlin DSL + version catalog (`gradle/libs.versions.toml`)
- **AGP:** 9.0.1 — Kotlin is built into AGP 9.0+, do NOT apply `org.jetbrains.kotlin.android` plugin separately
- **UI:** Jetpack Compose with Material Design 3 and dynamic color (Material You)
- **Architecture:** MVVM — Compose UI → ViewModel (StateFlow) → Repository → Data Sources (Retrofit + Room)
- **Networking:** Retrofit 3.0 + Kotlin Serialization + OkHttp (shared client with timeouts)
- **Async:** Kotlin Coroutines + Flow
- **DI:** Hilt 2.59.2 (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Storage:** Room 2.8.4 (saved locations), DataStore Preferences (user settings)
- **Logging:** Timber (DebugTree in debug)
- **Location:** Google Play Services FusedLocationProviderClient
- **Maps:** osmdroid 6.1.20 for map rendering with custom weather overlays
- **Compose BOM:** 2026.02.01

## Build Commands

Build requires explicit JAVA_HOME and ANDROID_HOME on this machine:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\marlobell\AppData\Local\Android\Sdk"
.\gradlew.bat assembleDebug --no-daemon
```

Or simplified:
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
│   ├── remote/         # Retrofit API services (WeatherApi, AirQualityApi, GeocodingApi), mappers, withRetry()
│   ├── model/          # API response DTOs (@Serializable)
│   └── repository/     # WeatherRepository, LocationRepository
├── domain/model/       # Domain models (CurrentWeather, DailyForecast, HourlyForecast, WeatherCondition, Location, TemperatureUnit)
├── ui/
│   ├── theme/          # MD3 theme (dynamic color), Color.kt, Type.kt, TemperatureColors.kt (shared color scales)
│   ├── navigation/     # Bottom nav (Current/Forecast/Maps), NavHost with shared location state
│   ├── current/        # Current conditions screen + ViewModel
│   ├── forecast/       # 10-day forecast screen + ViewModel
│   ├── maps/           # Weather map with overlays (MapsScreen, MapsViewModel, MapLayer, MapsUiState)
│   ├── search/         # Location search + saved locations + ViewModel
│   └── components/     # Shared composables (ZephyrusTopAppBar, HourlyForecastRow)
├── di/                 # Hilt modules (NetworkModule, DatabaseModule, LocationModule)
├── util/               # WeatherIcons, AirportCodes, extensions
├── MainActivity.kt
└── ZephyrusApplication.kt
```

## Key Conventions

- **WMO weather codes** are mapped via `WeatherCondition.fromWmoCode()` — always use domain enum, never raw ints in UI.
- **Three named Retrofit instances** (`@Named("weather")`, `@Named("airQuality")`, `@Named("geocoding")`) for the three Open-Meteo endpoints, all sharing a single `OkHttpClient` with 15s timeouts.
- **Airport code search** uses a local lookup table (`AirportCodes.kt`) → city name → geocoding API, since Open-Meteo doesn't support IATA codes.
- **Temperature unit** (°F/°C) is persisted in DataStore and flows reactively via `UserPreferences.temperatureUnit`.
- **Result<T>** pattern is used in repositories for error handling — never throw from repository methods.
- **Timber logging** is used everywhere — tag with class context, log all state transitions and API calls.
- **Shared location state** lives at the NavHost level (`rememberSaveable` for lat, lon, name) — all screens receive location via parameters, not independently.
- **Exponential backoff retry** — all API calls use `withRetry()` (coroutine-based, in `RetryInterceptor.kt`) with configurable max retries, initial delay, and max delay. Never use `Thread.sleep()` in interceptors.
- **Grid weather data** — the Maps screen fetches an 8×8 grid of weather data using `WeatherRepository.getGridWeatherData()` with `Semaphore(8)` for concurrency limiting. Grid point data includes temperature, humidity, pressure, and precipitation.

## Maps Implementation

- **osmdroid** is used for map rendering (not Google Maps) — no API key required.
- `GroundOverlay.setPosition()` takes two `GeoPoint`s (SW corner, NE corner), NOT a `BoundingBox`.
- The bitmap origin (top-left pixel) maps to the **SW corner** geographically — grid data (row 0 = north) must be vertically flipped when drawing.
- `Configuration.getInstance().userAgentValue` must be set before using map tiles.
- MapView requires proper lifecycle management: call `onResume()`, `onPause()`, `onDetach()` via `DisposableEffect`.
- Old overlay bitmaps must be recycled before removing overlays to prevent memory leaks.
- Use `clipToBounds()` modifier on the map container to prevent the map from overflowing its layout bounds when zooming.
- `DelayedMapListener` wraps `MapListener` with a debounce delay — use for viewport change detection.
- Dynamic viewport: read `map.boundingBox` for actual visible area rather than calculating from zoom level formulas.
- `setMinZoomLevel()` / `setMaxZoomLevel()` enforce zoom constraints.

## Color Scales (TemperatureColors.kt)

Shared color utilities used by both the Forecast screen and Maps overlays:
- **Temperature:** 7-stop scale 0°F (deep blue) → 105°F (deep red)
- **Humidity:** 5-stop scale 0% (tan) → 100% (deep blue)
- **Pressure:** 5-stop scale 980 hPa (purple) → 1040 hPa (red)
- **Precipitation:** 5-stop scale 0 in/h (light green) → 2+ in/h (purple)

Use `interpolateColorStops()` for smooth gradients. Never use transparent (alpha=0) as the base color for overlays — it makes the entire overlay invisible.

## Weather Data

- Pull from publicly available US weather data via [Open-Meteo](https://open-meteo.com/) (free, no API key required).
- Weather: `api.open-meteo.com/v1/forecast`
- Air Quality/Pollen: `air-quality-api.open-meteo.com/v1/air-quality`
- Geocoding: `geocoding-api.open-meteo.com/v1/search`
- Open-Meteo can rate-limit burst requests — limit concurrency (Semaphore) and use exponential backoff.
- Do not commit API keys or secrets. Use `local.properties` or `BuildConfig` fields.

## Code Quality

- Evaluate every change holistically. New code must fit cleanly into the existing architecture — do not generate isolated snippets that ignore surrounding context.
- Avoid spaghetti code: keep clear separation between UI, domain, and data layers.
- Prefer small, single-responsibility classes and functions.
- Reuse existing utilities and components before creating new ones (e.g., `TemperatureColors.kt` is shared between Forecast and Maps).
- **Vet all library versions, API endpoints, and SDK references against official documentation.** Do not guess or trust version numbers from unofficial sources. Always confirm from the library's official site, Maven Central, or the platform's release notes.
- Use the latest stable APIs — avoid deprecated composables (e.g., use `SearchBar` with `inputField` slot, use `hiltViewModel` from `androidx.hilt.lifecycle.viewmodel.compose`).
- Always run a clean build and fix all warnings before committing.
- Recycle bitmaps and manage resources properly — Android is memory-constrained.

## App Branding

- Logo source is `zephyrus.svg` in the repo root — it includes its own background and padding.
- When regenerating icons, use the SVG as-is at full canvas size (no additional scaling or background).
- Adaptive icon XML uses the foreground image for BOTH `android:foreground` and `android:background` layers since the SVG includes its own background.
- Node.js with `sharp` library is available for image processing (installed in repo root).

## Guidelines

- This project is MIT-licensed.
- Keep the UI minimal and clean, consistent with the "simplistic" design goal in the README.
- When adding new data overlays or features, follow the existing pattern: add to data model → repository → ViewModel → UI state → screen composable.
- Prefer dropdown menus over chip rows when there are 3+ selectable options to save screen space.
- Hourly forecast should always show at least 8 hours, fetching next-day data if needed.