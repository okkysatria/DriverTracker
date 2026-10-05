# Driver Tracker

[![Download Driver tracker](https://img.shields.io/badge/Download-Driver%20tracker-2ea44f?style=for-the-badge&logo=android)](https://github.com/okkysatria/DriverTracker/releases/latest/download/Driver-tracker.apk)

Driver Tracker is an Android app for recording driver orders, tracking trips, and reviewing income and route activity.

## Features

- Record passenger, food, and parcel orders with income, expenses, notes, and pickup or destination details.
- Track GPS location, distance, speed, and trip duration while an order is active.
- View order history and daily, weekly, and monthly income reports. Export reports as PDF or share a summary.
- View order locations and demand areas on an OpenStreetMap map.
- Create route posters as PNG images, including transparent backgrounds, and export routes as GPX.
- Configure tracking, notifications, map overlays, and app appearance in Settings.

## Technology

- Kotlin and Jetpack Compose with Material 3
- Navigation 3 and Lifecycle ViewModel
- Room for saved orders and DataStore for preferences
- Android foreground location service with Google Play services location APIs
- osmdroid and OpenStreetMap tiles for maps
- H3 spatial indexing and optional ONNX Runtime model support for hotspot scoring

## Project Structure

```text
app/src/main/java/com/example/drivertracker/
├── data/          Room database, entities, preferences, and tracking state
├── ml/            Hotspot scoring and ONNX model support
├── service/       Foreground GPS tracking and notification actions
└── ui/
    ├── components/ Reusable dialogs, map views, and map pins
    ├── navigation/ App destinations and bottom navigation
    └── screens/    Recorder, radar, route poster, reports, and settings
```

## Requirements

- Android Studio with Android SDK Platform 37
- JDK 17 or a version supported by the configured Android Gradle Plugin
- Android device or emulator running Android 8.1 (API 27) or later
- Internet access for map tiles and location permission for GPS features

The map uses the public OpenStreetMap tile service. Keep the map attribution visible and follow the tile usage policy. Use a tile provider suited to your deployment if you expect substantial traffic.

