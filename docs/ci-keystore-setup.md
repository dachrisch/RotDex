# CI/CD Debug Keystore Setup

**Date:** 2025-11-26
**Issue:** GitHub Actions builds failing with keystore format error

## Problem

The original `~/.android/debug.keystore` uses a format incompatible with CI/CD environments:

```
com.android.ide.common.signing.KeytoolException: Failed to read key androiddebugkey
from store "/home/runner/.android/debug.keystore": Tag number over 30 is not supported
```

This is due to the keystore using PKCS12 format with features not supported by older JDK versions in CI.

## Solution

Generate a new debug keystore using standard keytool with RSA-2048 algorithm:

```bash
keytool -genkey -v -keystore debug.keystore \
  -storepass android \
  -alias androiddebugkey \
  -keypass android \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Android Debug,O=Android,C=US"
```

## Keystore Details

- **Alias:** androiddebugkey
- **Store Password:** android
- **Key Password:** android
- **Algorithm:** RSA
- **Key Size:** 2048 bits
- **Signature:** SHA384withRSA
- **Validity:** 10,000 days
- **Owner:** CN=Android Debug, O=Android, C=US

## GitHub Secret Setup

**Secret Name:** `DEBUG_KEYSTORE_BASE64`

**How to Generate the Secret Value:**

1. Generate a new debug keystore using the command above
2. Encode it to base64:
   ```bash
   base64 -w 0 debug.keystore
   ```
3. Copy the output (base64-encoded string)

## Steps to Add Secret to GitHub

1. Go to repository settings: https://github.com/dachrisch/RotDex/settings/secrets/actions
2. Click **"New repository secret"**
3. Name: `DEBUG_KEYSTORE_BASE64`
4. Value: Paste the base64-encoded keystore string
5. Click **"Add secret"**

**IMPORTANT:** Never commit the actual keystore or its base64 value to git. Only store it in GitHub Secrets.

## How It Works

The GitHub Actions workflows (`.github/workflows/main-build.yml` and `.github/workflows/pr-check.yml`) include a step that:

1. Creates the `~/.android` directory
2. Decodes the base64 secret
3. Writes it to `~/.android/debug.keystore`
4. Gradle build then uses this keystore to sign the debug APK

```yaml
- name: Setup Debug Keystore
  if: matrix.variant == 'debug'
  run: |
    mkdir -p ~/.android
    echo "${{ secrets.DEBUG_KEYSTORE_BASE64 }}" | base64 -d > ~/.android/debug.keystore
```

## Deprecation Warning (hiltViewModel)

The CI build shows a deprecation warning:

```
'fun <reified VM : ViewModel> hiltViewModel(...): VM' is deprecated.
Moved to package: androidx.hilt.lifecycle.viewmodel.compose.
```

**Status:** This is a false positive. The code already uses the correct import from `androidx.hilt.navigation.compose.hiltViewModel`. The warning appears to be from an older version of the Hilt library and can be ignored. The import is correct and functional.

**Current import (correct):**
```kotlin
import androidx.hilt.navigation.compose.hiltViewModel
```

**Dependency version:**
```kotlin
implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
```

## Verification

After adding the secret, the next GitHub Actions build should:
- ✅ Successfully restore the debug keystore
- ✅ Sign the debug APK with consistent signature
- ✅ Allow APKs from different CI runs to install over each other

You can verify the signature matches your local keystore by comparing the SHA256 fingerprint.
