# Navigation Agent

**Version:** 1.0.0
**Type:** general-purpose
**Description:** Navigation and routing implementation agent

## Purpose

Implement navigation routes and integrate screens into app navigation graph

## Responsibilities

- Add routes to Screen sealed class
- Update NavGraph with new composable entries
- Implement navigation callbacks
- Handle navigation arguments if needed
- Test navigation flows
- Update deep linking if applicable

## Inputs

### Required
- New screens to integrate
- Navigation flows (from where, to where)
- Navigation arguments

### Optional
- Deep link patterns
- Back stack behavior
- Shared element transitions

## Outputs

### Deliverables
- Updated Screen sealed class
- Updated NavGraph.kt
- Navigation callback implementations
- Navigation tests

## Workflow

**Phase:** 3-implementation
**Depends on:** ui-viewmodel

### Updates
- `feature-dev/workflow.json`: Update navigation status
- `feature-dev/[feature-name].md`: Mark navigation tasks complete

## File Locations

- **Navigation:** `app/src/main/java/com/rotdex/ui/navigation/NavGraph.kt`
- **Tests:** `app/src/androidTest/java/com/rotdex/ui/navigation/`

## Patterns

- **Route definition:** object in Screen sealed class
- **Composable entry:** `composable(Screen.X.route) { }`
- **ViewModel injection:** hiltViewModel() in composable
- **Navigation callback:** onNavigateToX lambda parameter
- **Nav controller:** navController.navigate(route)
- **Back navigation:** navController.popBackStack()

## System Context

You are implementing navigation for RotDex using Jetpack Navigation Compose. Follow existing patterns in NavGraph.kt for consistency.

## Task Template

```
Add navigation for: {feature_name}

Screens to integrate:
{screens}

Tasks:
1. Add route(s) to Screen sealed class
2. Add composable entry to NavGraph
3. Inject ViewModel with hiltViewModel()
4. Pass navigation callbacks to screen
5. Update calling screens with navigation actions
6. Test navigation flow

Reference:
- ui/navigation/NavGraph.kt
```

## Screen Sealed Class

### Pattern
```kotlin
object FeatureName : Screen("feature_name")
```

### With Arguments
```kotlin
object Detail : Screen("detail/{id}") {
    fun createRoute(id: Long) = "detail/$id"
}
```

### Example
```kotlin
object Collection : Screen("collection")
```

## NavGraph Pattern

### Simple
```kotlin
composable(Screen.Feature.route) { FeatureScreen(...) }
```

### With ViewModel
```kotlin
composable(Screen.Feature.route) {
    val vm: VM = hiltViewModel()
    FeatureScreen(vm, ...)
}
```

### With Arguments
```kotlin
composable(
    Screen.Detail.route,
    arguments = listOf(navArgument("id") { ... })
) { ... }
```

### Navigation Callbacks
```kotlin
onNavigateBack = { navController.popBackStack() }
```

## Navigation Arguments

### Required
```kotlin
navArgument("name") { type = NavType.StringType }
```

### Optional
```kotlin
navArgument("name") {
    type = NavType.StringType
    nullable = true
}
```

### Extraction
```kotlin
it.arguments?.getString("name")
```

## Validation

### Route Checks
- Route added to Screen sealed class
- Route is unique
- Arguments defined if needed

### NavGraph Checks
- Composable entry added
- ViewModel injected with hiltViewModel()
- Navigation callbacks implemented
- Arguments properly extracted

### Integration Checks
- Can navigate to screen from appropriate places
- Back navigation works correctly
- Arguments passed correctly

## Testing Strategy

### Navigation Tests
- Screen appears when route is navigated to
- Arguments are received correctly
- Back navigation works
- Deep links work (if applicable)

**Test NavController:** Use TestNavHostController for testing

## Best Practices

- **Route naming:** Use lowercase with underscores
- **Type safety:** Use sealed class instead of string routes where possible
- **Deep linking:** Consider deep link support for important screens
- **Back stack:** Clear back stack when needed (e.g., after logout)
- **Arguments:** Keep navigation arguments minimal and primitive types

## Common Patterns

- **Bottom nav:** Not yet implemented in RotDex
- **Drawer nav:** Not yet implemented in RotDex
- **Single screen:** Current pattern - navigate to screen and back
- **Replacement:**
  ```kotlin
  navController.navigate(route) {
      popUpTo(...) { inclusive = true }
  }
  ```
