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
- **Validity:** 10,000 days (until 2053)
- **Owner:** CN=Android Debug, O=Android, C=US

**SHA256 Fingerprint:**
```
E9:C6:E3:CA:59:08:5D:A0:B8:63:29:10:2E:41:40:E5:FC:97:16:63:30:BC:B4:6A:5D:D6:A2:C2:F8:70:FF:F9
```

## GitHub Secret Setup

**Secret Name:** `DEBUG_KEYSTORE_BASE64`

**Secret Value:** (Base64-encoded keystore)
```
MIIKZgIBAzCCChAGCSqGSIb3DQEHAaCCCgEEggn9MIIJ+TCCBcAGCSqGSIb3DQEHAaCCBbEEggWtMIIFqTCCBaUGCyqGSIb3DQEMCgECoIIFQDCCBTwwZgYJKoZIhvcNAQUNMFkwOAYJKoZIhvcNAQUMMCsEFD1uRDRnR52nxl8mePyTW+lCpOwTAgInEAIBIDAMBggqhkiG9w0CCQUAMB0GCWCGSAFlAwQBKgQQHpsV7zxOBfCpUDsvCBCizwSCBNBHp2l6kXQfMs011r0j2UWDZPJZTnlJFpQdC57xyvDcJsE5dEyKHozcCBc5Cmg1lG7f6GaNbytMAq9LLdWTjbmTdmlwvSejB981nhVFgJEgGFh0nWyi5N8Lzg1+xZk5WtBvmGkOg5ONT4M1iWzYQJaapC8oeo1srKzN2ejB3qqZYALcwb89FYoQKszcgEWC2f5Q1eA/iIfZZQqieg2lFol83lb5iNO7bvRawYtFrklx5piv8WJG8Spp75xnu3NOt44TpMPBNW1bfEFzFccnxjQ+AnDFwoybQNx2Sdtn7ebssV5/uLv5VL9hocWoazW4EvQDYVNWc4nerEeUv4LOPMugWARSwBZkzx5FtQS36wbcb34XllhDHxemRNbMCgQunG2LjzI746n7EuWLdIXkvzgTqZx1Kf4lc4NHbz9coxPA9VqqEh39lemh7W3lNhlWFmwVj7gNMHgs/nzuD5qqYfR8SJExV1XAWi3veiacRTWfoDFM7FfOUgrintP4EtmnlqFghofbEfZBF4wOhmUIbIwMRu+ic1WjHUf2RURcXeO2LJSckng7lBllgedwvOL1PNgul91lCldbykvpw60iFxDQBTFrTkWwUIr7vOyd3a+3ZZV3c0XGbe8vhRX95YONFHgubYQ0HV7ODynAGxfEFH4LBSCV6A0EYMkaeyfp0Pa/SsCVR+XJbwcKTRU/jExr4anHMA/yV48qPK2EdwNURgyua8FQVevXGjjnUVdv9Z9WGWQSjOupsbZCVGQiAM2kYcGLVqyhydQA/GTt7SKP45BkmutY/lpd4ksUJqKawUahniKoqxaAC7z6DKhllxKku4+BAWhbuoleH4Y4mwx0i5WtxtL2zEhLcDWk9lcqDM22zhP8VGZk2lmuOfn4FL4TbdqcZwmpugGRQtxoFtzxeu61dbxj2jZ3IudgZuOi1f10BrjLzP92KBxeQKrrEf7rQuEPcAc1JBSQN12VtTrCdQL4U6lt1kep62GJ60yfQbPd/MnE7IDXKsLwa/+UsG4MHRPPKQOcL/zaWkkRv3X6FNMnYYLofSx4UlxmBMoIGF6+G+rbTXpoZmu6kYZoOUajL5LpFMg6ot+vAFvYlAaPqvcynMEEhZ08tJIORci+y8BHwsYUj0fOHanc/phJCbl7YqwAZ2ITzLMKyzK9UE7woXtyB5SK+HubCvX/S5hY6ET+Bx7rsMEfC4kvFfFLJNGmQfL3kPkpXxX61qCYOixI0kAwYWnCiCDYiYPgLV+C3ITVZ6WU8gK6AW66ipBy3xOoICnIFhJz2VZrX6/X+fwlGN/SkpGT80x5aLfQ57my4gdTcP5yk9rphup5wl78MtgNyfMumYBtxZqg1rAbHVUP9kEWptot92r1OwmF39P4d9GRFazPTs2QVgz/M8ZCD2tMwkmGB5JxLWdSL5gW2NAz1kiKdXPkztn0r2217gonYxPJWn0WKdWcCfHE0B0CfDpwfdIMqYqPMxOucFlk8jXzsV0nAyYHcZWT0L6SN97OZXo4mO5JbMuaethby+xCi20nc86iruBr5p0dOE3PQYYEEqmouSls2jXLsCiZAhMedeJmFgcI+YjN4ZyJ47oxEhajnGDOmR/GRhNPcmt9GXyY1NghgZ9HZFbg+I8jb94n6oOy7zFSMC0GCSqGSIb3DQEJFDEgHh4AYQBuAGQAcgBvAGkAZABkAGUAYgB1AGcAawBlAHkwIQYJKoZIhvcNAQkVMRQEElRpbWUgMTc2NDEyMTU0MTU1MjCCBDEGCSqGSIb3DQEHBqCCBCIwggQeAgEAMIIEFwYJKoZIhvcNAQcBMGYGCSqGSIb3DQEFDTBZMDgGCSqGSIb3DQEFDDArBBS57+ZAEUwFAjfmKk6Hdzl0v9redwICJxACASAwDAYIKoZIhvcNAgkFADAdBglghkgBZQMEASoEEE/9V6FAfjiYgi6fygYb5TSAggOgHU8py9Ht2t0YpQl2KdDhjM5YliERdJI9e52sgjrVXXKtC52Wb5xa+YzahjsomH+0lITE28ojw4toa3z066LR3E902TRLISUbyhzNtAhn4LpXKXut58oRsT3dEdVWAJW7iMRjAF5h0mwTWMZI7au2PWGCUG3Nd+Mft0aZXTMBd4QTtSse4Frjvj2OSZQhNpBJgLNlVODNFhuP2Lh8rvgJm9DEycPocVjZPtKzQLCLBhoQCYZOkV4nhScM9WGZk0rNR3jhv0WL8LT03zjqn72dqembptjkZ9/KE47H5EzD3kkiHwjHxInw/EXXLF5a4kjTdy8L46v/yBI0oFWs+cETTDxvk9zGdciIbhbStFARQtjXyWY6B28V0oMHbPHsbRkqMF7tb2C1P0PrrHZBJNGyIYrlzmGtb+GK1qpmyV5MpmZC2/RdyT7JEG0YR5sgrvq6M5spZaqbSgJxIW+6rGlxLFar+BtaJkoENiyY+JSZ5e9MLZnCTIHR9rGcN1v/9Ns9/87luYLI8I9hn2Sm2uDFnaz/1aege4WIT5xgv5ZR+o13BY19+lc5m/GTOegztIWARhQ8EzesEHGtxg5pAxR78unhNqsT41rVpkkdZuI1/P2TQRB1gK5yaUXDGiTK8sldgw6G3oLWvPGOu4SwEYzxLtQJmpDnuRHqapwsiG3uCEIwminKgOJrIH/LnXFK3yFzEEVR/mYgEuqRW1watEXeE0PLNodqsvvbGEN5aZWtbQ9yNxi5QdP+D7VmbakzVVkWzc+N/a5p3vIab2YXD+W541OLmIlwkUutcOWP2vXsOC68OQsxaV680vSekwewA914BVNTozrMhjiMRF3boX26AFlK5LzexmMak5rjhUDZSkvJ7LP7Jk7LQDBRJrb0BrikU4RgYJVUtQK0fONSYts1WwXa0O4rIR3ok2gttQVTvD0TSi2aUs+KJYexK7Ill6A304/WL7/S8ZafJ20ZC4jnXmq9FyxtWohNNJadmVID+DSoysSIBQO9xs11ipHTozx0TCrtctQbZj6nzEZSUhx4nOoFh7hGoUMEEuw9DrZSJNwTTOow9wlczHjlbbTJmq57BC7hwDCTWaREEpXSAMIO5+3KeuF9Iyp0ErI67x3JITVZ6fqsWylm1vcEnRBw4jhDj1mxSlNhUfRHGa/SbaFKesNQNgcFcx5d9cWPg3SMAt77DQud0bb5l9So8NqNlHYadecfyuiUQTWltNwFy9sKxjBNMDEwDQYJYIZIAWUDBAIBBQAEIJ8RvcmJXuvLXfzDXOKdeuwre/eV1w1vjA/4xzFabD87BBQ+ygwUJBhhk0vSHgXt4dcNFfBWOwICJxA=
```

## Steps to Add Secret to GitHub

1. Go to repository settings: https://github.com/dachrisch/RotDex/settings/secrets/actions
2. Click **"New repository secret"**
3. Name: `DEBUG_KEYSTORE_BASE64`
4. Value: Paste the entire base64 string above
5. Click **"Add secret"**

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
- ✅ Match the signature fingerprint: `E9:C6:E3:CA:59:08:5D:A0:B8:63:29:10:2E:41:40:E5:FC:97:16:63:30:BC:B4:6A:5D:D6:A2:C2:F8:70:FF:F9`
