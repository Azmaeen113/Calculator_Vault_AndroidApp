# Calculator Vault - Project Structure

## Overview
A privacy-focused Android calculator app with a hidden vault feature, Google Sign-In authentication, and Firebase cloud sync for calculation history.

---

## Directory Structure

```
Calculator_Vault_androidApp/
│
├── 📁 app/
│   ├── build.gradle.kts                    # App-level build configuration
│   ├── google-services.json                # Firebase configuration
│   ├── proguard-rules.pro                  # ProGuard obfuscation rules
│   │
│   └── 📁 src/main/
│       ├── AndroidManifest.xml             # App manifest & permissions
│       │
│       ├── 📁 java/com/example/calculator_vault_androidapp/
│       │   │
│       │   ├── 📄 MainActivity.java           # Calculator UI & vault PIN entry
│       │   ├── 📄 HistoryActivity.java        # Calculation history + Google Sign-In
│       │   ├── 📄 PinSetupActivity.java       # PIN setup/change screen
│       │   ├── 📄 VaultActivity.java          # Hidden vault file manager
│       │   │
│       │   ├── 📁 adapters/
│       │   │   ├── 📄 HistoryAdapter.java     # RecyclerView adapter for history
│       │   │   └── 📄 VaultFileAdapter.java   # RecyclerView adapter for vault files
│       │   │
│       │   ├── 📁 database/
│       │   │   ├── 📄 DatabaseHelper.java     # SQLite local database helper
│       │   │   └── 📄 FirebaseHelper.java     # Firebase Auth & Realtime Database
│       │   │
│       │   ├── 📁 models/
│       │   │   ├── 📄 CalculationHistory.java # History data model
│       │   │   └── 📄 VaultFile.java          # Vault file data model
│       │   │
│       │   └── 📁 utils/
│       │       ├── 📄 CryptoUtils.java        # Encryption & hashing utilities
│       │       └── 📄 FileUtils.java          # File operations utilities
│       │
│       └── 📁 res/
│           │
│           ├── 📁 layout/
│           │   ├── activity_main.xml          # Calculator screen layout
│           │   ├── activity_history.xml       # History page layout
│           │   ├── activity_pin_setup.xml     # PIN setup layout
│           │   ├── activity_vault.xml         # Vault screen layout
│           │   ├── item_history.xml           # History list item
│           │   └── item_vault_file.xml        # Vault file list item
│           │
│           ├── 📁 drawable/
│           │   ├── ic_back.xml                # Back navigation icon
│           │   ├── ic_calculator.xml          # Calculator icon
│           │   ├── ic_clear.xml               # Clear icon
│           │   ├── ic_delete.xml              # Delete icon
│           │   ├── ic_delete_all.xml          # Delete all icon
│           │   ├── ic_download.xml            # Download icon
│           │   ├── ic_file.xml                # File icon
│           │   ├── ic_folder_empty.xml        # Empty folder icon
│           │   ├── ic_google.xml              # Google Sign-In icon
│           │   ├── ic_history.xml             # History icon
│           │   ├── ic_key.xml                 # Key/PIN icon
│           │   ├── ic_lock.xml                # Lock icon
│           │   ├── ic_open.xml                # Open file icon
│           │   ├── ic_select_all.xml          # Select all icon
│           │   ├── ic_upload.xml              # Upload icon
│           │   ├── bg_file_type.xml           # File type background
│           │   └── bg_file_type_vault.xml     # Vault file type background
│           │
│           ├── 📁 values/
│           │   ├── colors.xml                 # Color definitions
│           │   ├── strings.xml                # String resources
│           │   └── themes.xml                 # App themes & button styles
│           │
│           ├── 📁 values-night/               # Dark theme overrides
│           │
│           ├── 📁 mipmap-*/                   # App launcher icons (all densities)
│           │
│           └── 📁 xml/                        # XML configurations
│
├── 📁 gradle/
│   ├── libs.versions.toml                  # Dependency versions catalog
│   └── 📁 wrapper/
│       └── gradle-wrapper.properties       # Gradle wrapper configuration
│
├── build.gradle.kts                        # Project-level build config
├── settings.gradle.kts                     # Project settings
├── gradle.properties                       # Gradle properties
├── gradlew                                 # Gradle wrapper (Unix)
├── gradlew.bat                             # Gradle wrapper (Windows)
├── local.properties                        # Local SDK path
│
├── ANDROID_APP_PROMPT.md                   # Original app requirements
└── PROJECT_STRUCTURE.md                    # This file
```

---

## Key Components

### Activities

| Activity | Description |
|----------|-------------|
| `MainActivity` | Full-featured calculator with secret PIN detection (enter 5-digit PIN to access vault) |
| `HistoryActivity` | Displays calculation history with Google Sign-In for cloud sync |
| `PinSetupActivity` | Initial PIN setup and PIN change functionality |
| `VaultActivity` | Hidden file manager for storing private photos, videos, and files |

### Database Layer

| Class | Description |
|-------|-------------|
| `DatabaseHelper` | SQLite database for local storage (history, vault files, settings) |
| `FirebaseHelper` | Firebase Authentication (Google Sign-In) + Realtime Database sync |

### Models

| Model | Description |
|-------|-------------|
| `CalculationHistory` | Stores expression, result, and timestamp |
| `VaultFile` | Stores file metadata (name, path, type, encrypted status) |

### Utilities

| Utility | Description |
|---------|-------------|
| `CryptoUtils` | PIN hashing (SHA-256), file encryption/decryption |
| `FileUtils` | File copy, move, delete, and type detection |

### Adapters

| Adapter | Description |
|---------|-------------|
| `HistoryAdapter` | RecyclerView adapter for calculation history list |
| `VaultFileAdapter` | RecyclerView adapter for vault file grid/list |

---

## Color Scheme

| Color Name | Hex Code | Usage |
|------------|----------|-------|
| `calculator_background` | `#1C1C1C` | Main app background |
| `button_number` | `#333333` | Number buttons (0-9) |
| `button_operator` | `#FF9800` | Operator buttons (+, -, ×, ÷) |
| `button_function` | `#505050` | Function buttons (√, x², %) |
| `button_equals` | `#4CAF50` | Equals button |
| `button_clear` | `#FF5252` | Clear button |
| `vault_background` | `#1A1A2E` | Vault screen background |
| `vault_accent` | `#00D9FF` | Vault accent color |

---

## Dependencies

- **AndroidX AppCompat** - Backward compatibility
- **Material Design 3** - UI components
- **Firebase BOM** - Firebase version management
- **Firebase Auth** - Google Sign-In authentication
- **Firebase Realtime Database** - Cloud data sync
- **Google Play Services Auth** - Google Sign-In SDK

---

## Build Information

- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 36
- **Java Version**: 11

---

## Firebase Configuration

- **Project**: Calculator Vault
- **Package**: `com.example.calculator_vault_androidapp`
- **Auth Provider**: Google Sign-In
- **Database**: Firebase Realtime Database
- **Data Path**: `users/{userId}/calculation_history/`
