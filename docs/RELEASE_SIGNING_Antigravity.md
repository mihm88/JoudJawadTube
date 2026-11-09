# Production Release Signing & Security Guide 🔐

This document provides complete instructions for managing, backing up, and verifying the official production release signing key for **Joud&Jawad Tube**.

---

## 🛡️ Preventing Keystore Loss

Android strictly enforces certificate continuity: every future update (including in-app updates) **must be signed with the exact same certificate**. If the signing key is lost, you cannot update existing installations without uninstalling the app.

### How Keystore Loss is Prevented:
1. **Separation from Source Code**:
   - The keystore is stored outside the Git repository in:
     `C:\Users\engmo\keystores\joudjawadtube-release_Antigravity.jks`
   - It is never committed to Git (`.gitignore` blocks `*.jks`, `*.keystore`, `*.p12`, `keystore.properties`).
2. **Automated Redundant Local Backup**:
   - A mirror copy is preserved at:
     `C:\Users\engmo\keystores\backup\joudjawadtube-release_Antigravity_backup.jks`
3. **Recommended Secondary Off-site Backups**:
   - Copy `joudjawadtube-release_Antigravity.jks` to an encrypted USB flash drive or secure cloud vault (e.g., 1Password, Bitwarden, or encrypted 7-Zip archive).
   - Store the passwords separately in your password manager.

---

## 📜 Certificate & Fingerprint Details

| Property | Value |
|:---|:---|
| **Keystore File** | `C:\Users\engmo\keystores\joudjawadtube-release_Antigravity.jks` |
| **Keystore Format** | PKCS12 (Standard, future-proof) |
| **Key Alias** | `joudjawadtube` |
| **Key Algorithm** | RSA 4096-bit (Self-signed SHA384withRSA) |
| **Validity** | 10,000 Days (~27 Years, expires year 2054) |
| **Subject / Owner** | `CN=Joud&Jawad Tube, OU=Mobile, O=mihm88, L=Khartoum, C=SD` |
| **Certificate SHA-256 Digest** | `9D:70:A7:A6:3A:1D:71:3F:FA:40:DD:EA:4E:CD:15:F1:F5:EA:88:9C:A9:0F:B1:59:60:B2:CC:2F:40:66:B0:82` |
| **Certificate SHA-1 Digest** | `3C:A6:45:88:21:B0:D8:60:1D:AA:25:B2:63:3A:30:63:8E:A0:B8:9F` |

---

## ⚙️ Local Signing Configuration (`keystore.properties`)

The project automatically reads signing credentials from `keystore.properties` at the project root (ignored by Git):

```properties
storeFile=C:/Users/engmo/keystores/joudjawadtube-release_Antigravity.jks
storePassword=JoudJawadTube2026SecureReleaseKey
keyAlias=joudjawadtube
keyPassword=JoudJawadTube2026SecureReleaseKey
```

When building with Gradle, `app/build.gradle.kts` applies **v1 (JAR Scheme)**, **v2 (Full APK Scheme)**, and **v3 (Android 9+ Scheme)** signatures automatically:
```bash
.\gradlew.bat assembleRelease
```
The resulting signed and optimized APK is generated at:
```
app/build/outputs/apk/release/JoudJawadTube-v1.1.0.apk
```

---

## 🔍 Exact Commands to Verify APK Signatures

Using the Android SDK Build-Tools:

### 1. Verify Signature Schemes (v1, v2, v3) & Certificate
```powershell
$BT = "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0"
& "$BT\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\JoudJawadTube-v1.1.0.apk
```
**Expected Output:**
```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Signer #1 certificate SHA-256 digest: 9d70a7a63a1d713ffa40ddea4ecd15f1f5ea889ca90fb15960b2cc2f4066b082
```

### 2. Verify Merged Permissions & Capabilities
```powershell
& "$BT\aapt2.exe" dump permissions app\build\outputs\apk\release\JoudJawadTube-v1.1.0.apk
```

### 3. Verify Target SDK (API 36) & Locales
```powershell
& "$BT\aapt2.exe" dump badging app\build\outputs\apk\release\JoudJawadTube-v1.1.0.apk | Select-String "package|targetSdkVersion|locales"
```

### 4. Verify 4-Byte Zip Alignment
```powershell
& "$BT\zipalign.exe" -c -v 4 app\build\outputs\apk\release\JoudJawadTube-v1.1.0.apk
```

---

## ☁️ GitHub Actions Automated CI/CD Setup

To enable automated release signing on GitHub Actions:

1. Copy the Base64 representation of your keystore to your clipboard:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\keystores\joudjawadtube-release_Antigravity.jks")) | Set-Clipboard
```
2. Navigate to your GitHub repository:
   **Settings > Secrets and variables > Actions > New repository secret**
3. Add the following secrets:
   - `JJT_KEYSTORE_BASE64`: Paste the clipboard value.
   - `JJT_KEYSTORE_PASSWORD`: `JoudJawadTube2026SecureReleaseKey`
   - `JJT_KEY_ALIAS`: `joudjawadtube`
   - `JJT_KEY_PASSWORD`: `JoudJawadTube2026SecureReleaseKey`
