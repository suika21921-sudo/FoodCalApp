# Mealoo

Mealoo is an Android food tracking, meal planning, inventory, shopping, and nutrition assistant. The default Android build is standalone/local-first: core data stays in Room and DataStore, the Google Drive integration is separate from Firebase, and on-device inference uses `llama.cpp`.

## Features

- Daily calorie, macro, water, caffeine, and alcohol tracking
- Pantry, refrigerator, freezer, expiry, and consumption management
- Recipes, meal planning, shopping lists, and discount calculations
- English, Traditional Chinese, and Japanese localization
- Optional Google Drive integration, separate from Firebase
- Optional on-device inference through `llama.cpp`
- Preserved Firebase/community source in an explicitly enabled cloud flavor

## Requirements

- Android Studio
- JDK 17
- Android SDK 35
- Android NDK and CMake 3.22.1 or later
- Git with submodule support
- Optional: Node.js 22 for reviewing the preserved backend source

## Getting started

Clone the repository with its native inference dependency:

```bash
git clone --recurse-submodules https://github.com/suika21921-sudo/FoodCalApp.git
cd FoodCalApp
```

Open the directory in Android Studio and let Gradle synchronize the project.

## Build modes

### Standalone — default

`standalone` is the default and supported local-first mode. It:

- does not apply the Google Services Gradle plugin;
- does not package Firebase Auth, Firestore, or Storage SDK dependencies;
- does not generate Firebase resources or include `FirebaseInitProvider`;
- does not require `google-services.json`, `.firebaserc`, a Firebase project ID, Firebase credentials, App Check, deployed Cloud Functions, Secret Manager, or an OpenAI key;
- keeps Room, DataStore, the existing Google Drive integration, and local `llama.cpp` functionality available.

Build and verify it with:

```bash
./gradlew :app:assembleStandaloneDebug
./gradlew :app:testStandaloneDebugUnitTest
./gradlew :app:lintStandaloneDebug
```

### Cloud — optional legacy/reference mode

The `cloud` flavor preserves the older Firebase-backed identity/community adapters and external Gemini handoff as optional legacy/reference functionality. It is not the default and is not connected to the original production Firebase project, which has been removed.

Anyone building this flavor must create and configure a Firebase project they control, register `com.mealoo.app`, and provide an ignored `app/src/cloud/google-services.json`. The legacy shared path `app/google-services.json` is deliberately rejected for cloud builds to prevent accidental reuse. No original Firebase credentials, project configuration, or production identifiers are included.

Enable cloud variants explicitly:

```bash
./gradlew -Pmealoo.cloud=true :app:assembleCloudDebug
./gradlew -Pmealoo.cloud=true :app:testCloudDebugUnitTest
./gradlew -Pmealoo.cloud=true :app:lintCloudDebug
```

The preserved `functions/`, `firestore.rules`, `storage.rules`, and `firebase.json` files are optional reference infrastructure. They are not required by standalone builds and are not deployed as part of the Android build or this repository workflow.

## Google Drive backup

Google Drive transport and Google sign-in are separate from Firebase and are not disabled by this flavor split. Legacy Drive payloads encrypted from a Firebase UID cannot be decrypted by standalone mode. The retained cloud identity bridge is reference code only: it does not provide a Firebase sign-in or a supported migration path after removal of the original project. Existing database backup/restore also has portability limitations and should not be treated as cross-device recovery until that subsystem receives a separate review.

## Local AI

Local AI remains available in standalone mode through the pinned `llama.cpp` submodule. Compatible GGUF model files are local assets and must not be committed. External cloud-AI handoff controls are compiled only into the optional cloud flavor.

## Security

Never commit API keys, `google-services.json`, `.firebaserc`, service-account credentials, Firebase Admin credentials, signing keys, model files, or local configuration.

The default standalone build contains no Firebase product SDKs (Auth, Firestore, Storage, Functions, or App Check), Firebase configuration resources, or Firebase automatic initialization provider. Generic `firebase-encoders` libraries may still appear transitively through the existing billing dependency; they do not configure or initialize a Firebase project. The optional backend source is retained for review and possible future reuse only; no backend deployment is performed by the documented build process.

## Third-party software

Native inference uses the upstream [`llama.cpp`](https://github.com/ggerganov/llama.cpp) project as a Git submodule. Its own license applies to that dependency.

## License

Mealoo is released under the [MIT License](LICENSE).
