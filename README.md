# Mealoo

Mealoo is an Android food tracking, meal planning, inventory, shopping, and nutrition assistant. It combines local-first Room storage with optional Firebase community features, Google Drive backup, and AI-assisted meal analysis.

## Features

- Daily calorie, macro, water, caffeine, and alcohol tracking
- Pantry, refrigerator, freezer, expiry, and consumption management
- Recipes, meal planning, shopping lists, and discount calculations
- English, Traditional Chinese, and Japanese localization
- Optional Firebase authentication, community recipes, and cloud functions
- Optional on-device inference through `llama.cpp`

## Requirements

- Android Studio
- JDK 17
- Android SDK 35
- Android NDK and CMake 3.22.1 or later
- Node.js 22 and Firebase CLI for Cloud Functions development

## Getting started

Clone the repository with its native inference dependency:

```bash
git clone --recurse-submodules https://github.com/suika21921-sudo/FoodCalApp.git
cd FoodCalApp
```

Open the directory in Android Studio and let Gradle synchronize the project.

### Firebase configuration

The production Firebase configuration is intentionally not included. Create your own Firebase project, register the Android application ID `com.mealoo.app`, and place the downloaded file at:

```text
app/google-services.json
```

Copy `.firebaserc.example` to `.firebaserc` and replace the placeholder with your Firebase project ID.

Cloud Functions read the OpenAI key from Firebase Secret Manager. Configure it without writing the key into the repository:

```bash
firebase functions:secrets:set OPENAI_API_KEY
```

## Build

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

## Security

Never commit API keys, `google-services.json`, `.firebaserc`, signing keys, or local configuration. If a secret is committed accidentally, revoke it immediately and replace it with a new credential.

## Third-party software

Native inference uses the upstream [`llama.cpp`](https://github.com/ggerganov/llama.cpp) project as a Git submodule. Its own license applies to that dependency.

## License

Mealoo is released under the [MIT License](LICENSE).
