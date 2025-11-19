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

**Location:** `app/src/test/java/` (currently empty)

**Run all unit tests:**
```bash
./gradlew testDebugUnitTest
```

## Test Coverage

### Current Coverage

- **HomeScreenTest**: Verifies HomeScreen renders and navigation buttons work
- **DailyRewardsScreenTest**: Ensures DailyRewardsScreen renders without runtime crashes
- **FusionScreenTest**: Validates FusionScreen renders and back navigation works

### Why Instrumented Tests?

Instrumented tests catch runtime errors that unit tests can't detect, such as:
- **NoSuchMethodError** from library version mismatches
- **UI rendering crashes** from Compose components
- **Animation failures** from missing dependencies
- **Integration issues** between ViewModels and UI

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
