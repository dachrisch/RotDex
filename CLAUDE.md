# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RotDex is an Android app that transforms user prompts into collectible AI-generated cards. The app features a gacha-style collection system with card fusion mechanics, daily rewards, and an energy-based progression system.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run static analysis (Detekt)
./gradlew detekt

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.rotdex.data.api.FreepikApiModelsTest"

# Run specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rotdex.ui.screens.HomeScreenTest
```

## Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Database**: Room (local storage for cards and user data)
- **Networking**: Retrofit + OkHttp for Freepik Mystic API integration
- **Dependency Injection**: Hilt
- **Background Tasks**: WorkManager (energy regeneration)
- **Image Loading**: Coil
- **Async**: Kotlin Coroutines + Flow

### Project Structure

```
app/src/main/java/com/rotdex/
├── data/
│   ├── api/           # API service interfaces and models (Freepik Mystic API)
│   ├── database/      # Room database, DAOs, and converters
│   ├── manager/       # Business logic managers (Fusion, SpinWheel, Streak)
│   ├── models/        # Data models and game configuration
│   └── repository/    # Repository pattern implementations
├── di/                # Hilt dependency injection modules
├── ui/
│   ├── navigation/    # Jetpack Navigation setup
│   ├── screens/       # Composable screens
│   ├── theme/         # Material Design theme configuration
│   └── viewmodel/     # ViewModels for screens
├── workers/           # WorkManager background workers
├── MainActivity.kt
└── RotDexApplication.kt
```

### Key Architectural Patterns

#### Dependency Injection with Hilt
- All modules are in `di/` package: `DatabaseModule`, `NetworkModule`, `RepositoryModule`
- ViewModels are injected via `@HiltViewModel` annotation
- Use `@AndroidEntryPoint` for Activities and Composables
- Application class is annotated with `@HiltAndroidApp`

#### Repository Pattern
- `CardRepository`: Manages card CRUD operations, AI generation, and fusion logic
- `UserRepository`: Manages user profile, energy, coins, gems, and streaks
- Repositories coordinate between local database (Room) and remote APIs (Retrofit)

#### Database Schema (Room)
- **Entities**: `Card`, `UserProfile`, `SpinHistory`, `FusionHistory`
- **Database Version**: 3 (uses `fallbackToDestructiveMigration`)
- **DAOs**: `CardDao`, `UserProfileDao`, `SpinHistoryDao`, `FusionHistoryDao`
- **Type Converters**: Located in `Converters.kt` for complex types (CardRarity, lists)

#### ViewModels
- ViewModels use `viewModelScope` for coroutine launching
- Expose UI state via `StateFlow` and `Flow`
- Business logic is delegated to repositories and managers
- Error handling returns `Result<T>` types from repository operations

### Important Implementation Details

#### Card Generation Flow
1. User enters prompt in `CardCreateScreen`
2. `CardCreateViewModel` calls `cardRepository.generateCard()`
3. Repository checks and spends energy via `userRepository.spendEnergy()`
4. API call to Freepik Mystic API to start image generation (async job)
5. Poll job status every 2 seconds (max 15 attempts = 30 seconds)
6. Download image from URL using `Dispatchers.IO` to avoid `NetworkOnMainThreadException`
7. Save image to local storage in `filesDir/card_images/`
8. Assign random rarity based on drop rates
9. Save card to Room database with local file path as `imageUrl`

#### Energy System
- Max energy: 10 (configured in `GameConfig.MAX_ENERGY`)
- Card generation costs 1 energy (`GameConfig.CARD_GENERATION_ENERGY_COST`)
- Regenerates via `EnergyRegenerationWorker` (periodic WorkManager task every 15 minutes)
- Energy regeneration interval: 30 minutes per point (`GameConfig.ENERGY_REGEN_INTERVAL_MINUTES`)

#### Card Fusion System
- Managed by `FusionManager` class
- Validation rules in `FusionRules` object
- Recipes defined in `FusionRecipes` object (both public and secret recipes)
- Fusion cost: 50 coins (`GameConfig.FUSION_COIN_COST`)
- Success rate depends on card count and rarity tier
- Matching recipe adds +20% success rate bonus
- Input cards are deleted, result card is created
- All fusions recorded in `FusionHistory` for tracking and recipe discovery

#### Navigation
- Uses Jetpack Navigation Compose
- Routes defined in `Screen` sealed class in `NavGraph.kt`
- Screens: Home, DailyRewards, Collection (placeholder), CardCreate, Fusion
- ViewModels are scoped to navigation entries via `hiltViewModel()`

#### API Integration (Freepik Mystic)
- Base URL configured in `NetworkModule`
- Requires API key in Authorization header (Bearer token)
- Image generation is asynchronous (poll-based)
- Models: `ImageGenerationRequest`, `ImageGenerationResponse`, `ImageStatusResponse`
- Image status enum: `IN_PROGRESS`, `COMPLETED`, `FAILED`

#### Game Economy Configuration
All costs and limits centralized in `GameConfig.kt`:
- Card generation: 1 energy
- Fusion: 50 coins
- Max energy: 10
- Energy regen: every 30 minutes
- Daily rewards: 50 coins + 3 energy

## Development Guidelines

### API Key Setup
The Freepik API key must be configured in `NetworkModule.kt`. Look for the API key injection or configuration in the Hilt module.

### Adding New Screens
1. Create screen Composable in `ui/screens/`
2. Create ViewModel in `ui/viewmodel/` with `@HiltViewModel`
3. Add route to `Screen` sealed class in `NavGraph.kt`
4. Add `composable()` entry in `NavGraph()`
5. Update navigation calls from other screens

### Working with Card Images
- Images are stored locally in `context.filesDir/card_images/`
- The `imageUrl` field in `Card` entity stores the absolute file path
- Use `Dispatchers.IO` for all network and file I/O operations
- Coil is configured to load images from file paths

### Background Tasks
- Energy regeneration runs every 15 minutes via WorkManager
- Worker is scheduled in `MainActivity.onCreate()`
- WorkManager configuration uses `HiltWorkerFactory` for DI

### Testing
- Unit tests in `app/src/test/`
- Instrumented tests (UI tests) in `app/src/androidTest/`
- Compose UI tests use `ui-test-junit4` dependency
- API integration tests in `FreepikApiIntegrationTest.kt`

### Code Quality
- Detekt is configured for static analysis
- Configuration: `config/detekt/detekt.yml`
- Baseline: `config/detekt/baseline.xml`
- Run `./gradlew detekt` before committing
