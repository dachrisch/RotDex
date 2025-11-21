# UI & ViewModel Agent

**Version:** 1.0.0
**Type:** general-purpose
**Description:** UI screens and ViewModel implementation agent

## Purpose

Implement Jetpack Compose screens and ViewModels with state management

## Responsibilities

- Create composable screens with Material Design 3
- Implement ViewModels with @HiltViewModel
- Define UI state data classes
- Implement state management with StateFlow
- Handle user interactions and navigation
- Implement loading, error, and success states
- Write UI tests for screens

## Inputs

### Required
- Screen requirements and mockups
- User interactions to handle
- Data to display

### Optional
- Animation requirements
- Accessibility requirements
- Screen size variations

## Outputs

### Deliverables
- Screen composables in `ui/screens/`
- ViewModels in `ui/viewmodel/`
- UI state classes
- Instrumented UI tests

## Workflow

**Phase:** 3-implementation
**Depends on:** business-logic, data-layer
**Parallel with:** navigation

### Updates
- `feature-dev/workflow.json`: Update ui_implementation status
- `feature-dev/[feature-name].md`: Mark UI tasks complete

## File Locations

- **Screens:** `app/src/main/java/com/rotdex/ui/screens/`
- **ViewModels:** `app/src/main/java/com/rotdex/ui/viewmodel/`
- **Theme:** `app/src/main/java/com/rotdex/ui/theme/`
- **UI Tests:** `app/src/androidTest/java/com/rotdex/ui/screens/`

## Patterns

- **ViewModel:** @HiltViewModel with @Inject constructor
- **State:** Sealed class or data class for UI state
- **StateFlow:** StateFlow for observable state
- **Events:** Separate state from one-time events
- **Composable:** Stateless composable accepting state and callbacks
- **Preview:** @Preview annotations for development

## System Context

You are implementing UI for RotDex using Jetpack Compose and Material Design 3. Follow MVVM pattern with state hoisting. Use existing theme and components.

## Task Template

```
Implement UI for: {feature_name}

Requirements:
{requirements}

Tasks:
1. Define UI state data class
2. Create ViewModel with @HiltViewModel
3. Implement state management with StateFlow
4. Create screen composable
5. Implement user interactions
6. Handle loading, error, success states
7. Add @Preview composables
8. Write UI tests

Reference:
- ui/screens/CardCreateScreen.kt
- ui/viewmodel/CardCreateViewModel.kt
- ui/theme/ for styling
```

## ViewModel Structure

- **Annotation:** @HiltViewModel
- **Injection:** @Inject constructor with repository
- **State:** Private MutableStateFlow, public StateFlow
- **Scope:** Use viewModelScope for coroutines
- **Error handling:** Catch exceptions and update state

## UI State Patterns

- **Loading state:** Loading, Success, Error sealed class
- **Data state:** Data classes with nullable fields
- **Event state:** Separate events from persistent state
- **Validation state:** Track form validation errors

## Compose Patterns

- **State hoisting:** State managed in ViewModel, passed to composable
- **Callbacks:** Lambda parameters for user actions
- **Remember:** Use remember for local UI state
- **Side effects:** LaunchedEffect for one-time operations
- **Scaffold:** Use Scaffold for app bar and layout

## Material Design

- **Theme:** Use MaterialTheme.colorScheme
- **Components:** Material3 components (Button, Card, TextField, etc.)
- **Typography:** MaterialTheme.typography
- **Spacing:** Consistent padding and spacing
- **Accessibility:** Content descriptions and semantic properties

## Validation

### ViewModel Checks
- @HiltViewModel annotation present
- Repository injected via constructor
- StateFlow for state exposure
- viewModelScope for coroutines
- Proper error handling

### Screen Checks
- Stateless composable design
- State and callbacks as parameters
- @Preview annotations
- Accessibility support
- Loading and error states handled

## Testing Strategy

### UI Tests
- Screen displays correctly
- User interactions work
- Loading state shown appropriately
- Error messages displayed
- Navigation callbacks invoked

**Compose Testing:** Use ComposeTestRule and semantics

## Best Practices

- **Separation of concerns:** ViewModel handles logic, composable handles UI
- **Reusability:** Extract common components
- **Performance:** Use remember and derivedStateOf appropriately
- **Preview:** Add previews for different states
- **Dark theme:** Support both light and dark themes
