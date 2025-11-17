# RotDex Architecture

## Overview

RotDex follows the **MVVM (Model-View-ViewModel)** architecture pattern with clean architecture principles, ensuring separation of concerns and maintainability.

## Architecture Layers

### 📱 Presentation Layer (UI)
- **Jetpack Compose** for declarative UI
- **ViewModels** for UI state management
- **Navigation** for screen transitions
- **Theme System** for consistent styling

**Location**: `app/src/main/java/com/rotdex/ui/`

### 💼 Domain Layer
- **Use Cases** for business logic
- **Domain Models** representing core entities
- **Repository Interfaces** for data abstraction

**Location**: `app/src/main/java/com/rotdex/domain/`

### 💾 Data Layer
- **Repository Implementations** for data operations
- **Room Database** for local card storage
- **Retrofit Services** for AI API calls
- **Data Models** (DTOs)

**Location**: `app/src/main/java/com/rotdex/data/`

## Core Components

### 1. Card Entity
Represents a brainrot card with:
- Unique ID
- AI-generated image URL
- User prompt
- Rarity level
- Creation timestamp
- Metadata (tags, favorites, etc.)

### 2. AI Service
Handles communication with AI generation APIs:
- Image generation requests
- Error handling and retry logic
- Response parsing

### 3. Collection Manager
Manages the user's card collection:
- CRUD operations
- Filtering and sorting
- Statistics calculation

### 4. Rarity System
Determines card rarity based on:
- Generation parameters
- Randomization algorithm
- User engagement metrics

## Data Flow

```
User Input → ViewModel → Use Case → Repository → Data Source
     ↓                                              ↓
   View ← StateFlow ← ViewModel ← Use Case ← Repository
```

1. **User Action**: User enters prompt and taps generate
2. **ViewModel**: Validates input, shows loading state
3. **Use Case**: Executes business logic
4. **Repository**: Fetches from API or database
5. **Data Source**: Returns data (AI-generated card or cached cards)
6. **Flow Back**: Data flows back through layers, updating UI

## State Management

- **UI State**: Using Compose State and StateFlow
- **Database**: Room for persistent storage
- **Network**: Retrofit for API calls
- **Images**: Coil for async image loading and caching

## Key Design Decisions

### Why Jetpack Compose?
- Modern, declarative UI
- Less boilerplate
- Better performance
- Easier testing

### Why Room Database?
- Type-safe queries
- Compile-time verification
- Migration support
- SQLite wrapper with less boilerplate

### Why Kotlin Coroutines?
- Structured concurrency
- Better async/await syntax
- Cancellation support
- Integration with Jetpack libraries

## Module Structure (Future)

For scalability, the app can be modularized:

```
:app (Application module)
:feature:generator (Card generation feature)
:feature:collection (Collection viewing feature)
:core:ui (Shared UI components)
:core:data (Data layer)
:core:domain (Domain models and interfaces)
:core:network (Network utilities)
:core:database (Room database)
```

## Testing Strategy

### Unit Tests
- ViewModels
- Use Cases
- Repository logic
- Utility functions

### Integration Tests
- Database operations
- API service calls
- Navigation flows

### UI Tests
- User interactions
- Screen rendering
- End-to-end flows

## Dependencies Management

All dependencies are centralized in `app/build.gradle.kts` with version catalogs for:
- AndroidX libraries
- Compose
- Networking
- Database
- Testing

## Performance Considerations

1. **Image Caching**: Coil handles image caching automatically
2. **Database Queries**: Room with Flow for reactive updates
3. **Lazy Loading**: LazyColumn for efficient list rendering
4. **Background Work**: Coroutines for off-main-thread operations
5. **Memory**: Proper lifecycle awareness and scope management

## Security

- API keys stored in local.properties (not in version control)
- ProGuard rules for release builds
- Input validation for user prompts
- Secure network communication (HTTPS)

## Future Enhancements

- Offline-first architecture
- Multi-module structure
- Dependency injection with Hilt
- CI/CD pipeline
- Analytics integration
- Crash reporting

---

**Last Updated**: 2025-11-17
