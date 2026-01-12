# Calculator Vault - Source Structure

## src/main/

```
src/main/
│
├── AndroidManifest.xml
│
├── java/com/example/calculator_vault_androidapp/
│   │
│   ├── MainActivity.java
│   ├── HistoryActivity.java
│   ├── PinSetupActivity.java
│   ├── VaultActivity.java
│   │
│   ├── adapters/
│   │   ├── HistoryAdapter.java
│   │   └── VaultFileAdapter.java
│   │
│   ├── database/
│   │   ├── DatabaseHelper.java
│   │   └── FirebaseHelper.java
│   │
│   ├── models/
│   │   ├── CalculationHistory.java
│   │   └── VaultFile.java
│   │
│   └── utils/
│       ├── CryptoUtils.java
│       └── FileUtils.java
│
└── res/
    │
    ├── layout/
    │   ├── activity_main.xml
    │   ├── activity_history.xml
    │   ├── activity_pin_setup.xml
    │   ├── activity_vault.xml
    │   ├── item_history.xml
    │   └── item_vault_file.xml
    │
    ├── drawable/
    │   ├── ic_back.xml
    │   ├── ic_calculator.xml
    │   ├── ic_clear.xml
    │   ├── ic_delete.xml
    │   ├── ic_delete_all.xml
    │   ├── ic_download.xml
    │   ├── ic_file.xml
    │   ├── ic_folder_empty.xml
    │   ├── ic_google.xml
    │   ├── ic_history.xml
    │   ├── ic_key.xml
    │   ├── ic_lock.xml
    │   ├── ic_open.xml
    │   ├── ic_select_all.xml
    │   ├── ic_upload.xml
    │   ├── bg_file_type.xml
    │   └── bg_file_type_vault.xml
    │
    ├── values/
    │   ├── colors.xml
    │   ├── strings.xml
    │   └── themes.xml
    │
    ├── values-night/
    │
    ├── mipmap-hdpi/
    ├── mipmap-mdpi/
    ├── mipmap-xhdpi/
    ├── mipmap-xxhdpi/
    ├── mipmap-xxxhdpi/
    ├── mipmap-anydpi-v26/
    │
    └── xml/
```
