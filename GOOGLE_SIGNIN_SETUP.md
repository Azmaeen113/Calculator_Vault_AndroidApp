# Google Sign-In Setup Instructions

## Overview
Your calculation history is now protected by Google Sign-In. Each Google account will have its own separate history stored in Firebase.

## Firebase Console Setup

### Step 1: Get SHA-1 Certificate Fingerprint

Run this command in your project directory:
```bash
cd android
./gradlew signingReport
```

Or for Windows:
```bash
gradlew.bat signingReport
```

Copy the **SHA-1** and **SHA-256** fingerprints from the output.

### Step 2: Add SHA Fingerprints to Firebase

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: `Calculator_Vault_AndroidApp`
3. Click on **Project Settings** (gear icon)
4. Scroll to **Your apps** section
5. Click on your Android app
6. Scroll to **SHA certificate fingerprints**
7. Click **Add fingerprint**
8. Paste your SHA-1 fingerprint
9. Click **Add fingerprint** again and add SHA-256

### Step 3: Enable Google Sign-In

1. In Firebase Console, go to **Authentication**
2. Click **Sign-in method** tab
3. Click on **Google**
4. Toggle **Enable**
5. Select a **Support email** (your email)
6. Click **Save**

### Step 4: Get Web Client ID

1. Still in Firebase Console, go to **Project Settings**
2. Scroll to **Your apps**
3. Under **SDK setup and configuration**, select **Config**
4. Find and copy the `oauth_client` with `client_type: 3` (Web client)
5. It looks like: `123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com`

### Step 5: Update Your Code

Open `HistoryActivity.java` and replace this line:

```java
private static final String WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE.apps.googleusercontent.com";
```

With your actual Web Client ID:

```java
private static final String WEB_CLIENT_ID = "123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com";
```

### Step 6: Download Updated google-services.json

1. In Firebase Console, go to **Project Settings**
2. Scroll to **Your apps**
3. Click **Download google-services.json**
4. Replace the existing `app/google-services.json` with the new one

## Testing

1. **Sync Gradle** in Android Studio
2. **Clean & Rebuild** the project
3. Run the app
4. Click on **History** button
5. You should see **Sign in with Google** button
6. Click it and sign in with your Google account
7. Your calculation history will now be tied to your Google account

## How It Works

- **Each Google account** = Separate history
- **Calculations are saved locally** first (works offline)
- **Synced to Firebase** when signed in
- **Sign out** anytime - history stays in cloud
- **Sign back in** from any device to access your history

## Troubleshooting

### "Sign-in failed" error
- Check that SHA fingerprints are added in Firebase Console
- Verify Web Client ID is correct in HistoryActivity.java
- Make sure Google Sign-In is enabled in Firebase Authentication

### "Developer error" message
- This means SHA-1 fingerprint doesn't match
- Re-run `gradlew signingReport` and update Firebase Console

### History not syncing
- Make sure you're signed in (check top of History screen)
- Check internet connection
- Verify Firebase Realtime Database rules allow authenticated users to write

## Firebase Database Rules

Make sure your Firebase Realtime Database has these rules:

```json
{
  "rules": {
    "users": {
      "$uid": {
        ".read": "$uid === auth.uid",
        ".write": "$uid === auth.uid"
      }
    }
  }
}
```

This ensures each user can only access their own data.

## Done! 🎉

Your app now has Google Sign-In for calculation history. Each user has their own private history in the cloud!
