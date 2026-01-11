# Calculator Vault - Android App Development Prompt

> Use this prompt with GitHub Copilot in Android Studio to create the Android version of Calculator Vault.

---

## APP OVERVIEW

A calculator app that functions as a disguised secret vault. Users can perform normal calculations, but entering a secret 5-digit PIN unlocks a hidden vault where they can securely store files. The app also maintains calculation history.

---

## TECH STACK

- **Language:** Java
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34
- **Database:** SQLite (local) + Firebase Realtime Database (cloud backup)
- **Firebase Auth:** Anonymous authentication for device identification
- **Architecture:** MVVM pattern with Activities/Fragments

---

## DATABASE SCHEMA

### SQLite Tables

#### 1. config table
```sql
CREATE TABLE config (
    id INTEGER PRIMARY KEY,
    pin_hash TEXT,
    is_first_time INTEGER,
    created_at TEXT
);
```

#### 2. vault_files table
```sql
CREATE TABLE vault_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_name TEXT,
    original_extension TEXT,
    file_data BLOB,
    file_size INTEGER,
    uploaded_at TEXT
);
```

#### 3. calculation_history table
```sql
CREATE TABLE calculation_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    expression TEXT,
    result TEXT,
    calculated_at TEXT
);
```

### Firebase Structure

```
/users/{device_id}/
  /config/
    - pin_hash: string
    - created_at: timestamp
  /vault_files/
    /{file_id}/
      - file_name: string
      - original_extension: string
      - file_data: base64_string
      - file_size: long
      - uploaded_at: timestamp
  /calculation_history/
    /{history_id}/
      - expression: string
      - result: string
      - calculated_at: timestamp
```

---

## SCREENS/ACTIVITIES

### 1. MainActivity (Calculator Screen)

**Layout:** `activity_main.xml`

- Display TextField at top (non-editable, right-aligned, large font)
- 4x4 GridLayout for calculator buttons:
  - Row 1: 7, 8, 9, ÷
  - Row 2: 4, 5, 6, ×
  - Row 3: 1, 2, 3, -
  - Row 4: C, 0, =, +
- "History" button below the grid (full width)
- All buttons should be 60dp height minimum

**Functionality:**
- Standard calculator operations (+, -, ×, ÷)
- After pressing "=", save the calculation to history (expression + result)
- Secret trigger: When user types a 5-digit number that matches the stored PIN hash, automatically open VaultActivity
- Format numbers: show integers without decimals, show decimals only when needed

### 2. PinSetupActivity

**Layout:** `activity_pin_setup.xml`

- Title: "Set / Change PIN"
- EditText for current PIN (hidden by default, shown only for PIN change)
- EditText for new PIN (password input type)
- EditText for confirm PIN (password input type)
- Submit button

**Functionality:**
- On first launch, show only new PIN and confirm PIN fields
- PIN must be exactly 5 digits
- PIN and confirm must match
- Hash PIN using SHA-256 before storing
- For PIN change: verify current PIN, ensure new PIN is different, re-encrypt all vault files

### 3. VaultActivity (Secret File Vault)

**Layout:** `activity_vault.xml`

- Title: "Secure Vault"
- RecyclerView for file list with columns:
  - File Name
  - Type (extension)
  - Size (formatted as KB/MB)
  - Date Added
- Bottom toolbar with buttons:
  - Upload File
  - Open Selected
  - Download Selected
  - Delete Selected
  - Change PIN
  - Lock Vault (returns to calculator)

**Functionality:**
- Upload: Use file picker, encrypt file with XOR using PIN, store in database
- Open: Decrypt file, save to temp location, open with appropriate app using Intent
- Download: Decrypt file, let user choose save location
- Delete: Confirm dialog, then remove from database
- Change PIN: Opens PinSetupActivity in change mode, re-encrypts all files
- Lock: Clear temp files, return to calculator

### 4. HistoryActivity

**Layout:** `activity_history.xml`

- Title: "Calculation History"
- "Clear All" button
- "Back to Calculator" button
- RecyclerView with columns:
  - Expression
  - Result
  - Time
  - Delete button (per row)

**Functionality:**
- Show all calculations sorted by date (newest first)
- Individual delete with confirmation dialog
- Clear all with confirmation dialog
- Back button returns to calculator

---

## CLASSES TO CREATE

### Models (package: `models/`)

#### VaultFile.java
```java
public class VaultFile {
    private Integer id;
    private String fileName;
    private String originalExtension;
    private byte[] fileData;
    private long fileSize;
    private String uploadedAt;

    public VaultFile() {}

    public VaultFile(Integer id, String fileName, String originalExtension, 
                     byte[] fileData, long fileSize, String uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.originalExtension = originalExtension;
        this.fileData = fileData;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalExtension() { return originalExtension; }
    public void setOriginalExtension(String originalExtension) { this.originalExtension = originalExtension; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}
```

#### CalculationHistory.java
```java
public class CalculationHistory {
    private int id;
    private String expression;
    private String result;
    private String calculatedAt;

    public CalculationHistory() {}

    public CalculationHistory(int id, String expression, String result, String calculatedAt) {
        this.id = id;
        this.expression = expression;
        this.result = result;
        this.calculatedAt = calculatedAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(String calculatedAt) { this.calculatedAt = calculatedAt; }
}
```

### Database (package: `database/`)

#### DatabaseHelper.java - SQLite helper

Methods needed:
- `setPinHash(String hash)`
- `getPinHash(): String`
- `isFirstTime(): boolean`
- `saveFile(VaultFile file)`
- `getAllFiles(): List<VaultFile>`
- `getFileData(int id): byte[]`
- `deleteFile(int id)`
- `updateAllFilesEncryption(String oldPin, String newPin)`
- `saveCalculation(String expression, String result)`
- `getCalculationHistory(): List<CalculationHistory>`
- `deleteCalculation(int id)`
- `clearAllHistory()`

#### FirebaseHelper.java - Firebase operations

Methods needed:
- Anonymous authentication
- Sync all data to Firebase
- Methods to backup/restore from Firebase

### Utils (package: `utils/`)

#### CryptoUtils.java
```java
public class CryptoUtils {
    
    // Hash a 5-digit PIN using SHA-256 and return hex string
    public static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // Verify input PIN against stored hash
    public static boolean verifyPin(String inputPin, String storedHash) {
        return hashPin(inputPin).equals(storedHash);
    }

    // XOR encryption
    public static byte[] encryptData(byte[] data, String pin) {
        return xorWithKey(data, pin.getBytes());
    }

    // XOR decryption
    public static byte[] decryptData(byte[] data, String pin) {
        return xorWithKey(data, pin.getBytes());
    }

    private static byte[] xorWithKey(byte[] data, byte[] key) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return out;
    }
}
```

#### FileUtils.java
```java
public class FileUtils {
    private static final List<File> TEMP_FILES = new ArrayList<>();

    public static String formatFileSize(long bytes) {
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    public static String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(idx + 1) : "";
    }

    public static File createTempFile(Context context, String fileName, byte[] data) throws IOException {
        File tempDir = new File(context.getCacheDir(), "vault_temp");
        if (!tempDir.exists()) tempDir.mkdirs();
        
        File tempFile = new File(tempDir, fileName);
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(data);
        fos.close();
        
        TEMP_FILES.add(tempFile);
        return tempFile;
    }

    public static void deleteTempFiles() {
        for (File f : new ArrayList<>(TEMP_FILES)) {
            if (f.exists()) f.delete();
        }
        TEMP_FILES.clear();
    }
}
```

### Adapters (package: `adapters/`)

#### VaultFileAdapter.java
- RecyclerView adapter for vault files
- Display file name, type, size, date
- Handle item selection

#### HistoryAdapter.java
- RecyclerView adapter for calculation history
- Display expression, result, time
- Include delete button per row

---

## APP FLOW

### 1. First Launch
1. Check if PIN is set (`isFirstTime` or `pinHash` is null)
2. If first time → Show `PinSetupActivity`
3. After PIN set → Show `MainActivity` (Calculator)

### 2. Normal Use
1. User uses calculator normally
2. Calculations are saved to history
3. User can view history via History button

### 3. Secret Vault Access
1. User types their 5-digit PIN on calculator
2. System detects PIN match (compare SHA-256 hash)
3. `VaultActivity` opens automatically
4. Calculator display is cleared

### 4. Vault Operations
1. Files are encrypted with XOR using PIN before storage
2. Temp files are cleaned up when locking vault
3. PIN change re-encrypts all stored files

---

## DEPENDENCIES

### build.gradle (Module: app)

```gradle
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}

android {
    namespace 'com.example.calculatorvault'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.calculatorvault"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-database'
    implementation 'com.google.firebase:firebase-auth'
}
```

### build.gradle (Project level)

```gradle
plugins {
    id 'com.android.application' version '8.2.0' apply false
    id 'com.google.gms.google-services' version '4.4.0' apply false
}
```

---

## PERMISSIONS

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
        android:maxSdkVersion="29" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="Calculator"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.CalculatorVault">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <activity android:name=".PinSetupActivity" />
        <activity android:name=".VaultActivity" />
        <activity android:name=".HistoryActivity" />
        
        <!-- FileProvider for sharing files -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
        
    </application>

</manifest>
```

### res/xml/file_paths.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="vault_temp" path="vault_temp/" />
</paths>
```

---

## IMPORTANT IMPLEMENTATION NOTES

1. **File Picker:** Use `ActivityResultLauncher` (not deprecated `startActivityForResult`)
2. **Permissions:** Handle runtime permissions for storage access on Android 10+
3. **File Sharing:** Use `FileProvider` for sharing files with other apps
4. **Temp Files:** Clean up temp files in `onDestroy()` and when locking vault
5. **Dialogs:** Use `AlertDialog` for confirmations
6. **Notifications:** Use `Toast` for success/error messages
7. **Error Handling:** Implement proper try-catch blocks
8. **Threading:** Use `ExecutorService` or `AsyncTask` for database operations
9. **Data Storage:** Store encrypted files as BLOB in SQLite, base64 in Firebase
10. **Timestamps:** Format consistently using `SimpleDateFormat`

---

## PROJECT STRUCTURE

```
app/
├── src/main/
│   ├── java/com/example/calculatorvault/
│   │   ├── MainActivity.java
│   │   ├── PinSetupActivity.java
│   │   ├── VaultActivity.java
│   │   ├── HistoryActivity.java
│   │   ├── models/
│   │   │   ├── VaultFile.java
│   │   │   └── CalculationHistory.java
│   │   ├── database/
│   │   │   ├── DatabaseHelper.java
│   │   │   └── FirebaseHelper.java
│   │   ├── adapters/
│   │   │   ├── VaultFileAdapter.java
│   │   │   └── HistoryAdapter.java
│   │   └── utils/
│   │       ├── CryptoUtils.java
│   │       └── FileUtils.java
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   ├── activity_pin_setup.xml
│   │   │   ├── activity_vault.xml
│   │   │   ├── activity_history.xml
│   │   │   ├── item_vault_file.xml
│   │   │   └── item_history.xml
│   │   ├── values/
│   │   │   ├── colors.xml
│   │   │   ├── strings.xml
│   │   │   └── themes.xml
│   │   └── xml/
│   │       └── file_paths.xml
│   └── AndroidManifest.xml
├── build.gradle
└── google-services.json
```

---

## HOW TO USE THIS PROMPT

1. **Create New Project in Android Studio:**
   - Choose "Empty Activity"
   - Name: `CalculatorVault`
   - Package: `com.example.calculatorvault`
   - Language: **Java**
   - Minimum SDK: API 24

2. **Set up Firebase:**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create new project
   - Add Android app (use your package name)
   - Download `google-services.json` → place in `app/` folder
   - Enable Anonymous Authentication
   - Enable Realtime Database

3. **Open Copilot Chat and work step by step:**
   - "Create the model classes"
   - "Create DatabaseHelper.java with all methods"
   - "Create CryptoUtils.java"
   - "Create FileUtils.java"
   - "Create activity_main.xml layout"
   - "Create MainActivity.java"
   - Continue for each component...

---

## ORIGINAL JAVAFX APP REFERENCE

This Android app is based on the JavaFX Calculator Vault application with the following original components:

| JavaFX Component | Android Equivalent |
|------------------|-------------------|
| `HelloApplication.java` | `MainActivity.java` |
| `CalculatorController.java` | `MainActivity.java` |
| `VaultController.java` | `VaultActivity.java` |
| `PINSetupController.java` | `PinSetupActivity.java` |
| `HistoryController.java` | `HistoryActivity.java` |
| `DatabaseManager.java` | `DatabaseHelper.java` |
| `ConfigManager.java` | `CryptoUtils.java` |
| `FileVaultService.java` | `FileUtils.java` + `CryptoUtils.java` |
| `CalculatorService.java` | Inline in `MainActivity.java` |
| FXML files | XML layout files |

---

*Generated from Calculator_Vault JavaFX application for Android conversion*
