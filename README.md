# ICADD Records — Native Android App

Native Kotlin Android app (not a PWA) backed by Firebase Firestore for
fast, real-time data — replacing the Apps Script + Google Drive backend.

## What's included
- Login / employee self-registration
- Dashboard (live counts)
- Inward register (view + add)
- Outward register (view + add, links to Inward)
- Attendance (view + record)
- Employees (admin only — add employees, toggle access)
- A GitHub Actions workflow that automatically compiles the APK — you
  never need Android Studio.

## One-time setup (about 15 minutes)

### 1. Create a Firebase project
1. Go to https://console.firebase.google.com → **Add project** → name it
   (e.g. "ICADD Records") → finish creation (Analytics optional, can skip).
2. Inside the project, click **Build > Authentication > Get started** →
   enable the **Email/Password** sign-in method.
3. Click **Build > Firestore Database > Create database** → start in
   **production mode** → pick a region close to India (e.g. asia-south1).
4. Click **Build > Storage > Get started** → keep default rules for now.

### 2. Register the Android app in Firebase
1. In Project Settings (gear icon) → **Your apps** → **Add app** → Android.
2. Package name: `com.icadd.records` (must match exactly).
3. Skip nickname/SHA-1 (not required for this setup) → **Register app**.
4. Download the **google-services.json** file it offers you.

### 3. Put the project on GitHub
1. Create a new **empty** GitHub repository (e.g. `records-android-app`).
2. Upload every file/folder from this project into the repo (GitHub's
   web "Add file > Upload files" works fine — drag the whole folder in).
3. Also upload the `google-services.json` from Step 2 into the `app/`
   folder of the repo (same folder as `app/build.gradle.kts`).
4. Commit directly to the `main` branch.

### 4. Let GitHub build your APK
- As soon as you commit, go to the repo's **Actions** tab — a "Build
  Android APK" run starts automatically (takes ~3–5 minutes).
- When it finishes (green check), open the run → scroll to
  **Artifacts** → download **records-app-debug-apk** → unzip it to get
  `app-debug.apk`.
- Transfer that .apk to your Android phone (email it to yourself, or
  Google Drive) and tap it to install (allow "install unknown apps"
  for that source when prompted).

### 5. First login
- Open the app → tap **New employee? Register** → the very first
  person to register automatically becomes **admin**.
- As admin, go to the **Employees** tab to add your staff by email —
  they then register in the app with that same email to set their
  own password.

## If the Actions build fails
Open the failed run → click the red step to see the error, and send
me that error text — most first-build issues are a small Gradle
version mismatch and are quick to fix.
