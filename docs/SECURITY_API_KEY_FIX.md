# CRITICAL: Fix Hardcoded API Key Security Vulnerability

**Status**: 🔴 **PENDING - HIGH PRIORITY**
**Created**: 2025-11-21
**Severity**: CRITICAL

## Issue

The Freepik API key is hardcoded in `app/src/main/java/com/rotdex/di/NetworkModule.kt:32`:

```kotlin
.addHeader("x-freepik-api-key", "FPSXffcc4eacf8e5d7348b79d256b5c8968b")
```

**Risks:**
- ❌ API key exposed in source code
- ❌ Visible in git history
- ❌ Can be extracted from APK
- ❌ Unauthorized API usage possible
- ❌ Potential billing/quota abuse

---

## Solution: Secrets Gradle Plugin (Option B)

### Implementation Steps

#### Step 1: Add Secrets Gradle Plugin

**File**: `app/build.gradle.kts`

```kotlin
plugins {
    // ... existing plugins ...
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
}

// Optional: Configure plugin
secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}
```

#### Step 2: Update NetworkModule

**File**: `app/src/main/java/com/rotdex/di/NetworkModule.kt`

Change line 32 from:
```kotlin
.addHeader("x-freepik-api-key", "FPSXffcc4eacf8e5d7348b79d256b5c8968b")
```

To:
```kotlin
.addHeader("x-freepik-api-key", BuildConfig.FREEPIK_API_KEY)
```

#### Step 3: Create Template File

**File**: `local.properties.template`

```properties
# RotDex API Configuration Template
#
# Instructions:
# 1. Copy this file to: local.properties
# 2. Replace the placeholder with your actual Freepik API key
# 3. DO NOT commit local.properties to git (it's in .gitignore)
#
# Get your API key from: https://www.freepik.com/api/dashboard

FREEPIK_API_KEY=your_freepik_api_key_here
```

#### Step 4: Update .gitignore

**File**: `.gitignore`

Add (if not already present):
```
local.properties
local.defaults.properties
secrets.properties
```

#### Step 5: Update GitHub Actions Workflow

**File**: `.github/workflows/android.yml` (or your workflow file)

Add before the build step:
```yaml
- name: Create local.properties with API key
  run: |
    echo "FREEPIK_API_KEY=${{ secrets.FREEPIK_API_KEY }}" >> local.properties

- name: Build
  run: ./gradlew build
```

#### Step 6: Update Documentation

**File**: `README.md`

Add setup section:
```markdown
## 🔑 API Key Setup

This project uses the Freepik Mystic API. To build the app:

1. **Get an API key** from [Freepik API Dashboard](https://www.freepik.com/api/dashboard)

2. **Create `local.properties`** in the project root:
   ```bash
   cp local.properties.template local.properties
   ```

3. **Add your API key** to `local.properties`:
   ```properties
   FREEPIK_API_KEY=your_actual_api_key_here
   ```

4. **Build the project**:
   ```bash
   ./gradlew assembleDebug
   ```

**Note**: `local.properties` is gitignored and will never be committed.
```

---

## Manual Steps Required

### Immediate Actions

1. **🔴 REVOKE EXPOSED API KEY**
   - Go to Freepik API Dashboard
   - Revoke key: `FPSXffcc4eacf8e5d7348b79d256b5c8968b`
   - Generate new API key

2. **Create local.properties**
   ```bash
   echo "FREEPIK_API_KEY=your_new_key_here" > local.properties
   ```

3. **Add GitHub Secret**
   - Repository → Settings → Secrets and variables → Actions
   - New secret: `FREEPIK_API_KEY`
   - Value: your new API key

### Optional (Recommended)

4. **Clean Git History** (removes exposed key from history)

   Using BFG Repo-Cleaner:
   ```bash
   # Install BFG
   brew install bfg  # macOS
   # or download from: https://rtyley.github.io/bfg-repo-cleaner/

   # Create replacement file
   echo "FPSXffcc4eacf8e5d7348b79d256b5c8968b==>REDACTED_API_KEY" > replacements.txt

   # Run BFG
   bfg --replace-text replacements.txt

   # Force push (WARNING: coordinate with team first!)
   git reflog expire --expire=now --all && git gc --prune=now --aggressive
   git push --force
   ```

---

## Benefits of Secrets Gradle Plugin

✅ **Automatic BuildConfig generation** - No manual configuration needed
✅ **Type-safe access** - Compile-time constants
✅ **Multiple environments** - Can use `local.defaults.properties` for defaults
✅ **CI/CD ready** - Works seamlessly with GitHub Secrets
✅ **Obfuscation** - Integrates with ProGuard/R8 for release builds
✅ **Industry standard** - Used by Google Maps SDK and many Android apps
✅ **No runtime overhead** - Keys compiled into BuildConfig

---

## Testing the Fix

After implementation:

```bash
# 1. Verify local.properties is gitignored
git status  # should NOT show local.properties

# 2. Build should work
./gradlew clean assembleDebug

# 3. Verify BuildConfig was generated
cat app/build/generated/source/buildConfig/debug/com/rotdex/BuildConfig.kt | grep FREEPIK_API_KEY

# 4. Run tests
./gradlew test

# 5. Verify no hardcoded key in codebase
grep -r "FPSXffcc" app/src/  # should return nothing
```

---

## Timeline

**Priority**: CRITICAL - Must fix before production release
**Estimated Time**: 30-45 minutes
**Complexity**: Medium (requires API key rotation)

---

## Related Files

- `app/build.gradle.kts` - Add plugin
- `app/src/main/java/com/rotdex/di/NetworkModule.kt` - Update usage
- `.gitignore` - Ensure local.properties excluded
- `.github/workflows/android.yml` - Update CI/CD
- `README.md` - Add setup documentation
- `local.properties.template` - Create template

---

## References

- [Secrets Gradle Plugin Documentation](https://github.com/google/secrets-gradle-plugin)
- [Android Security Best Practices](https://developer.android.com/privacy-and-security/security-best-practices)
- [BFG Repo-Cleaner](https://rtyley.github.io/bfg-repo-cleaner/)
- [GitHub Encrypted Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

---

**Next Steps**: When ready to implement, follow the steps above in order. Test thoroughly before pushing to production.
