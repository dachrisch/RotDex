# Testing Guide

## Running Tests

### Instrumented UI Tests (AndroidTest)

Instrumented tests run on an Android device or emulator and verify that the UI renders correctly without runtime errors.

**Location:** `app/src/androidTest/java/com/rotdex/ui/screens/`

**Run all instrumented tests:**
```bash
./gradlew connectedDebugAndroidTest
```

**Run specific test class:**
```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rotdex.ui.screens.HomeScreenTest
```

**Run from Android Studio:**
1. Right-click on the test file or test method
2. Select "Run 'testName'"
3. Choose a connected device or emulator

### Unit Tests

**Location:** `app/src/test/java/`

**Run all unit tests:**
```bash
./gradlew testDebugUnitTest
```

**Current Unit Tests:**
- **FreepikApiModelsTest**: Verifies JSON serialization/deserialization of API models
- **CardRepositoryCompilationTest**: Catches missing imports and type resolution errors at compile time

## Test Coverage

### Current Coverage

#### Compilation Verification Tests (Unit Tests)
- **CardRepositoryCompilationTest**:
  - ✅ Verifies all Freepik API response types are accessible
  - ✅ Catches missing import statements (e.g., `ImageStatusResponse`)
  - ✅ Validates type references resolve correctly
  - ✅ Ensures API models have proper structure
  - **Catches errors like**: `Unresolved reference: ImageStatusResponse`

- **FreepikApiModelsTest**:
  - ✅ Tests JSON serialization of request models
  - ✅ Tests JSON deserialization of response models
  - ✅ Validates all API response states (processing, completed, failed)
  - ✅ Ensures Gson can handle all model structures

#### Integration Tests (Instrumented)
- **FreepikApiIntegrationTest**:
  - ✅ Verifies Hilt dependency injection works
  - ✅ Ensures `AiApiService` is properly created
  - ✅ Validates all API models can be instantiated
  - ✅ Tests null handling in optional fields

#### Screen-Specific Tests
- **HomeScreenTest**: Verifies HomeScreen renders and navigation buttons work
- **DailyRewardsScreenTest**: Ensures DailyRewardsScreen renders without runtime crashes, includes animation testing
- **FusionScreenTest**: Validates FusionScreen renders and back navigation works

#### Runtime Error Detection
- **ComposeRuntimeErrorTest**: Comprehensive test that catches library incompatibilities across all screens
  - ✅ Detects NoSuchMethodError from Compose BOM version mismatches
  - ✅ Catches animation API incompatibilities (e.g., CircularProgressIndicator keyframes)
  - ✅ Identifies missing dependencies that compile but fail at runtime
  - ✅ Validates all screens render with proper animation support

### Why Instrumented Tests?

Instrumented tests catch runtime errors that unit tests can't detect, such as:
- **NoSuchMethodError** from library version mismatches (e.g., Compose BOM 2024.01.00 animation incompatibility)
- **UI rendering crashes** from Compose components
- **Animation failures** from missing dependencies
- **Integration issues** between ViewModels and UI

### Real-World Examples

#### Example 1: Missing Import Error

**Problem**: Missing import for `ImageStatusResponse` in CardRepository
```
e: Unresolved reference: ImageStatusResponse
```
- Type defined in `AiApiService.kt` ✅
- Import statement missing in `CardRepository.kt` ❌
- Kotlin compiler cache didn't detect the issue ❌

**Solution**: `CardRepositoryCompilationTest`
- Test explicitly references `ImageStatusResponse` type
- **Fails to compile** if import is missing
- **Fails to compile** if type is not properly defined
- Runs on every build, catches issues immediately

**Prevention**:
```kotlin
@Test
fun `verify ImageStatusResponse type is accessible`() {
    val response: ImageStatusResponse? = null
    assertNull(response)
}
```
This test ensures the type is importable and accessible.

#### Example 2: Compose BOM Animation Incompatibility

**Problem**: Compose BOM 2024.01.00 provided incompatible animation library versions
- Compiled successfully ✅
- Linted successfully ✅
- **Crashed at runtime** ❌ with `NoSuchMethodError: KeyframesSpec$KeyframeEntity.at()`

**Solution**: `ComposeRuntimeErrorTest.dailyRewardsScreen_rendersWithoutRuntimeErrors()`
- Renders DailyRewardsScreen with CircularProgressIndicator
- Advances animation clock to trigger keyframes animations
- **Would have failed immediately** with the old BOM
- **Passes now** with Compose BOM 2024.06.00

#### Example 3: API Model Serialization

**Problem**: Freepik API expects specific JSON structure
- Code compiles ✅
- May fail at runtime with serialization errors ❌

**Solution**: `FreepikApiModelsTest`
- Tests actual JSON serialization/deserialization
- Validates field names match API expectations
- Catches missing or renamed fields before runtime
- Ensures Gson can handle the model structure

## CI/CD

### Compilation Check

The CI pipeline compiles both main and test sources to ensure:
- No compilation errors in production code
- No compilation errors in test code
- All dependencies are correctly configured

### Running Instrumented Tests in CI

Instrumented tests require an Android emulator, which is resource-intensive in CI. For now, they are:
- ✅ Compiled in CI to catch syntax errors
- ✅ Runnable locally before pushing
- ❌ Not executed in CI (future enhancement)

To run locally before pushing:
```bash
# Start an emulator or connect a device
./gradlew connectedDebugAndroidTest

# Verify all tests pass before pushing
```

## Adding New Tests

### For New Screens

1. Create a test file in `app/src/androidTest/java/com/rotdex/ui/screens/`
2. Name it `<ScreenName>Test.kt`
3. Add tests for:
   - Screen renders without crash
   - Main UI elements are present
   - User interactions work

### Example Test

```kotlin
@RunWith(AndroidJUnit4::class)
class MyNewScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun screenRendersWithoutCrash() {
        composeTestRule.setContent {
            RotDexTheme {
                MyNewScreen(onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithText("Screen Title").assertExists()
    }
}
```

## Troubleshooting

### Emulator Not Found

```bash
# List available emulators
emulator -list-avds

# Start an emulator
emulator -avd <avd_name>
```

### Test Timeout

Increase timeout in `app/build.gradle.kts`:
```kotlin
android {
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }
}
```

### Flaky Tests

- Ensure animations are disabled
- Use `composeTestRule.waitForIdle()` before assertions
- Use `waitUntil` for async operations
