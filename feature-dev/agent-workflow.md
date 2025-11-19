# Agent-Driven Feature Development Workflow

## Overview

This document describes the multi-agent workflow for feature development in RotDex. Each agent is specialized for a specific phase or layer of development, enabling parallel execution and efficient implementation.

## Agent Definitions

All agents are defined in `.claude/agents/` directory as JSON configuration files. Each agent has:
- **Purpose**: What the agent does
- **Responsibilities**: Specific tasks it handles
- **Inputs/Outputs**: What it needs and produces
- **Workflow**: When it runs and what it depends on
- **Patterns**: Code patterns and best practices to follow

## Development Phases

### Phase 1: Planning
**Agent**: `planning.json`
**Type**: Plan agent
**Trigger**: New feature request

**Tasks**:
- Analyze requirements and user stories
- Break down into actionable tasks
- Identify affected components (Database, Repository, ViewModel, UI)
- Create dependency graph
- Assess risks and complexity
- Generate implementation checklist

**Output**: `feature-dev/[feature-name]-plan.md`

**Updates**:
- Add entry to `workflow.json`
- Create tracking entry in `features/STATUS.md`

---

### Phase 2: Exploration
**Agent**: `exploration.json`
**Type**: Explore agent (medium thoroughness)
**Trigger**: After planning completes

**Tasks**:
- Find similar existing features
- Identify reusable components and patterns
- Locate relevant ViewModels, Repositories, DAOs
- Review existing navigation and UI patterns
- Document architectural patterns to follow

**Output**: `feature-dev/[feature-name]-exploration.md`

**Benefits**:
- Ensures consistency with existing code
- Identifies opportunities for code reuse
- Provides concrete examples to follow

---

### Phase 3: Parallel Implementation
**Run these agents in parallel for maximum efficiency**

#### 3a. Data Layer Agent
**Agent**: `data-layer.json`
**Type**: general-purpose

**Tasks**:
- Create Room entity classes
- Define DAO interfaces with queries
- Add database migrations
- Create data models and DTOs
- Update DatabaseModule for Hilt
- Write unit tests for database operations

**Outputs**:
- `data/models/[Entity].kt`
- `data/database/[Entity]Dao.kt`
- Tests in `test/java/com/rotdex/data/database/`

#### 3b. API Integration Agent (if needed)
**Agent**: `api-integration.json`
**Type**: general-purpose

**Tasks**:
- Define API service interface
- Create request/response models
- Add JSON serialization
- Update NetworkModule
- Add logging interceptors
- Write integration tests

**Outputs**:
- `data/api/[Service]ApiService.kt`
- Tests in `androidTest/java/com/rotdex/data/api/`

#### 3c. Business Logic Agent
**Agent**: `business-logic.json`
**Type**: general-purpose

**Tasks**:
- Create repository classes
- Implement manager classes for complex logic
- Add validation rules
- Implement error handling
- Update RepositoryModule
- Write unit tests

**Outputs**:
- `data/repository/[Feature]Repository.kt`
- `data/manager/[Feature]Manager.kt`
- Tests in `test/java/com/rotdex/data/repository/`

#### 3d. UI & ViewModel Agent
**Agent**: `ui-viewmodel.json`
**Type**: general-purpose

**Tasks**:
- Define UI state data classes
- Create ViewModel with @HiltViewModel
- Implement state management with StateFlow
- Create screen composables
- Handle user interactions
- Write UI tests

**Outputs**:
- `ui/viewmodel/[Feature]ViewModel.kt`
- `ui/screens/[Feature]Screen.kt`
- Tests in `androidTest/java/com/rotdex/ui/screens/`

#### 3e. Navigation Agent
**Agent**: `navigation.json`
**Type**: general-purpose

**Tasks**:
- Add routes to Screen sealed class
- Update NavGraph with composable entries
- Implement navigation callbacks
- Test navigation flows

**Outputs**:
- Updated `ui/navigation/NavGraph.kt`

---

### Phase 4: Testing
**Agent**: `testing.json`
**Type**: general-purpose
**Trigger**: After all implementation agents complete

**Tasks**:
- Write unit tests for repositories and business logic
- Write instrumented tests for UI
- Write integration tests for API and database
- Verify error handling paths
- Test edge cases

**Coverage Goals**:
- Repositories: > 80%
- ViewModels: > 70%
- Managers: > 80%
- UI: Critical paths tested

**Commands**:
```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew detekt
```

---

### Phase 5: Review
**Agent**: `review.json`
**Type**: general-purpose
**Trigger**: After testing completes

**Review Areas**:
1. **Security**: No vulnerabilities (SQL injection, XSS, hardcoded secrets)
2. **Error Handling**: Proper try-catch, user-friendly messages
3. **Resource Management**: No leaks, proper cleanup
4. **Performance**: No main thread blocks, efficient queries
5. **Architecture**: MVVM adherence, separation of concerns
6. **Code Quality**: Clean code, no duplication, idiomatic Kotlin
7. **Testing**: Adequate coverage, meaningful tests
8. **Documentation**: Complex logic documented

**Output**: `feature-dev/[feature-name]-review.md`

---

## Workflow Execution Examples

### Example 1: Collection Screen Feature

**Step 1: Planning**
```
Use planning agent with:
Feature: "Card Collection Screen with grid layout and filtering"
Requirements: [list of requirements]
```

**Step 2: Exploration**
```
Use exploration agent with:
Search for: Similar screens (HomeScreen, CardCreateScreen)
Patterns to find: Grid layouts, filtering, navigation
```

**Step 3: Parallel Implementation**
Launch all these agents **in one message** for parallel execution:

```
Agent 1 (data-layer):
"No new database entities needed, but add filtering methods to CardDao"

Agent 2 (business-logic):
"Add filtering and sorting methods to CardRepository"

Agent 3 (ui-viewmodel):
"Create CollectionViewModel with state for cards, filters, and CollectionScreen composable"

Agent 4 (navigation):
"Add Collection route and integrate into NavGraph"
```

**Step 4: Testing**
```
Use testing agent:
"Write tests for Collection feature - ViewModel tests for filtering logic,
UI tests for grid display and interactions"
```

**Step 5: Review**
```
Use review agent:
"Review Collection feature implementation for quality and security"
```

### Example 2: Card Trading Feature (More Complex)

**Phase 1: Planning Agent**
- Analyzes trading requirements
- Identifies needed components:
  - Database: TradeOffer and TradeHistory entities
  - API: Trade negotiation endpoints (if multiplayer)
  - Repository: TradeRepository
  - Manager: TradeManager for business rules
  - ViewModel: TradeViewModel
  - UI: TradeScreen

**Phase 2: Exploration Agent**
- Finds fusion system as similar feature (multi-card selection)
- Identifies card selection UI patterns
- Reviews existing manager patterns (FusionManager)

**Phase 3: Parallel Implementation (5 agents)**
1. **Data Layer**: Create TradeOffer and TradeHistory entities + DAOs
2. **API Integration**: Create TradeApiService (if multiplayer)
3. **Business Logic**: Create TradeRepository and TradeManager
4. **UI/ViewModel**: Create TradeViewModel and TradeScreen
5. **Navigation**: Add trade routes to NavGraph

**Phase 4: Testing**
- Unit tests for TradeManager business rules
- Repository tests with mocked DAO
- ViewModel tests for state management
- UI tests for trade flow

**Phase 5: Review**
- Security: Validate trade authorization
- Performance: Check database query efficiency
- UX: Verify error messages are clear

---

## Agent Coordination via workflow.json

The `workflow.json` file serves as the coordination mechanism between agents:

```json
{
  "features": {
    "collection_screen": {
      "status": "in_progress",
      "current_phase": "implementation",
      "agents": {
        "planning": "completed",
        "exploration": "completed",
        "data_layer": "completed",
        "business_logic": "in_progress",
        "ui_viewmodel": "pending",
        "navigation": "pending",
        "testing": "pending",
        "review": "pending"
      }
    }
  }
}
```

---

## Progress Tracking

### workflow.json Updates
Each agent updates `workflow.json` with:
- Agent completion status
- Files created/modified
- Tests added
- Blockers encountered

### Feature Documentation
Maintain `feature-dev/[feature-name].md` with:
- Implementation progress checklist
- Key decisions made
- Issues encountered and resolved
- References to commits

### STATUS.md
Update `docs/features/STATUS.md` with:
- Overall feature status
- Completion percentage
- Current blockers
- Next steps

---

## Running Agents

### Single Agent
```
Use Task tool with:
- subagent_type: [agent name from .claude/agents/]
- prompt: Detailed task description
- description: Short task name
```

### Parallel Agents
**Launch in ONE message with multiple Task tool calls**:
```
Message with 5 Task tool calls:
1. Data layer agent task
2. API integration agent task
3. Business logic agent task
4. UI/ViewModel agent task
5. Navigation agent task
```

---

## Best Practices

### Before Starting
1. Read existing `workflow.json` to understand current state
2. Check `STATUS.md` for feature priorities
3. Review CLAUDE.md for architecture patterns

### During Development
1. Launch implementation agents in parallel when possible
2. Update `workflow.json` after each phase
3. Document decisions in feature docs
4. Track blockers immediately

### After Completion
1. Run full test suite
2. Run detekt static analysis
3. Complete code review
4. Update all documentation
5. Mark feature as complete in STATUS.md

---

## Agent Communication

Agents communicate through:
1. **workflow.json**: Structured state and coordination
2. **Feature docs**: Detailed implementation notes
3. **Code comments**: Technical decisions in code
4. **Git commits**: Atomic changes with descriptive messages

---

## Quality Gates

Before marking a feature complete:
- [ ] All unit tests passing
- [ ] All instrumented tests passing
- [ ] Detekt checks passing (`./gradlew detekt`)
- [ ] Code review completed
- [ ] Documentation updated
- [ ] No critical or important review findings unresolved
- [ ] Feature works end-to-end
- [ ] workflow.json updated
- [ ] STATUS.md updated

---

## Troubleshooting

### Agent Stuck or Blocked
1. Check `workflow.json` for dependencies
2. Review agent outputs for error messages
3. Verify all prerequisite agents completed
4. Check if code compiles

### Parallel Agents Conflict
1. Ensure agents work on different files/layers
2. If same file needed, run sequentially
3. Data layer should complete before business logic

### Tests Failing
1. Run testing agent to add missing tests
2. Check for dependency injection issues
3. Verify mocks are properly configured
4. Review error messages in test output

---

## Templates

### Feature Branch Creation
```bash
git checkout -b feature/[feature-name]
```

### Feature Doc Creation
```bash
cp feature-dev/TEMPLATE.md feature-dev/[feature-name].md
```

### Initial workflow.json Entry
```json
{
  "feature_name": {
    "status": "planning",
    "priority": "medium",
    "description": "...",
    "agents": {
      "planning": "pending"
    }
  }
}
```

---

## References

- **Agent Definitions**: `.claude/agents/*.json`
- **Architecture Guide**: `CLAUDE.md`
- **Feature Status**: `docs/features/STATUS.md`
- **Workflow State**: `feature-dev/workflow.json`
- **Existing Implementation**: `feature-dev/card-generation-implementation.md`
