# Android runtime dependencies

DriveFly Mobile retains DriveLink Mobile's MIT LICENSE.

The APK uses AndroidX / Jetpack Compose (The Android Open Source Project), Kotlin and kotlinx.coroutines (JetBrains and contributors), and Gson (Google and contributors), under Apache License 2.0. The full license is in licenses/Apache-2.0.txt. Dependencies are resolved from Google's Maven repository and Maven Central by the Gradle project.

JUnit and Android test libraries are development dependencies; they are not part of the application APK. Their upstream licenses apply when those libraries are downloaded for building or testing.

Flight-control use is inspired by virtualHOTAS; its original MIT notice is in [licenses/virtualHOTAS-MIT.txt](licenses/virtualHOTAS-MIT.txt). The Flask/browser implementation is not included in this native Android APK.
